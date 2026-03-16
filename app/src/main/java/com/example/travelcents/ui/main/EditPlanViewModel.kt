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

    // Core Variables
    var type by mutableStateOf("")
    var title by mutableStateOf("")
    var date by mutableStateOf("")
    var time by mutableStateOf("") // Maps to startTime
    var endTime by mutableStateOf("")
    var location by mutableStateOf("")
    var notes by mutableStateOf("")

    // Type-Specific Variables
    var airline by mutableStateOf("")
    var flightNumber by mutableStateOf("")
    var cuisine by mutableStateOf("")

    fun loadEventDetails(eventId: String?, tripId: String?) {
        val uid = auth.currentUser?.uid
        if (uid == null || eventId == null || tripId == null) return

        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val allData = document.data ?: emptyMap()
                    val detailsMap = allData["details"] as? Map<*, *>

                    // 1. Get the Type
                    type = document.getString("type") ?: ""

                    // 2. Map Core Fields
                    title = document.getString("title")
                        ?: document.getString("activity_name")
                                ?: detailsMap?.get("title")?.toString()
                                ?: document.getString("hotel_name")
                                ?: document.getString("restaurant_name")
                                ?: type.replaceFirstChar { it.uppercase() }

                    date = document.getString("date") ?: ""
                    time = document.getString("startTime") ?: ""
                    endTime = document.getString("endTime") ?: ""

                    location = document.getString("location")
                        ?: document.getString("destination_airport")
                                ?: ""

                    notes = document.getString("notes")
                        ?: document.getString("description")
                                ?: ""

                    // 3. Map Type-Specific Fields
                    airline = document.getString("airline") ?: ""
                    flightNumber = document.getString("flight_number") ?: ""
                    cuisine = document.getString("cuisine") ?: ""
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditPlanViewModel", "Fetch Error: ${e.message}")
            }
    }

    fun updateEvent(eventId: String?, tripId: String?, onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || eventId == null || tripId == null) return

        // Base updates for all event types
        val updates = mutableMapOf<String, Any>(
            "title" to title,
            "activity_name" to title, // Fallback for UI
            "date" to date,
            "startTime" to time,
            "endTime" to endTime,
            "location" to location,
            "notes" to notes
        )

        // Inject specific fields based on the type so the Itinerary Cards update properly
        when (type.lowercase()) {
            "flight" -> {
                updates["destination_airport"] = location
                updates["airline"] = airline
                updates["flight_number"] = flightNumber
            }
            "hotel" -> {
                updates["hotel_name"] = title
            }
            "restaurant" -> {
                updates["restaurant_name"] = title
                updates["cuisine"] = cuisine
            }
        }

        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .update(updates)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("EditPlanViewModel", "Update Failed: ${e.message}")
            }
    }

    fun deleteEvent(eventId: String?, tripId: String?, onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null || eventId == null || tripId == null) return

        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .delete()
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("EditPlanViewModel", "Error deleting event: ${e.message}")
            }
    }
}