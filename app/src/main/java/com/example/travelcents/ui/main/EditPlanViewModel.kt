package com.example.travelcents.ui.itinerary

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditPlanViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI State variables
    var title by mutableStateOf("")
    var date by mutableStateOf("")
    var time by mutableStateOf("")
    var location by mutableStateOf("")
    var notes by mutableStateOf("")

    /**
     * Fetches a specific event from the nested Firestore path:
     * users/{uid}/trips/{tripId}/events/{eventId}
     */
    fun loadEventDetails(eventId: String?, tripId: String?) {
        val uid = auth.currentUser?.uid
        if (uid == null || eventId == null || tripId == null) {
            Log.e("EditPlanViewModel", "Missing required IDs: uid=$uid, eventId=$eventId, tripId=$tripId")
            return
        }

        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val allData = document.data ?: emptyMap()

                    // We extract 'type' to use as a fallback for the title
                    val type = document.getString("type") ?: ""

                    // Logic to find the best title: check field 'title', then 'activity_name', then fallback to type
                    title = document.getString("title")
                        ?: document.getString("activity_name")
                                ?: type.replaceFirstChar { it.uppercase() }

                    date = document.getString("date") ?: ""

                    // Maps to the startTime field in your TravelEvent model
                    time = document.getString("startTime") ?: ""

                    // Checks top-level flattened details
                    location = document.getString("location") ?: document.getString("destination_airport") ?: ""
                    notes = document.getString("notes") ?: document.getString("description") ?: ""

                    Log.d("EditPlanViewModel", "Successfully loaded event: $title")
                } else {
                    Log.d("EditPlanViewModel", "No document found at path")
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditPlanViewModel", "Fetch Error: ${e.message}")
            }
    }

    /**
     * Updates the event document at the same nested path
     */
    fun updateEvent(eventId: String?, tripId: String?, onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || eventId == null || tripId == null) return

        val updatedData = mapOf(
            "title" to title,
            "date" to date,
            "startTime" to time,
            "location" to location,
            "notes" to notes
        )

        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .update(updatedData)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("EditPlanViewModel", "Update Failed: ${e.message}")
            }
    }
}