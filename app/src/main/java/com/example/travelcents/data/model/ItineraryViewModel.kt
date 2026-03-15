package com.example.travelcents.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.travelcents.data.model.TravelEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.firestore.Query

class ItineraryViewModel : ViewModel() {

    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun fetchLatestItinerary(uid: String) {
        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING) // <-- ADD THIS BACK
            .limit(1)                                         // <-- ADD THIS BACK
            .get()
            .addOnSuccessListener { tripSnapshot ->
                if (tripSnapshot.isEmpty) {
                    Log.d("ItineraryViewModel", "No trips found.")
                    return@addOnSuccessListener
                }

                val doc = tripSnapshot.documents.first()
                val newestTripId = doc.id

                listenToEvents(uid, newestTripId)
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "DATABASE ERROR: ${e.message}")
            }
    }

    private fun listenToEvents(uid: String, tripId: String) {
        // This is your exact same listener code from before!
        db.collection("users").document(uid)
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
        // Grab the UID inside the function to ensure it's fresh
        val uid = auth.currentUser?.uid

        if (uid == null) {
            // If the user isn't ready yet, let's log it so we know for sure
            Log.e("ItineraryViewModel", "UID is NULL. Firebase isn't ready yet.")
            return
        }

        Log.d("ItineraryViewModel", "UID found: $uid. Fetching trip: ${tripId ?: "Latest"}")

        if (tripId != null) {
            listenToEvents(uid, tripId)
        } else {
            fetchLatestItinerary(uid)
        }
    }
}