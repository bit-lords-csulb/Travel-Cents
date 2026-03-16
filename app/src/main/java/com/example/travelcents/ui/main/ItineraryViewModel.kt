package com.example.travelcents.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.travelcents.data.model.TravelEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItineraryViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    private val _tripTitle = MutableStateFlow("Loading Trip...")
    val tripTitle: StateFlow<String> = _tripTitle.asStateFlow()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun fetchLatestItinerary(uid: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { tripSnapshot ->
                if (tripSnapshot.isEmpty) {
                    Log.d("ItineraryViewModel", "No trips found.")
                    return@addOnSuccessListener
                }

                val doc = tripSnapshot.documents.first()
                val newestTripId = doc.id

                _tripTitle.value = doc.getString("tripName") ?: "Unnamed Trip"

                listenToEvents(uid, newestTripId)
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
            }
    }

    private fun fetchTripTitle(uid: String, tripId: String) {
        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .get()
            .addOnSuccessListener { doc ->
                _tripTitle.value = doc.getString("tripName") ?: "Unnamed Trip"
            }
    }

    private fun listenToEvents(uid: String, tripId: String) {
        snapshotListener = db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ItineraryViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fetchedEvents = snapshot.documents.mapNotNull { doc ->
                        val allData = doc.data ?: emptyMap()

                        val coreKeys = listOf(
                            "eventId",
                            "type",
                            "itineraryId",
                            "tz",
                            "date",
                            "startTime",
                            "endTime"
                        )
                        val detailsMap =
                            allData.filterKeys { it !in coreKeys }.mapValues { it.value.toString() }

                        TravelEvent(
                            eventId = doc.getString("eventId") ?: "",
                            type = doc.getString("type") ?: "unknown",
                            itineraryId = doc.getString("itineraryId") ?: "",
                            tz = doc.getString("tz") ?: "",
                            date = doc.getString("date") ?: "",
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            details = detailsMap
                        )
                    }

                    _events.value = fetchedEvents
                }
            }
    }


    fun loadTrip(tripId: String? = null) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Log.e("ItineraryViewModel", "UID is NULL. Firebase isn't ready yet.")
            return
        }

        Log.d("ItineraryViewModel", "UID found: $uid. Fetching trip: ${tripId ?: "Latest"}")

        if (tripId != null) {
            fetchTripTitle(uid, tripId)
            listenToEvents(uid, tripId)
        } else {
            fetchLatestItinerary(uid)
        }
    }
    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
    }
}


