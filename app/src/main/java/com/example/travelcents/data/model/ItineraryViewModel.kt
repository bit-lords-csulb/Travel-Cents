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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ItineraryViewModel", "User is not logged in!")
            return
        }

        // Step 1: Find the most recently created trip
        db.collection("users").document(uid)
            .collection("trips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { tripSnapshot ->
                if (tripSnapshot.isEmpty) {
                    Log.d("ItineraryViewModel", "No trips found for user.")
                    return@addOnSuccessListener
                }

                // Step 2: Grab the ID of that newest trip
                val newestTripId = tripSnapshot.documents.first().id

                // Step 3: Trigger the event listener using that ID
                listenToEvents(uid, newestTripId)
            }
            .addOnFailureListener { e ->
                Log.e("ItineraryViewModel", "Error fetching latest trip", e)
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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ItineraryViewModel", "User is not logged in!")
            return
        }

        if (tripId != null) {
            // Scenario A: We were given a specific ID (from the Home screen)
            listenToEvents(uid, tripId)
        } else {
            // Scenario B: We were given nothing (from the Current tab)
            fetchLatestItinerary(uid) // Make sure to update fetchLatestItinerary to accept the uid!
        }
    }
}