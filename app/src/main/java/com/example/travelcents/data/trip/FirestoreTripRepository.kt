package com.example.travelcents.data.trip

import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.resolveTripName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreTripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TripRepository {

    override suspend fun getLatestActiveTripKey(viewerUid: String): TripKey? {
        var lastDocument: DocumentSnapshot? = null

        while (true) {
            TripPerformanceLogger.recordTripQuery(
                source = "FirestoreTripRepository.getLatestActiveTripKey",
                detail = "ownerUid=$viewerUid"
            )
            var query: Query = tripsCollection(viewerUid)
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
                return TripKey(ownerUid = viewerUid, tripId = activeTrip.id)
            }

            lastDocument = documents.last()
        }
    }

    override suspend fun getTripSummaries(viewerUid: String): List<Itinerary> {
        TripPerformanceLogger.recordTripQuery(
            source = "FirestoreTripRepository.getTripSummaries",
            detail = "ownerUid=$viewerUid"
        )
        return tripsCollection(viewerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { mapTripSummary(it, viewerUid) }
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
            detail = "tripId=${key.tripId}"
        )
        return db.collection("groups")
            .whereEqualTo("linkedTripId", key.tripId)
            .get()
            .await()
            .documents
            .flatMap { doc ->
                (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            }
            .distinct()
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
                homeImageUrl = document.getString("homeImageUrl") ?: ""
            )
        }.getOrNull()
    }

    private fun tripDocument(key: TripKey) = tripsCollection(key.ownerUid).document(key.tripId)

    private fun tripsCollection(ownerUid: String) = db.collection("users")
        .document(ownerUid)
        .collection("trips")
}
