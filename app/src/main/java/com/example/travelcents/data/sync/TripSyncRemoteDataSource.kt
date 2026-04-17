package com.example.travelcents.data.sync

import com.example.travelcents.data.trip.TripAccessRole
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.local.trip.LocalUserStub
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.resolveTripName
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class TripManifestRemote(
    val manifestVersion: Long,
    val tripCount: Int,
    val latestActiveTripKey: TripKey?
)

data class RemoteTripMember(
    val memberUid: String,
    val role: String,
    val displayName: String,
    val avatarUrl: String
)

class TripSyncRemoteDataSource(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : HomeSyncRemoteSource {
    override suspend fun fetchManifest(viewerUid: String): TripManifestRemote? {
        val snapshot = manifestDocument(viewerUid).get().await()
        if (!snapshot.exists()) return null

        val manifestVersion = snapshot.getLong("manifestVersion") ?: return null
        val latestOwnerUid = snapshot.getString("latestActiveOwnerUid").orEmpty()
        val latestTripId = snapshot.getString("latestActiveTripId").orEmpty()

        return TripManifestRemote(
            manifestVersion = manifestVersion,
            tripCount = (snapshot.getLong("tripCount") ?: 0L).toInt(),
            latestActiveTripKey = if (latestOwnerUid.isBlank() || latestTripId.isBlank()) {
                null
            } else {
                TripKey(ownerUid = latestOwnerUid, tripId = latestTripId)
            }
        )
    }

    override suspend fun fetchTripRefs(viewerUid: String): List<Itinerary> {
        return tripRefsCollection(viewerUid)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val ownerUid = document.getString("ownerUid").orEmpty()
                val tripId = document.getString("tripId").orEmpty()
                if (ownerUid.isBlank() || tripId.isBlank()) return@mapNotNull null

                Itinerary(
                    itineraryId = tripId,
                    userId = ownerUid,
                    tripName = resolveTripName(
                        document.getString("tripName"),
                        document.getString("destination").orEmpty()
                    ),
                    destination = document.getString("destination").orEmpty(),
                    origin = document.getString("origin").orEmpty(),
                    originIata = document.getString("originIata").orEmpty(),
                    destinationIata = document.getString("destinationIata").orEmpty(),
                    dateFrom = document.getString("dateFrom").orEmpty(),
                    dateTo = document.getString("dateTo").orEmpty(),
                    durationDays = (document.getLong("durationDays") ?: 0L).toInt(),
                    currency = document.getString("currency") ?: "USD",
                    travelStyle = document.getString("travelStyle").orEmpty(),
                    adults = (document.getLong("adults") ?: 1L).toInt(),
                    children = (document.getLong("children") ?: 0L).toInt(),
                    createdAt = document.getString("createdAt").orEmpty(),
                    status = document.getString("status").orEmpty(),
                    eventIds = (document.get("eventIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                    homeImageUrl = document.getString("homeImageUrl").orEmpty(),
                    ownerUid = ownerUid,
                    memberUids = (document.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        .ifEmpty { listOf(ownerUid) },
                    roleByUid = (document.get("roleByUid") as? Map<*, *>)
                        ?.mapNotNull { entry ->
                            val key = entry.key as? String ?: return@mapNotNull null
                            val value = entry.value as? String ?: return@mapNotNull null
                            key to value
                        }
                        ?.toMap()
                        .orEmpty()
                        .ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) },
                    accessSchemaVersion = (document.getLong("accessSchemaVersion") ?: Itinerary.ACCESS_SCHEMA_VERSION.toLong()).toInt(),
                    summaryVersion = document.getLong("summaryVersion") ?: 0L,
                    eventsVersion = document.getLong("eventsVersion") ?: 0L,
                    optionsVersion = document.getLong("optionsVersion") ?: 0L,
                    membersVersion = document.getLong("membersVersion") ?: 0L,
                    updatedAtEpochMs = document.getLong("updatedAtEpochMs") ?: 0L
                )
            }
            .distinctBy { itinerary -> "${itinerary.ownerUid}:${itinerary.itineraryId}" }
            .sortedWith(
                compareByDescending<Itinerary> { trip -> trip.createdAt }
                    .thenByDescending { trip -> trip.dateFrom }
            )
    }

    suspend fun fetchTripRef(
        viewerUid: String,
        tripKey: TripKey
    ): Itinerary? {
        val snapshot = tripRefDocument(viewerUid, tripKey).get().await()
        if (!snapshot.exists()) return null

        val ownerUid = snapshot.getString("ownerUid").orEmpty()
        val tripId = snapshot.getString("tripId").orEmpty()
        if (ownerUid.isBlank() || tripId.isBlank()) return null

        return Itinerary(
            itineraryId = tripId,
            userId = ownerUid,
            tripName = resolveTripName(
                snapshot.getString("tripName"),
                snapshot.getString("destination").orEmpty()
            ),
            destination = snapshot.getString("destination").orEmpty(),
            origin = snapshot.getString("origin").orEmpty(),
            originIata = snapshot.getString("originIata").orEmpty(),
            destinationIata = snapshot.getString("destinationIata").orEmpty(),
            dateFrom = snapshot.getString("dateFrom").orEmpty(),
            dateTo = snapshot.getString("dateTo").orEmpty(),
            durationDays = (snapshot.getLong("durationDays") ?: 0L).toInt(),
            currency = snapshot.getString("currency") ?: "USD",
            travelStyle = snapshot.getString("travelStyle").orEmpty(),
            adults = (snapshot.getLong("adults") ?: 1L).toInt(),
            children = (snapshot.getLong("children") ?: 0L).toInt(),
            createdAt = snapshot.getString("createdAt").orEmpty(),
            status = snapshot.getString("status").orEmpty(),
            eventIds = (snapshot.get("eventIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            homeImageUrl = snapshot.getString("homeImageUrl").orEmpty(),
            ownerUid = ownerUid,
            memberUids = (snapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                .ifEmpty { listOf(ownerUid) },
            roleByUid = (snapshot.get("roleByUid") as? Map<*, *>)
                ?.mapNotNull { entry ->
                    val key = entry.key as? String ?: return@mapNotNull null
                    val value = entry.value as? String ?: return@mapNotNull null
                    key to value
                }
                ?.toMap()
                .orEmpty()
                .ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) },
            accessSchemaVersion = (snapshot.getLong("accessSchemaVersion")
                ?: Itinerary.ACCESS_SCHEMA_VERSION.toLong()).toInt(),
            summaryVersion = snapshot.getLong("summaryVersion") ?: 0L,
            eventsVersion = snapshot.getLong("eventsVersion") ?: 0L,
            optionsVersion = snapshot.getLong("optionsVersion") ?: 0L,
            membersVersion = snapshot.getLong("membersVersion") ?: 0L,
            updatedAtEpochMs = snapshot.getLong("updatedAtEpochMs") ?: 0L
        )
    }

    suspend fun fetchTripSummary(tripKey: TripKey): Itinerary? {
        val snapshot = tripDocument(tripKey).get().await()
        if (!snapshot.exists()) return null
        return snapshot.toItinerary(ownerUid = tripKey.ownerUid)
    }

    suspend fun fetchTripEvents(tripKey: TripKey): List<TravelEvent> {
        return tripDocument(tripKey)
            .collection("events")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                TravelEvent.fromFirestoreMap(
                    map = data,
                    documentId = document.id,
                    fallbackItineraryId = tripKey.tripId
                )
            }
    }

    suspend fun fetchTripMembers(
        tripKey: TripKey,
        cachedUserStubs: Map<String, LocalUserStub> = emptyMap()
    ): List<RemoteTripMember> {
        val snapshot = tripDocument(tripKey).get().await()
        if (!snapshot.exists()) return emptyList()

        val ownerUid = snapshot.getString("ownerUid").orEmpty().ifBlank { tripKey.ownerUid }
        val memberUids = (snapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>()
            .orEmpty()
            .ifEmpty { listOf(ownerUid) }
        val roleByUid = (snapshot.get("roleByUid") as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                val value = entry.value as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()
            .ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) }

        val missingUserUids = memberUids.filterNot { memberUid -> memberUid in cachedUserStubs }
        val fetchedProfiles = fetchUserProfileMap(missingUserUids)
        return memberUids.map { memberUid ->
            val cachedStub = cachedUserStubs[memberUid]
            val profile = fetchedProfiles[memberUid]
            RemoteTripMember(
                memberUid = memberUid,
                role = roleByUid[memberUid]
                    ?: if (memberUid == ownerUid) TripAccessRole.OWNER.wireValue else TripAccessRole.VIEWER.wireValue,
                displayName = cachedStub?.displayName
                    .orEmpty()
                    .ifBlank { profile?.first.orEmpty() }
                    .ifBlank { memberUid },
                avatarUrl = cachedStub?.avatarUrl
                    .orEmpty()
                    .ifBlank { profile?.second.orEmpty() }
            )
        }
    }

    suspend fun fetchTripOptionsBulk(tripKey: TripKey): Map<String, List<EventOption>> {
        return runCatching {
            db.collectionGroup("options")
                .whereEqualTo("ownerUid", tripKey.ownerUid)
                .whereEqualTo("tripId", tripKey.tripId)
                .get()
                .await()
                .documents
                .map { document ->
                    val raw = document.data ?: emptyMap()
                    EventOption.fromMap(
                        raw + mapOf(
                            "optionId" to (raw["optionId"]?.toString() ?: document.id),
                            "eventId" to raw["eventId"]?.toString().orEmpty(),
                            "tripId" to (raw["tripId"]?.toString().orEmpty().ifBlank { tripKey.tripId }),
                            "ownerUid" to (raw["ownerUid"]?.toString().orEmpty().ifBlank { tripKey.ownerUid })
                        )
                    )
                }
                .groupBy { option -> option.eventId }
                .filterKeys { eventId -> eventId.isNotBlank() }
                .mapValues { (_, options) ->
                    options.sortedByDescending { option -> option.selected }
                }
        }.getOrElse { error ->
            if (!shouldFallbackOptionQuery(error)) throw error
            fetchTripOptionsByEventFallback(tripKey)
        }
    }

    suspend fun createTrip(ownerUid: String, itinerary: Itinerary, events: List<TravelEvent>) {
        val tripKey = TripKey(ownerUid = ownerUid, tripId = itinerary.itineraryId)
        val version = nextVersionToken()
        val tripData = itinerary.copy(
            ownerUid = ownerUid,
            userId = ownerUid,
            memberUids = itinerary.memberUids.ifEmpty { listOf(ownerUid) },
            roleByUid = itinerary.roleByUid.ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) },
            summaryVersion = version,
            eventsVersion = version,
            optionsVersion = version,
            membersVersion = version,
            updatedAtEpochMs = version
        ).toFirestoreMap() + timestampMetadata(version)

        val pendingWrites = buildList<PendingSet> {
            events.forEach { event ->
                val eventRef = eventDocument(tripKey, event.eventId)
                add(
                    PendingSet(
                        reference = eventRef,
                        data = event.toFirestoreMap() + timestampMetadata(version)
                    )
                )
                event.options.forEach { option ->
                    add(
                        PendingSet(
                            reference = eventRef.collection("options").document(option.optionId),
                            data = option.scopedTo(
                                ownerUid = ownerUid,
                                tripId = itinerary.itineraryId,
                                eventId = event.eventId
                            ).toMap() + timestampMetadata(version)
                        )
                    )
                }
            }
        }

        val writeChunks = pendingWrites.chunked(MAX_CREATE_TRIP_BATCH_WRITES - 1)
        writeChunks.dropLast(1).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { write -> batch.set(write.reference, write.data) }
            batch.commit().await()
        }

        val finalBatch = db.batch()
        writeChunks.lastOrNull().orEmpty().forEach { write ->
            finalBatch.set(write.reference, write.data)
        }
        finalBatch.set(tripDocument(tripKey), tripData)
        finalBatch.commit().await()

        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun updateTripSummaryFields(
        tripKey: TripKey,
        fields: Map<String, Any>
    ) {
        val version = nextVersionToken()
        tripDocument(tripKey).set(
            fields + mapOf(
                "summaryVersion" to version
            ) + timestampMetadata(version),
            SetOptions.merge()
        ).await()
        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun updateHomeImage(
        tripKey: TripKey,
        imageUrl: String
    ) {
        updateTripSummaryFields(
            tripKey = tripKey,
            fields = mapOf("homeImageUrl" to imageUrl)
        )
    }

    suspend fun upsertEvent(
        tripKey: TripKey,
        event: TravelEvent
    ) {
        val version = nextVersionToken()
        db.runBatch { batch ->
            batch.set(
                eventDocument(tripKey, event.eventId),
                event.toFirestoreMap() + timestampMetadata(version)
            )
            batch.set(
                tripDocument(tripKey),
                mapOf(
                    "eventsVersion" to version,
                    "eventIds" to FieldValue.arrayUnion(event.eventId)
                ) + timestampMetadata(version),
                SetOptions.merge()
            )
        }.await()
        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun deleteEvent(
        tripKey: TripKey,
        eventId: String
    ) {
        val version = nextVersionToken()
        db.runBatch { batch ->
            batch.delete(eventDocument(tripKey, eventId))
            batch.set(
                tripDocument(tripKey),
                mapOf(
                    "eventsVersion" to version,
                    "eventIds" to FieldValue.arrayRemove(eventId)
                ) + timestampMetadata(version),
                SetOptions.merge()
            )
        }.await()
        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun persistEventPlacements(
        tripKey: TripKey,
        events: List<TravelEvent>
    ) {
        if (events.isEmpty()) return

        val version = nextVersionToken()
        db.runBatch { batch ->
            events.forEach { event ->
                batch.set(
                    eventDocument(tripKey, event.eventId),
                    mapOf(
                        "date" to event.date,
                        "sortOrder" to (event.details["sortOrder"] ?: "0")
                    ) + timestampMetadata(version),
                    SetOptions.merge()
                )
            }
            batch.set(
                tripDocument(tripKey),
                mapOf("eventsVersion" to version) + timestampMetadata(version),
                SetOptions.merge()
            )
        }.await()
        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun persistEventAndOptions(
        tripKey: TripKey,
        eventId: String,
        event: TravelEvent,
        options: List<EventOption>,
        updatedOptionIds: Set<String>? = null
    ) {
        val version = nextVersionToken()
        db.runBatch { batch ->
            val eventRef = eventDocument(tripKey, eventId)
            batch.set(eventRef, event.toFirestoreMap() + timestampMetadata(version))
            options
                .filter { option -> updatedOptionIds == null || option.optionId in updatedOptionIds }
                .forEach { option ->
                    batch.set(
                        eventRef.collection("options").document(option.optionId),
                        option.scopedTo(
                            ownerUid = tripKey.ownerUid,
                            tripId = tripKey.tripId,
                            eventId = eventId
                        ).toMap() + timestampMetadata(version)
                    )
                }
            batch.set(
                tripDocument(tripKey),
                mapOf("optionsVersion" to version) + timestampMetadata(version),
                SetOptions.merge()
            )
        }.await()
        runCatching {
            refreshTripIndexesForTrip(tripKey)
        }
    }

    suspend fun refreshTripIndexesForTrip(
        tripKey: TripKey,
        removedViewerUids: Set<String> = emptySet()
    ) {
        val snapshot = tripDocument(tripKey).get().await()
        if (!snapshot.exists()) return

        val ownerUid = snapshot.getString("ownerUid").orEmpty().ifBlank { tripKey.ownerUid }
        val memberUids = (snapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>()
            .orEmpty()
            .ifEmpty { listOf(ownerUid) }
        val roleByUid = (snapshot.get("roleByUid") as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                val value = entry.value as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()
            .ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) }

        val metadataBackfill = mutableMapOf<String, Any>()
        if ((snapshot.getLong("summaryVersion") ?: 0L) == 0L) metadataBackfill["summaryVersion"] = nextVersionToken()
        if ((snapshot.getLong("eventsVersion") ?: 0L) == 0L) metadataBackfill["eventsVersion"] = nextVersionToken()
        if ((snapshot.getLong("optionsVersion") ?: 0L) == 0L) metadataBackfill["optionsVersion"] = nextVersionToken()
        if ((snapshot.getLong("membersVersion") ?: 0L) == 0L) metadataBackfill["membersVersion"] = nextVersionToken()
        if ((snapshot.getLong("updatedAtEpochMs") ?: 0L) == 0L) metadataBackfill["updatedAtEpochMs"] = nextVersionToken()
        if (snapshot.getString("ownerUid").isNullOrBlank()) metadataBackfill["ownerUid"] = ownerUid
        if ((snapshot.getLong("accessSchemaVersion") ?: 0L) < Itinerary.ACCESS_SCHEMA_VERSION) {
            metadataBackfill["accessSchemaVersion"] = Itinerary.ACCESS_SCHEMA_VERSION
        }

        if (metadataBackfill.isNotEmpty()) {
            tripDocument(tripKey).set(
                metadataBackfill + mapOf("updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            ).await()
        }

        val refreshedSnapshot = if (metadataBackfill.isEmpty()) snapshot else tripDocument(tripKey).get().await()
        val currentMembers = (refreshedSnapshot.get("memberUids") as? List<*>)?.filterIsInstance<String>()
            .orEmpty()
            .ifEmpty { listOf(ownerUid) }
        val currentRoles = (refreshedSnapshot.get("roleByUid") as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                val value = entry.value as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            .orEmpty()
            .ifEmpty { roleByUid }

        db.runBatch { batch ->
            currentMembers.forEach { viewerUid ->
                batch.set(
                    tripRefDocument(viewerUid, tripKey),
                    buildTripRefMap(
                        snapshot = refreshedSnapshot.data.orEmpty(),
                        ownerUid = ownerUid,
                        tripId = tripKey.tripId,
                        viewerUid = viewerUid,
                        roleByUid = currentRoles,
                        memberUids = currentMembers
                    )
                )
            }
            removedViewerUids.forEach { viewerUid ->
                batch.delete(tripRefDocument(viewerUid, tripKey))
            }
        }.await()

        (currentMembers + removedViewerUids).distinct().forEach { viewerUid ->
            refreshManifest(viewerUid)
        }
    }

    override suspend fun backfillTripRefsForViewer(
        viewerUid: String,
        trips: List<Itinerary>
    ) {
        val existingTripRefs = tripRefsCollection(viewerUid).get().await().documents
        val desiredIds = trips.map { trip -> "${trip.ownerUid}_${trip.itineraryId}" }.toSet()

        db.runBatch { batch ->
            existingTripRefs
                .filter { document -> document.id !in desiredIds }
                .forEach { document -> batch.delete(document.reference) }
            trips.forEach { trip ->
                val tripKey = TripKey(ownerUid = trip.ownerUid, tripId = trip.itineraryId)
                if (trip.ownerUid == viewerUid) {
                    val tripMetadataPatch = mutableMapOf<String, Any>()
                    if (trip.summaryVersion == 0L) tripMetadataPatch["summaryVersion"] = nextVersionToken()
                    if (trip.eventsVersion == 0L) tripMetadataPatch["eventsVersion"] = nextVersionToken()
                    if (trip.optionsVersion == 0L) tripMetadataPatch["optionsVersion"] = nextVersionToken()
                    if (trip.membersVersion == 0L) tripMetadataPatch["membersVersion"] = nextVersionToken()
                    if (trip.updatedAtEpochMs == 0L) tripMetadataPatch["updatedAtEpochMs"] = nextVersionToken()
                    if (tripMetadataPatch.isNotEmpty()) {
                        batch.set(
                            tripDocument(tripKey),
                            tripMetadataPatch + mapOf("updatedAt" to FieldValue.serverTimestamp()),
                            SetOptions.merge()
                        )
                    }
                }

                batch.set(
                    tripRefDocument(viewerUid, tripKey),
                    buildTripRefMap(
                        snapshot = trip.copy(
                            summaryVersion = trip.summaryVersion.takeIf { it > 0 } ?: nextVersionToken(),
                            eventsVersion = trip.eventsVersion.takeIf { it > 0 } ?: nextVersionToken(),
                            optionsVersion = trip.optionsVersion.takeIf { it > 0 } ?: nextVersionToken(),
                            membersVersion = trip.membersVersion.takeIf { it > 0 } ?: nextVersionToken(),
                            updatedAtEpochMs = trip.updatedAtEpochMs.takeIf { it > 0 } ?: nextVersionToken()
                        ).toFirestoreMap(),
                        ownerUid = trip.ownerUid,
                        tripId = trip.itineraryId,
                        viewerUid = viewerUid,
                        roleByUid = trip.roleByUid,
                        memberUids = trip.memberUids.ifEmpty { listOf(trip.ownerUid) }
                    )
                )
            }
        }.await()

        refreshManifest(viewerUid)
    }

    suspend fun removeTripIndexes(
        tripKey: TripKey,
        affectedViewerUids: Set<String>
    ) {
        if (affectedViewerUids.isEmpty()) return

        db.runBatch { batch ->
            affectedViewerUids.forEach { viewerUid ->
                batch.delete(tripRefDocument(viewerUid, tripKey))
            }
        }.await()

        affectedViewerUids.forEach { viewerUid ->
            runCatching {
                refreshManifest(viewerUid)
            }
        }
    }

    private suspend fun refreshManifest(viewerUid: String) {
        val tripRefs = fetchTripRefs(viewerUid)
        val latestActiveTrip = tripRefs
            .filterNot { trip -> trip.status.equals("archived", ignoreCase = true) }
            .maxByOrNull { trip -> trip.createdAt }

        manifestDocument(viewerUid).set(
            mapOf(
                "manifestVersion" to nextVersionToken(),
                "tripCount" to tripRefs.size,
                "latestActiveOwnerUid" to latestActiveTrip?.ownerUid.orEmpty(),
                "latestActiveTripId" to latestActiveTrip?.itineraryId.orEmpty(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun fetchTripOptionsByEventFallback(
        tripKey: TripKey
    ): Map<String, List<EventOption>> {
        val events = fetchTripEvents(tripKey)
        return events.associate { event ->
            val options = tripDocument(tripKey)
                .collection("events")
                .document(event.eventId)
                .collection("options")
                .get()
                .await()
                .documents
                .map { document ->
                    val raw = document.data ?: emptyMap()
                    EventOption.fromMap(
                        raw + mapOf(
                            "optionId" to (raw["optionId"]?.toString() ?: document.id),
                            "eventId" to (raw["eventId"]?.toString().orEmpty().ifBlank { event.eventId }),
                            "tripId" to (raw["tripId"]?.toString().orEmpty().ifBlank { tripKey.tripId }),
                            "ownerUid" to (raw["ownerUid"]?.toString().orEmpty().ifBlank { tripKey.ownerUid })
                        )
                    )
                }
                .sortedByDescending { option -> option.selected }
            event.eventId to options
        }
    }

    private suspend fun fetchUserProfileMap(
        uids: List<String>
    ): Map<String, Pair<String, String>> {
        if (uids.isEmpty()) return emptyMap()

        return uids.chunked(30).flatMap { chunk ->
            db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .map { document ->
                    val firstName = document.getString("firstName").orEmpty()
                    val lastName = document.getString("lastName").orEmpty()
                    val displayName = "$firstName $lastName".trim()
                        .ifBlank { document.getString("username").orEmpty() }
                        .ifBlank { document.id }
                    val avatarUrl = document.getString("profileImageUrl").orEmpty()
                    document.id to (displayName to avatarUrl)
                }
        }.toMap()
    }

    private fun buildTripRefMap(
        snapshot: Map<String, Any>,
        ownerUid: String,
        tripId: String,
        viewerUid: String,
        roleByUid: Map<String, String>,
        memberUids: List<String>
    ): Map<String, Any> {
        val versionFallback = nextVersionToken()
        return buildMap {
            put("ownerUid", ownerUid)
            put("tripId", tripId)
            put("tripName", snapshot["tripName"]?.toString().orEmpty())
            put("destination", snapshot["destination"]?.toString().orEmpty())
            put("origin", snapshot["origin"]?.toString().orEmpty())
            put("originIata", snapshot["originIata"]?.toString().orEmpty())
            put("destinationIata", snapshot["destinationIata"]?.toString().orEmpty())
            put("dateFrom", snapshot["dateFrom"]?.toString().orEmpty())
            put("dateTo", snapshot["dateTo"]?.toString().orEmpty())
            put("createdAt", snapshot["createdAt"]?.toString().orEmpty())
            put("status", snapshot["status"]?.toString().orEmpty())
            put("currency", snapshot["currency"]?.toString().orEmpty())
            put("travelStyle", snapshot["travelStyle"]?.toString().orEmpty())
            put("durationDays", (snapshot["durationDays"] as? Number)?.toInt() ?: 0)
            put("adults", (snapshot["adults"] as? Number)?.toInt() ?: 1)
            put("children", (snapshot["children"] as? Number)?.toInt() ?: 0)
            put("eventIds", (snapshot["eventIds"] as? List<*>)?.filterIsInstance<String>().orEmpty())
            put("homeImageUrl", snapshot["homeImageUrl"]?.toString().orEmpty())
            put("memberUids", memberUids)
            put("roleByUid", roleByUid)
            put(
                "role",
                roleByUid[viewerUid]
                    ?: if (viewerUid == ownerUid) {
                        TripAccessRole.OWNER.wireValue
                    } else {
                        TripAccessRole.VIEWER.wireValue
                    }
            )
            put("accessSchemaVersion", (snapshot["accessSchemaVersion"] as? Number)?.toInt() ?: Itinerary.ACCESS_SCHEMA_VERSION)
            put("summaryVersion", (snapshot["summaryVersion"] as? Number)?.toLong() ?: versionFallback)
            put("eventsVersion", (snapshot["eventsVersion"] as? Number)?.toLong() ?: versionFallback)
            put("optionsVersion", (snapshot["optionsVersion"] as? Number)?.toLong() ?: versionFallback)
            put("membersVersion", (snapshot["membersVersion"] as? Number)?.toLong() ?: versionFallback)
            put("updatedAtEpochMs", (snapshot["updatedAtEpochMs"] as? Number)?.toLong() ?: versionFallback)
            put("updatedAt", FieldValue.serverTimestamp())
        }
    }

    private fun timestampMetadata(version: Long): Map<String, Any> {
        return mapOf(
            "updatedAtEpochMs" to version,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    private fun nextVersionToken(): Long {
        return System.currentTimeMillis() * 1000L + Random.nextLong(1000L)
    }

    private fun manifestDocument(viewerUid: String) = db.collection("users")
        .document(viewerUid)
        .collection("sync")
        .document("trips")

    private fun tripRefDocument(viewerUid: String, tripKey: TripKey) = tripRefsCollection(viewerUid)
        .document("${tripKey.ownerUid}_${tripKey.tripId}")

    private fun tripRefsCollection(viewerUid: String) = db.collection("users")
        .document(viewerUid)
        .collection("tripRefs")

    private fun tripDocument(tripKey: TripKey) = db.collection("users")
        .document(tripKey.ownerUid)
        .collection("trips")
        .document(tripKey.tripId)

    private fun eventDocument(tripKey: TripKey, eventId: String) = tripDocument(tripKey)
        .collection("events")
        .document(eventId)

    private fun DocumentSnapshot.toItinerary(ownerUid: String): Itinerary? {
        return runCatching {
            Itinerary(
                itineraryId = id,
                userId = ownerUid,
                tripName = resolveTripName(
                    getString("tripName"),
                    getString("destination") ?: ""
                ),
                destination = getString("destination") ?: "",
                origin = getString("origin") ?: "",
                originIata = getString("originIata") ?: "",
                destinationIata = getString("destinationIata") ?: "",
                dateFrom = getString("dateFrom") ?: "",
                dateTo = getString("dateTo") ?: "",
                durationDays = (getLong("durationDays") ?: 0L).toInt(),
                currency = getString("currency") ?: "USD",
                travelStyle = getString("travelStyle") ?: "",
                adults = (getLong("adults") ?: 1L).toInt(),
                children = (getLong("children") ?: 0L).toInt(),
                createdAt = getString("createdAt") ?: "",
                status = getString("status") ?: "",
                eventIds = (get("eventIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                homeImageUrl = getString("homeImageUrl") ?: "",
                ownerUid = ownerUid,
                memberUids = (get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    .ifEmpty { listOf(ownerUid) },
                roleByUid = (get("roleByUid") as? Map<*, *>)
                    ?.mapNotNull { entry ->
                        val key = entry.key as? String ?: return@mapNotNull null
                        val value = entry.value as? String ?: return@mapNotNull null
                        key to value
                    }
                    ?.toMap()
                    .orEmpty()
                    .ifEmpty { mapOf(ownerUid to TripAccessRole.OWNER.wireValue) },
                accessSchemaVersion = (getLong("accessSchemaVersion") ?: Itinerary.ACCESS_SCHEMA_VERSION.toLong()).toInt(),
                summaryVersion = getLong("summaryVersion") ?: 0L,
                eventsVersion = getLong("eventsVersion") ?: 0L,
                optionsVersion = getLong("optionsVersion") ?: 0L,
                membersVersion = getLong("membersVersion") ?: 0L,
                updatedAtEpochMs = getLong("updatedAtEpochMs") ?: 0L
            )
        }.getOrNull()
    }

    private companion object {
        private const val MAX_CREATE_TRIP_BATCH_WRITES = 200
    }
}

private data class PendingSet(
    val reference: DocumentReference,
    val data: Map<String, Any>
)

internal fun shouldFallbackOptionQuery(error: Throwable): Boolean {
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
