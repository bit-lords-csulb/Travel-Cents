package com.example.travelcents.ui.main.chats.voting

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelcents.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

data class PlaceSuggestion(
    val yelpId: String = "",
    val name: String = "",
    val interest: String = "",
    val address: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val photoUrl: String = "",
    val yelpUrl: String = "",
    val rating: Double? = null,
    val reviewCount: Int = 0
)

class CreateEventViewModel(private val groupId: String) : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _location = MutableStateFlow("")
    val location = _location.asStateFlow()

    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()

    private val _startTime = MutableStateFlow("")
    val startTime: StateFlow<String> = _startTime.asStateFlow()

    private val _endTime = MutableStateFlow("")
    val endTime: StateFlow<String> = _endTime.asStateFlow()

    private val _photoUrl = MutableStateFlow("")
    val photoUrl = _photoUrl.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val placeSuggestions = _placeSuggestions.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces = _isLoadingPlaces.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private var creatorName = ""
    private var selectedPlace: PlaceSuggestion? = null

    init {
        fetchCreatorName()
    }

    private fun fetchCreatorName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            creatorName = "${doc.getString("firstName")} ${doc.getString("lastName")}".trim()
        }
    }

    fun onTitleChange(v: String) {
        if (selectedPlace?.name != v) {
            selectedPlace = null
        }
        _title.value = v
    }

    fun onDescriptionChange(v: String) {
        _description.value = v
    }

    fun onLocationChange(v: String) {
        _location.value = v
    }

    fun onDateChange(v: String) {
        _date.value = v
    }

    fun onStartTimeChange(time: String) {
        _startTime.value = time
    }

    fun onEndTimeChange(time: String) {
        _endTime.value = time
    }

    fun handleLocalImage(uri: Uri) {
        _photoUrl.value = uri.toString()
    }

    // Uses Yelp API to search for places based on a query and city
    // Returns a list of PlaceSuggestion objects
    fun loadPlaces(query: String, city: String) {
        if (city.isBlank()) return
        _isLoadingPlaces.value = true
        viewModelScope.launch {
            try {
                val response = client.get("https://api.yelp.com/v3/businesses/search") {
                    header("Authorization", "Bearer ${BuildConfig.YELP_API_KEY}")
                    parameter("term", query.ifBlank { "attractions" })
                    parameter("location", city)
                    parameter("limit", "15")
                }
                val json = JSONObject(response.bodyAsText())
                val businesses = json.optJSONArray("businesses")
                val results = mutableListOf<PlaceSuggestion>()
                businesses?.let {
                    for (i in 0 until it.length()) {
                        val b = it.getJSONObject(i)
                        val loc = b.optJSONObject("location")
                        val categories = b.optJSONArray("categories")
                            .toStringListFromObjects("title")
                        val displayAddress = loc?.optJSONArray("display_address")
                            .toStringList()
                            .joinToString(", ")
                            .ifBlank { loc?.optString("address1", "").orEmpty() }
                        val rating = b.optDouble("rating", 0.0).takeIf { value -> value > 0.0 }
                        results.add(
                            PlaceSuggestion(
                                yelpId = b.optString("id"),
                                name = b.optString("name"),
                                interest = buildInterestLabel(
                                    category = categories.firstOrNull().orEmpty(),
                                    rating = rating
                                ),
                                address = displayAddress,
                                category = categories.firstOrNull() ?: "Activity",
                                categories = categories,
                                photoUrl = b.optString("image_url"),
                                yelpUrl = b.optString("url"),
                                rating = rating,
                                reviewCount = b.optInt("review_count", 0)
                            )
                        )
                    }
                }
                _placeSuggestions.value = results
            } catch (e: Exception) {
                Log.e("YELP", "${e.message}")
            } finally {
                _isLoadingPlaces.value = false
            }
        }
    }

    // Called when a user selects a place from the suggestions
    // Updates the form fields with the selected place's data
    // Fetches a more detailed description from Wikipedia
    fun selectPlace(place: PlaceSuggestion) {
        selectedPlace = place
        _title.value = place.name
        _location.value = place.address
        _photoUrl.value = place.photoUrl
        _date.value = ""
        _startTime.value = ""
        _endTime.value = ""

        _description.value = "Fetching details..."

        viewModelScope.launch {
            try {
                val wikiTerm = java.net.URLEncoder.encode(place.name.replace(" ", "_"), "UTF-8")
                val response =
                    client.get("https://en.wikipedia.org/api/rest_v1/page/summary/$wikiTerm")

                if (response.status.value == 200) {
                    val extract = JSONObject(response.bodyAsText()).optString("extract")

                    // Ensure the wiki text isn't empty or a generic "Did you mean?"
                    if (extract.isNotBlank() && !extract.contains("may refer to:")) {
                        _description.value = extract
                    } else {
                        _description.value = buildFallbackDescription(place)
                    }
                } else {
                    _description.value = buildFallbackDescription(place)
                }
            } catch (_: Exception) {
                _description.value = buildFallbackDescription(place)
            }
        }
    }

    // Fallback description if Wikipedia API fails
    // Generates a clean, dynamic sentence using the data we already have from Yelp
    private fun buildFallbackDescription(place: PlaceSuggestion): String {
        // Check if your 'interest' string contains the star emoji to imply it has a rating
        val ratingPrefix = if (place.interest.contains("⭐")) "highly-rated " else ""
        val safeCategory = place.category.ifBlank { "destination" }.lowercase()
        val safeAddress = place.address.ifBlank { "the area" }

        return "${place.name} is a $ratingPrefix$safeCategory located at $safeAddress. Lets check it out!"
    }

    // Creates an event in the database with the current form data
    fun createEvent(onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _isCreating.value = true
        val place = selectedPlace
        val data = hashMapOf<String, Any>(
            "title" to _title.value,
            "description" to _description.value,
            "location" to _location.value,
            "date" to _date.value,
            "startTime" to _startTime.value,
            "endTime" to _endTime.value,
            "photoUrl" to _photoUrl.value,
            "createdBy" to uid,
            "createdByName" to creatorName,
            "createdAt" to FieldValue.serverTimestamp(),
            "upvotes" to emptyList<String>(),
            "downvotes" to emptyList<String>(),
            "commentCount" to 0,
            "isWon" to false
        )
        place?.let { selected ->
            if (selected.yelpId.isNotBlank()) data["yelpId"] = selected.yelpId
            if (selected.yelpUrl.isNotBlank()) data["yelpUrl"] = selected.yelpUrl
            if (selected.category.isNotBlank()) data["yelpCategory"] = selected.category
            if (selected.categories.isNotEmpty()) data["yelpCategories"] = selected.categories
            selected.rating?.let { data["yelpRating"] = it }
            if (selected.reviewCount > 0) data["yelpReviewCount"] = selected.reviewCount
            if (selected.photoUrl.isNotBlank()) data["yelpImageUrl"] = selected.photoUrl
        }
        db.collection("groups").document(groupId).collection("events").add(data)
            .addOnSuccessListener { _isCreating.value = false; onSuccess() }
    }

    fun clearForm() {
        _title.value = ""
        _description.value = ""
        _date.value = ""
        _startTime.value = ""
        _endTime.value = ""
        _location.value = ""
        _photoUrl.value = ""
        selectedPlace = null
    }

    fun clearSuggestions() {
        _placeSuggestions.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val groupId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CreateEventViewModel(groupId) as T
    }
}

private fun buildInterestLabel(category: String, rating: Double?): String {
    val categoryLabel = category.ifBlank { "Activity" }
    return if (rating != null) "$categoryLabel • $rating ⭐" else categoryLabel
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index)
                .takeIf { it.isNotBlank() }
                ?.let(::add)
        }
    }
}

private fun JSONArray?.toStringListFromObjects(key: String): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)
                ?.optString(key)
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
        }
    }
}
