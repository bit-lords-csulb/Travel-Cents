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

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
        .build()

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

    // Load place suggestions from Foursquare for all categories
    fun loadPlaces(destination: String) {
        if (destination.isBlank() || foursquareApiKey.isBlank()) return
        _isLoadingPlaces.value = true

        val cleanApiKey = foursquareApiKey.replace("\"", "").trim()
        val encodedCity = java.net.URLEncoder.encode(destination.trim(), "UTF-8")

        thread {
            val allPlaces = mutableListOf<PlaceSuggestion>()
            try {
                PLACE_CATEGORIES.forEach { category ->
                    val urlStr = "https://places-api.foursquare.com/places/search" +
                            "?near=$encodedCity" +
                            "&fsq_category_ids=${category.fsqCategoryId}" +
                            "&limit=4" +
                            "&fields=fsq_place_id,name,location,categories,photos"

                    val request = okhttp3.Request.Builder()
                        .url(urlStr)
                        .get()
                        .addHeader("Authorization", cleanApiKey)
                        .addHeader("Accept", "application/json")
                        .addHeader("X-Places-Api-Version", "20250617") // Ensure no dashes
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: "{}"
                            android.util.Log.d("FSQ_DEBUG", "Category ${category.name} raw response: $body")
                            val results = JSONObject(body).optJSONArray("results") ?: org.json.JSONArray()

                            for (i in 0 until results.length()) {
                                val p = results.getJSONObject(i)
                                val loc = p.optJSONObject("location")
                                val addr = loc?.optString("formatted_address") ?: ""

                                var photoUrl = ""
                                val photos = p.optJSONArray("photos")
                                if (photos != null && photos.length() > 0) {
                                    val first = photos.getJSONObject(0)
                                    photoUrl = "${first.optString("prefix")}400x300${first.optString("suffix")}"
                                }

                                allPlaces.add(PlaceSuggestion(
                                    fsqId = p.optString("fsq_place_id"),
                                    name = p.optString("name"),
                                    address = addr,
                                    category = category.name,
                                    photoUrl = photoUrl
                                ))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Foursquare", "Sequential Crash: ${e.message}")
            } finally {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    _placeSuggestions.value = allPlaces.toList()
                    _isLoadingPlaces.value = false
                    android.util.Log.d("Foursquare_Success", "UI Updated with ${allPlaces.size} places")
                }
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