package com.example.travelcents.data.trip

import android.util.Log
import com.example.travelcents.data.sync.TripSyncRemoteDataSource
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_ACTIVITIES
import com.example.travelcents.data.trip.model.YELP_POOL_TYPE_RESTAURANTS
import com.example.travelcents.data.trip.model.resolveTripName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreTripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TripRepository {
    private val tripSyncRemoteDataSource = TripSyncRemoteDataSource(db)

    override suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? {
        findLatestOwnedActiveTripKey(viewerUid)?.let { return it }

        return getSharedTripSummariesSafe(viewerUid)
            .filterNot { trip -> trip.status.equals("archived", ignoreCase = true) }
            .maxByOrNull { trip -> trip.createdAt }
            ?.let { trip ->
                TripKey(ownerUid = trip.ownerUid, tripId = trip.itineraryId)
            }
    }

    override suspend fun getTripSummaries(viewerUid: String): List<Itinerary> = coroutineScope {
        val ownedTrips = async { getOwnedTripSummaries(viewerUid) }
        val sharedTrips = async {
            runCatching {
                getSharedTripSummariesSafe(viewerUid)
            }.getOrElse { error ->
                Log.w(
                    TAG,
                    "Shared trip discovery failed for viewer $viewerUid. Returning owned trips only.",
                    error
                )
                emptyList()
            }
        }

        (ownedTrips.await() + sharedTrips.await())
            .distinctBy { trip -> "${trip.ownerUid}:${trip.itineraryId}" }
            .sortedWith(
                compareByDescending<Itinerary> { trip -> trip.createdAt }
                    .thenByDescending { trip -> trip.dateFrom }
            )
    }

    override suspend fun getTripSummary(key: TripKey): Itinerary? {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.getTripSummary",
            detail = "ownerUid=${key.ownerUid} tripId=${key.tripId}"
        )
        val document = tripDocument(key).get().await()
        if (!document.exists()) return null
        return mapTripSummary(document, key.ownerUid)
    }

    override fun observeTripSummary(key: TripKey): Flow<Itinerary?> = callbackFlow {
        TripPerformanceLogger.recordListenerAttached(
            source = "FirestoreTripRepository.observeTripSummary",
            tripId = key.tripId
        )
        val registration = tripDocument(key).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            TripPerformanceLogger.recordTripQuery(
                source = "FirestoreTripRepository.observeTripSummary",
                detail = "ownerUid=${key.ownerUid} tripId=${key.tripId}"
            )
            val trip = snapshot?.takeIf { it.exists() }?.let { mapTripSummary(it, key.ownerUid) }
            trySend(trip).isSuccess
        }

        awaitClose {
            TripPerformanceLogger.recordListenerDetached(
                source = "FirestoreTripRepository.observeTripSummary",
                tripId = key.tripId
            )
            registration.remove()
        }
    }

    override fun observeTripEvents(key: TripKey): Flow<List<TravelEvent>> = callbackFlow {
        TripPerformanceLogger.recordListenerAttached(
            source = "FirestoreTripRepository.observeTripEvents",
            tripId = key.tripId
        )
        val registration = tripDocument(key)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                TripPerformanceLogger.recordEventQuery(
                    source = "FirestoreTripRepository.observeTripEvents",
                    detail = "ownerUid=${key.ownerUid} tripId=${key.tripId}"
                )
                trySend(snapshot.toTripEvents(key.tripId)).isSuccess
            }

        awaitClose {
            TripPerformanceLogger.recordListenerDetached(
                source = "FirestoreTripRepository.observeTripEvents",
                tripId = key.tripId
            )
            registration.remove()
        }
    }

    override suspend fun getTripMembers(key: TripKey): List<String> {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.getTripMembers",
            detail = "ownerUid=${key.ownerUid} tripId=${key.tripId}"
        )

        val tripSnapshot = tripDocument(key).get().await()
        return normalizeTripMembers(
            ownerUid = key.ownerUid,
            memberUids = tripSnapshot.memberUids()
        )
    }

    override suspend fun getEventOptions(
        key: TripKey,
        eventIds: List<String>
    ): Map<String, List<EventOption>> = coroutineScope {
        if (eventIds.isEmpty()) return@coroutineScope emptyMap()

        eventIds.map { eventId ->
            async {
                TripPerformanceLogger.recordOptionQuery(
                    source = "FirestoreTripRepository.getEventOptions",
                    detail = "ownerUid=${key.ownerUid} tripId=${key.tripId} eventId=$eventId"
                )
                val snapshot = tripDocument(key)
                    .collection("events")
                    .document(eventId)
                    .collection("options")
                    .get()
                    .await()

                backfillMissingOptionScope(key, eventId, snapshot.documents)

                val options = snapshot.documents
                    .map { document ->
                        val raw = document.data ?: emptyMap()
                        EventOption.fromMap(
                            raw + mapOf(
                                "optionId" to (raw["optionId"]?.toString() ?: document.id),
                                "eventId" to (raw["eventId"]?.toString().orEmpty().ifBlank { eventId }),
                                "tripId" to (raw["tripId"]?.toString().orEmpty().ifBlank { key.tripId }),
                                "ownerUid" to (raw["ownerUid"]?.toString().orEmpty().ifBlank { key.ownerUid })
                            )
                        )
                    }
                    .sortedByDescending { it.selected }

                eventId to options
            }
        }.awaitAll().toMap()
    }

    override suspend fun ensureTripAccess(
        key: TripKey,
        memberUids: List<String>,
        defaultRole: TripAccessRole
    ) {
        val existingSnapshot = tripDocument(key).get().await()
        val previousMemberUids = existingSnapshot.memberUids().toSet()
        val version = System.currentTimeMillis()
        val tripRef = tripDocument(key)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Trip ${key.tripId} is no longer available.")
            }

            val access = mergeTripAccessMetadata(
                ownerUid = key.ownerUid,
                existingMemberUids = snapshot.memberUids(),
                existingRoleByUid = snapshot.roleByUid(),
                additionalMemberUids = memberUids,
                defaultRole = defaultRole
            )

            transaction.set(
                tripRef,
                mapOf(
                    "ownerUid" to key.ownerUid,
                    "memberUids" to access.memberUids,
                    "roleByUid" to access.roleByUid,
                    "accessSchemaVersion" to Itinerary.ACCESS_SCHEMA_VERSION,
                    "membersVersion" to version,
                    "updatedAtEpochMs" to version
                ),
                SetOptions.merge()
            )
        }.await()
        val updatedSnapshot = tripDocument(key).get().await()
        val currentMemberUids = updatedSnapshot.memberUids().toSet().ifEmpty { setOf(key.ownerUid) }
        tripSyncRemoteDataSource.refreshTripIndexesForTrip(
            tripKey = key,
            removedViewerUids = previousMemberUids - currentMemberUids
        )
    }

    override suspend fun backfillOwnedTripAccess(ownerUid: String) = coroutineScope {
        val tripDocuments = tripsCollection(ownerUid).get().await().documents
        tripDocuments.forEach { tripDocument ->
            val tripKey = TripKey(ownerUid = ownerUid, tripId = tripDocument.id)
            val existingMembers = tripDocument.memberUids()
            val existingRoles = tripDocument.roleByUid()
            val accessVersion = tripDocument.accessSchemaVersion()

            val linkedGroupEditors = linkedGroupMembers(tripKey)
            val legacySharedViewers = legacySharedTripMembers(tripKey)

            val shouldBackfill = accessVersion < Itinerary.ACCESS_SCHEMA_VERSION ||
                existingMembers.isEmpty() ||
                existingRoles.isEmpty() ||
                linkedGroupEditors.any { uid -> existingRoles[uid] != TripAccessRole.EDITOR.wireValue } ||
                legacySharedViewers.any { uid -> uid !in existingMembers }

            if (!shouldBackfill) return@forEach

            val viewerAccess = mergeTripAccessMetadata(
                ownerUid = ownerUid,
                existingMemberUids = existingMembers,
                existingRoleByUid = existingRoles,
                additionalMemberUids = legacySharedViewers.toList(),
                defaultRole = TripAccessRole.VIEWER
            )
            val finalAccess = mergeTripAccessMetadata(
                ownerUid = ownerUid,
                existingMemberUids = viewerAccess.memberUids,
                existingRoleByUid = viewerAccess.roleByUid,
                additionalMemberUids = linkedGroupEditors.toList(),
                defaultRole = TripAccessRole.EDITOR
            )

            db.runBatch { batch ->
                batch.set(
                    tripDocument.reference,
                    mapOf(
                        "ownerUid" to ownerUid,
                        "memberUids" to finalAccess.memberUids,
                        "roleByUid" to finalAccess.roleByUid,
                        "accessSchemaVersion" to Itinerary.ACCESS_SCHEMA_VERSION
                    ),
                    SetOptions.merge()
                )
            }.await()
            tripSyncRemoteDataSource.refreshTripIndexesForTrip(tripKey)
        }
    }

    override suspend fun deleteTrip(key: TripKey) = coroutineScope {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.deleteTrip",
            detail = "ownerUid=${key.ownerUid} tripId=${key.tripId}"
        )

        val tripRef = tripDocument(key)
        val eventDocuments = tripRef.collection("events").get().await().documents
        val optionDocuments = loadTripOptionDocumentsForDelete(key, eventDocuments)
        val yelpPoolDocuments = loadTripYelpPoolDocumentsForDelete(key)

        val tripSnapshot = tripRef.get().await()
        val affectedViewerUids = tripSnapshot.memberUids().toSet().ifEmpty { setOf(key.ownerUid) }
        val deleteRefs = buildList {
            addAll(optionDocuments.map { document -> document.reference })
            addAll(yelpPoolDocuments.map { document -> document.reference })
            addAll(eventDocuments.map { document -> document.reference })
            add(tripRef)
        }.distinctBy { reference -> reference.path }

        deleteRefs.chunked(MAX_BATCH_DELETE_SIZE).forEach { chunk ->
            db.runBatch { batch ->
                chunk.forEach { reference ->
                    batch.delete(reference)
                }
            }.await()
        }
        tripSyncRemoteDataSource.removeTripIndexes(key, affectedViewerUids)
    }

    private suspend fun findLatestOwnedActiveTripKey(ownerUid: String): TripKey? {
        var lastDocument: DocumentSnapshot? = null

        while (true) {
            TripPerformanceLogger.recordTripQuery(
                source = "FirestoreTripRepository.getLatestActiveTripKey",
                detail = "ownerUid=$ownerUid"
            )
            var query: Query = tripsCollection(ownerUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val documents = query.get().await().documents
            if (documents.isEmpty()) return null

            val activeTrip = documents.firstOrNull { document ->
                !document.getString("status").orEmpty().equals("archived", ignoreCase = true)
            }
            if (activeTrip != null) {
                return TripKey(ownerUid = ownerUid, tripId = activeTrip.id)
            }

            lastDocument = documents.last()
        }
    }

    private suspend fun getOwnedTripSummaries(ownerUid: String): List<Itinerary> {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.getTripSummaries",
            detail = "ownerUid=$ownerUid"
        )
        return tripsCollection(ownerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { document -> mapTripSummary(document, ownerUid) }
    }

    private suspend fun getSharedTripSummariesSafe(viewerUid: String): List<Itinerary> {
        return runCatching {
            getSharedTripSummariesByMembership(viewerUid)
        }.getOrElse { error ->
            if (!shouldFallbackSharedTripQuery(error)) {
                throw error
            }

            Log.w(
                TAG,
                "Shared trip membership query unavailable for viewer $viewerUid. Falling back to chat/group discovery.",
                error
            )
            getSharedTripSummariesFromFallback(viewerUid)
        }
    }

    private suspend fun getSharedTripSummariesByMembership(viewerUid: String): List<Itinerary> {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.getSharedTripSummaries",
            detail = "viewerUid=$viewerUid"
        )
        return db.collectionGroup("trips")
            .whereArrayContains("memberUids", viewerUid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val ownerUid = document.inferredOwnerUid()
                if (ownerUid.isBlank() || ownerUid == viewerUid) return@mapNotNull null
                mapTripSummary(document, ownerUid)
            }
    }

    private suspend fun getSharedTripSummariesFromFallback(viewerUid: String): List<Itinerary> =
        coroutineScope {
            val linkedGroupTripKeys = async { linkedGroupTripKeysForViewer(viewerUid) }
            val sharedMessageTripKeys = async { sharedMessageTripKeysForViewer(viewerUid) }

            (linkedGroupTripKeys.await() + sharedMessageTripKeys.await())
                .distinctBy { key -> "${key.ownerUid}:${key.tripId}" }
                .map { key ->
                    async {
                        runCatching {
                            getTripSummary(key)
                        }.getOrElse { error ->
                            Log.w(
                                TAG,
                                "Shared trip fallback could not load ${key.ownerUid}/${key.tripId}.",
                                error
                            )
                            null
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

    private suspend fun linkedGroupMembers(key: TripKey): Set<String> {
        return db.collection("groups")
            .whereEqualTo("linkedTripId", key.tripId)
            .whereEqualTo("linkedTripOwnerId", key.ownerUid)
            .get()
            .await()
            .documents
            .flatMap { document ->
                (document.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            }
            .filter { memberUid -> memberUid != key.ownerUid }
            .toSet()
    }

    private suspend fun linkedGroupTripKeysForViewer(viewerUid: String): List<TripKey> {
        return runCatching {
            db.collection("groups")
                .whereArrayContains("members", viewerUid)
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    val ownerUid = document.getString("linkedTripOwnerId").orEmpty()
                    val tripId = document.getString("linkedTripId").orEmpty()
                    if (ownerUid.isBlank() || tripId.isBlank() || ownerUid == viewerUid) {
                        null
                    } else {
                        TripKey(ownerUid = ownerUid, tripId = tripId)
                    }
                }
        }.getOrElse { error ->
            Log.w(TAG, "Linked-group shared trip fallback failed for viewer $viewerUid.", error)
            emptyList()
        }
    }

    private suspend fun legacySharedTripMembers(key: TripKey): Set<String> = coroutineScope {
        val sharedMessages = runCatching {
            db.collectionGroup("messages")
                .whereEqualTo("messageType", "trip_card")
                .whereEqualTo("ownerUid", key.ownerUid)
                .whereEqualTo("sharedTripId", key.tripId)
                .get()
                .await()
                .documents
        }.getOrElse { error ->
            Log.w(TAG, "Legacy share backfill query failed for trip ${key.tripId}.", error)
            emptyList()
        }

        sharedMessages
            .mapNotNull { document -> document.reference.parent.parent }
            .distinctBy { reference -> reference.path }
            .map { parentRef ->
                async {
                    runCatching {
                        parentRef.get().await()
                    }.getOrNull()
                }
            }
            .awaitAll()
            .mapNotNull { snapshot ->
                snapshot?.get("members") as? List<*>
            }
            .flatten()
            .filterIsInstance<String>()
            .filter { memberUid -> memberUid != key.ownerUid }
            .toSet()
    }

    private suspend fun sharedMessageTripKeysForViewer(viewerUid: String): List<TripKey> =
        coroutineScope {
            val groupRefs = async { chatContainerRefs("groups", viewerUid) }
            val directRefs = async { chatContainerRefs("directChats", viewerUid) }

            (groupRefs.await() + directRefs.await())
                .distinctBy { reference -> reference.path }
                .map { chatRef ->
                    async {
                        runCatching {
                            chatRef.collection("messages")
                                .whereEqualTo("messageType", "trip_card")
                                .get()
                                .await()
                                .documents
                                .mapNotNull { document -> document.sharedTripKey(viewerUid) }
                        }.getOrElse { error ->
                            Log.w(TAG, "Trip-card fallback failed for chat ${chatRef.path}.", error)
                            emptyList()
                        }
                    }
                }
                .awaitAll()
                .flatten()
                .distinctBy { key -> "${key.ownerUid}:${key.tripId}" }
        }

    private suspend fun chatContainerRefs(
        collection: String,
        viewerUid: String
    ) = runCatching {
        db.collection(collection)
            .whereArrayContains("members", viewerUid)
            .get()
            .await()
            .documents
            .map { document -> document.reference }
    }.getOrElse { error ->
        Log.w(TAG, "Chat fallback lookup failed for $collection and viewer $viewerUid.", error)
        emptyList()
    }

    private suspend fun loadTripOptionDocumentsForDelete(
        key: TripKey,
        eventDocuments: List<DocumentSnapshot>
    ): List<DocumentSnapshot> = coroutineScope {
        runCatching {
            TripPerformanceLogger.recordOptionQuery(
                source = "FirestoreTripRepository.deleteTrip",
                detail = "bulk ownerUid=${key.ownerUid} tripId=${key.tripId}"
            )
            db.collectionGroup("options")
                .whereEqualTo("ownerUid", key.ownerUid)
                .whereEqualTo("tripId", key.tripId)
                .get()
                .await()
                .documents
        }.getOrElse { error ->
            Log.w(TAG, "Bulk option delete lookup failed for trip ${key.tripId}; using fallback.", error)
            eventDocuments.map { eventDocument ->
                async {
                    TripPerformanceLogger.recordOptionQuery(
                        source = "FirestoreTripRepository.deleteTrip.fallback",
                        detail = "ownerUid=${key.ownerUid} tripId=${key.tripId} eventId=${eventDocument.id}"
                    )
                    eventDocument.reference
                        .collection("options")
                        .get()
                        .await()
                        .documents
                }
            }.awaitAll().flatten()
        }
    }

    private suspend fun loadTripYelpPoolDocumentsForDelete(
        key: TripKey
    ): List<DocumentSnapshot> = coroutineScope {
        listOf(
            YELP_POOL_TYPE_RESTAURANTS,
            YELP_POOL_TYPE_ACTIVITIES
        ).map { poolType ->
            async {
                val poolDocument = tripDocument(key)
                    .collection("optionPools")
                    .document(poolType)
                    .get()
                    .await()
                val itemDocuments = tripDocument(key)
                    .collection("optionPools")
                    .document(poolType)
                    .collection("items")
                    .get()
                    .await()
                    .documents

                buildList {
                    if (poolDocument.exists()) add(poolDocument)
                    addAll(itemDocuments)
                }
            }
        }.awaitAll().flatten()
    }

    private suspend fun backfillMissingOptionScope(
        key: TripKey,
        eventId: String,
        documents: List<DocumentSnapshot>
    ) {
        val staleDocuments = documents.filter { document ->
            document.getString("ownerUid").isNullOrBlank() ||
                document.getString("tripId").isNullOrBlank() ||
                document.getString("eventId").isNullOrBlank()
        }
        if (staleDocuments.isEmpty()) return

        db.runBatch { batch ->
            staleDocuments.forEach { document ->
                batch.set(
                    document.reference,
                    mapOf(
                        "ownerUid" to key.ownerUid,
                        "tripId" to key.tripId,
                        "eventId" to eventId
                    ),
                    SetOptions.merge()
                )
            }
        }.await()
    }

    private fun QuerySnapshot?.toTripEvents(tripId: String): List<TravelEvent> {
        return this?.documents.orEmpty().mapNotNull { document ->
            val data = document.data ?: return@mapNotNull null
            TravelEvent.fromFirestoreMap(
                map = data,
                documentId = document.id,
                fallbackItineraryId = tripId
            )
        }
    }

    private fun mapTripSummary(document: DocumentSnapshot, ownerUid: String): Itinerary? {
        return runCatching {
            Itinerary(
                itineraryId = document.id,
                userId = ownerUid,
                tripName = resolveTripName(
                    document.getString("tripName"),
                    document.getString("destination") ?: ""
                ),
                destination = document.getString("destination") ?: "",
                origin = document.getString("origin") ?: "",
                originIata = document.getString("originIata") ?: "",
                destinationIata = document.getString("destinationIata") ?: "",
                timeZoneId = document.getString("timeZoneId") ?: "",
                dateFrom = document.getString("dateFrom") ?: "",
                dateTo = document.getString("dateTo") ?: "",
                durationDays = (document.getLong("durationDays") ?: 0L).toInt(),
                currency = document.getString("currency") ?: "USD",
                travelStyle = document.getString("travelStyle") ?: "",
                adults = (document.getLong("adults") ?: 1L).toInt(),
                children = (document.getLong("children") ?: 0L).toInt(),
                createdAt = document.getString("createdAt") ?: "",
                status = document.getString("status") ?: "",
                eventIds = (document.get("eventIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                homeImageUrl = document.getString("homeImageUrl") ?: "",
                ownerUid = ownerUid,
                memberUids = document.memberUids().ifEmpty { listOf(ownerUid) },
                roleByUid = document.roleByUid().ifEmpty {
                    mapOf(ownerUid to TripAccessRole.OWNER.wireValue)
                },
                accessSchemaVersion = document.accessSchemaVersion().coerceAtLeast(0),
                summaryVersion = document.getLong("summaryVersion") ?: 0L,
                eventsVersion = document.getLong("eventsVersion") ?: 0L,
                optionsVersion = document.getLong("optionsVersion") ?: 0L,
                membersVersion = document.getLong("membersVersion") ?: 0L,
                updatedAtEpochMs = document.getLong("updatedAtEpochMs") ?: 0L
            )
        }.getOrNull()
    }

    private fun normalizeTripMembers(ownerUid: String, memberUids: List<String>): List<String> {
        val normalized = memberUids
            .filter { it.isNotBlank() }
            .distinct()
        return if (normalized.size == 1 && normalized.firstOrNull() == ownerUid) {
            emptyList()
        } else {
            normalized
        }
    }

    private fun DocumentSnapshot.inferredOwnerUid(): String {
        return getString("ownerUid").orEmpty().ifBlank {
            reference.parent.parent?.id.orEmpty()
        }
    }

    private fun DocumentSnapshot.sharedTripKey(viewerUid: String): TripKey? {
        val ownerUid = getString("ownerUid").orEmpty()
        val tripId = getString("sharedTripId").orEmpty()
        return if (ownerUid.isBlank() || tripId.isBlank() || ownerUid == viewerUid) {
            null
        } else {
            TripKey(ownerUid = ownerUid, tripId = tripId)
        }
    }

    private fun DocumentSnapshot.memberUids(): List<String> {
        return (get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
    }

    private fun DocumentSnapshot.roleByUid(): Map<String, String> {
        return (get("roleByUid") as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                val value = entry.value as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()
    }

    private fun DocumentSnapshot.accessSchemaVersion(): Int {
        return (getLong("accessSchemaVersion") ?: 0L).toInt()
    }

    private fun tripDocument(key: TripKey) = tripsCollection(key.ownerUid).document(key.tripId)

    private fun tripsCollection(ownerUid: String) = db.collection("users")
        .document(ownerUid)
        .collection("trips")

    private companion object {
        private const val MAX_BATCH_DELETE_SIZE = 450
        private const val TAG = "FirestoreTripRepository"
    }
}

internal fun shouldFallbackSharedTripQuery(error: Throwable): Boolean {
    val firestoreCodeName = runCatching {
        if (error::class.java.name != "com.google.firebase.firestore.FirebaseFirestoreException") {
            null
        } else {
            error.javaClass.getMethod("getCode").invoke(error)?.toString()?.substringAfterLast('.')
        }
    }.getOrNull()

    if (firestoreCodeName == "FAILED_PRECONDITION" || firestoreCodeName == "PERMISSION_DENIED") {
        return true
    }

    val message = error.message.orEmpty().lowercase()
    if ("permission denied" in message || "missing or insufficient permissions" in message) {
        return true
    }

    return "collection_group_contains" in message ||
        "collections_group_contains" in message ||
        "requires an index" in message
}
