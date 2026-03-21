package com.example.travelcents.ui.main.chats.voting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.travelcents.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.Locale
import kotlin.concurrent.thread

// Foursquare API model
data class PlaceSuggestion(
    val fsqId: String = "",
    val name: String = "",
    val address: String = "",
    val category: String = "",
    val photoUrl: String = ""
)

data class PlaceCategory(
    val name: String,
    val emoji: String,
    val fsqCategoryId: String
)

val PLACE_CATEGORIES = listOf(
    PlaceCategory("Dining",    "🍽️", "13000"), // Dining and Drinking
    PlaceCategory("Nightlife", "🎵", "10032"), // Nightlife Spots
    PlaceCategory("Outdoors",  "🌿", "16000"), // Landmarks and Outdoors
    PlaceCategory("Arts",      "🎨", "10000"), // Arts and Entertainment
    PlaceCategory("Shopping",  "🛍️", "17000")  // Retail
)

// ViewModel
class CreateEventViewModel(private val groupId: String) : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    // Foursquare API key
    private val foursquareApiKey = BuildConfig.FOURSQUARE_API_KEY

    // Current user
    val currentUid: String get() = auth.currentUser?.uid ?: ""
    private val _title          = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description    = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _location       = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _time           = MutableStateFlow("")
    val time: StateFlow<String> = _time.asStateFlow()

    private val _photoUrl       = MutableStateFlow("")

    private val _isCreating     = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val placeSuggestions: StateFlow<List<PlaceSuggestion>> = _placeSuggestions.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces: StateFlow<Boolean> = _isLoadingPlaces.asStateFlow()

    private val _categories = MutableStateFlow(PLACE_CATEGORIES)
    val categories: StateFlow<List<PlaceCategory>> = _categories.asStateFlow()

    private val _creatorName = MutableStateFlow("")

    init { fetchCreatorName() }

    private fun fetchCreatorName() {
        if (currentUid.isEmpty()) return
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last  = doc.getString("lastName")  ?: ""
                _creatorName.value = "$first $last".trim().ifBlank { "Unknown" }
            }
    }

    fun onTitleChange(value: String)       { _title.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onLocationChange(value: String)    { _location.value = value }
    fun onTimeChange(value: String)        { _time.value = value }

    // Pre-fill form from a place suggestion
    fun selectPlace(place: PlaceSuggestion) {
        _title.value    = place.name
        _location.value = place.address
        _photoUrl.value = place.photoUrl
    }

    // Mock data to simulate API call
    fun loadPlaces(destination: String) {
        if (destination.isBlank()) return
        _isLoadingPlaces.value = true

        thread {
            Thread.sleep(1000)
            val mockPlaces = mutableListOf<PlaceSuggestion>()
            val city = destination.trim()
            // Dining Mock
            mockPlaces.add(PlaceSuggestion("1", "$city Bistro", "123 Main St, $city", "Dining", "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400"))
            mockPlaces.add(PlaceSuggestion("2", "The $city Grill", "456 Oak Ave, $city", "Dining", "https://images.unsplash.com/photo-1552566626-52f8b828add9?w=400"))

            // Nightlife Mock
            mockPlaces.add(PlaceSuggestion("3", "Neon Lounge", "789 Pine St, $city", "Nightlife", "https://images.unsplash.com/photo-1514525253361-bee8d48700ef?w=400"))

            // Outdoors Mock
            mockPlaces.add(PlaceSuggestion("4", "$city Central Park", "Park Rd, $city", "Outdoors", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=400"))

            // Arts Mock
            mockPlaces.add(PlaceSuggestion("5", "Modern Art Gallery", "Gallery Way, $city", "Arts", "https://images.unsplash.com/photo-1499781350541-7783f6c6a0c8?w=400"))

            // 3. Update the UI on the Main Thread exactly like the real API would
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                _placeSuggestions.value = mockPlaces
                _isLoadingPlaces.value = false
                android.util.Log.d("MOCK_DATA", "Successfully loaded ${mockPlaces.size} mock places for $city")
            }
        }
    }

    fun createEvent(onSuccess: () -> Unit, onFailure: () -> Unit = {}) {
        if (_title.value.isBlank() || currentUid.isEmpty()) return
        _isCreating.value = true

        val data = hashMapOf(
            "title"         to _title.value.trim(),
            "description"   to _description.value.trim(),
            "location"      to _location.value.trim(),
            "time"          to _time.value.trim(),
            "photoUrl"      to _photoUrl.value,
            "createdBy"     to currentUid,
            "createdByName" to _creatorName.value,
            "createdAt"     to FieldValue.serverTimestamp(),
            "upvotes"       to emptyList<String>(),
            "downvotes"     to emptyList<String>(),
            "commentCount"  to 0
        )

        db.collection("groups").document(groupId)
            .collection("events").add(data)
            .addOnSuccessListener { _isCreating.value = false; onSuccess() }
            .addOnFailureListener { _isCreating.value = false; onFailure() }
    }

    class Factory(private val groupId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreateEventViewModel(groupId) as T
    }
}