package com.example.travelcents.ui.main.itinerary

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

        // 1. CLEAR FIELDS FOR NEW EVENTS
        if (eventId == "new") {
            type = "activity" // Default to activity for new events
            title = ""
            date = ""
            time = ""
            endTime = ""
            location = ""
            notes = ""
            airline = ""
            flightNumber = ""
            cuisine = ""
            return
        }

        // 2. FETCH EXISTING EVENTS
        db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events").document(eventId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val allData = document.data ?: emptyMap()
                    val detailsMap = allData["details"] as? Map<*, *>

                    type = document.getString("type") ?: ""

                    title = when (type.lowercase()) {
                        "hotel" -> document.getString("hotel_name")
                            ?: detailsMap?.get("hotel_name")?.toString()
                            ?: document.getString("title")
                            ?: detailsMap?.get("title")?.toString()
                            ?: type.replaceFirstChar { it.uppercase() }
                        "restaurant" -> document.getString("restaurant_name")
                            ?: detailsMap?.get("restaurant_name")?.toString()
                            ?: document.getString("title")
                            ?: detailsMap?.get("title")?.toString()
                            ?: type.replaceFirstChar { it.uppercase() }
                        else -> document.getString("title")
                            ?: document.getString("activity_name")
                            ?: detailsMap?.get("title")?.toString()
                            ?: type.replaceFirstChar { it.uppercase() }
                    }

                    date = document.getString("date") ?: ""
                    time = document.getString("startTime") ?: ""
                    endTime = document.getString("endTime") ?: ""

                    location = document.getString("location")
                        ?: document.getString("destination_airport")
                                ?: ""

                    notes = document.getString("notes")
                        ?: document.getString("description")
                                ?: ""

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

        // Base data for all event types
        val eventData = mutableMapOf<String, Any>(
            "type" to type,
            "title" to title,
            "activity_name" to title, // Fallback for UI
            "date" to date,
            "startTime" to time,
            "endTime" to endTime,
            "location" to location,
            "notes" to notes,
            "itineraryId" to tripId
        )

        // Inject specific fields based on the type
        when (type.lowercase()) {
            "flight" -> {
                eventData["destination_airport"] = location
                eventData["airline"] = airline
                eventData["flight_number"] = flightNumber
            }
            "hotel" -> {
                eventData["hotel_name"] = title
            }
            "restaurant" -> {
                eventData["restaurant_name"] = title
                eventData["cuisine"] = cuisine
            }
        }

        val eventsCollection = db.collection("users").document(uid)
            .collection("trips").document(tripId)
            .collection("events")

        // 3. CREATE NEW VS. UPDATE EXISTING
        if (eventId == "new") {
            // Create a brand new document reference to get a unique ID
            val newDocRef = eventsCollection.document()
            eventData["eventId"] = newDocRef.id // Save the ID inside the document itself

            newDocRef.set(eventData)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener { e -> Log.e("EditPlanViewModel", "Create Failed: ${e.message}") }
        } else {
            // Update the existing document
            eventsCollection.document(eventId).update(eventData)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener { e -> Log.e("EditPlanViewModel", "Update Failed: ${e.message}") }
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
