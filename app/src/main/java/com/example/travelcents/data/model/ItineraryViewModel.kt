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

    // This is the bucket that will hold the data for your UI
    private val _events = MutableStateFlow<List<TravelEvent>>(emptyList())
    val events: StateFlow<List<TravelEvent>> = _events.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun fetchItinerary(tripId: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ItineraryViewModel", "User is not logged in!")
            return
        }

        // Following your teammate's exact path!
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
                        // Because your teammate flattened the data, we reconstruct it here
                        val allData = doc.data ?: emptyMap()

                        // Separate the core fields from the "details"
                        val coreKeys = listOf("eventId", "type", "itineraryId", "tz", "date", "startTime", "endTime")
                        val detailsMap = allData.filterKeys { it !in coreKeys }.mapValues { it.value.toString() }

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

                    // Pour the data into the bucket!
                    _events.value = fetchedEvents
                }
            }
    }
}