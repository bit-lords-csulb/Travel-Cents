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
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject

data class PlaceSuggestion(
    val fsqId: String = "",
    val name: String = "",
    val interest: String = "",
    val address: String = "",
    val category: String = "",
    val photoUrl: String = ""
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

    private val _time = MutableStateFlow("")
    val time = _time.asStateFlow()

    private val _photoUrl = MutableStateFlow("")
    val photoUrl = _photoUrl.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())
    val placeSuggestions = _placeSuggestions.asStateFlow()

    private val _isLoadingPlaces = MutableStateFlow(false)
    val isLoadingPlaces = _isLoadingPlaces.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private var creatorName = ""
    init { fetchCreatorName() }

    private fun fetchCreatorName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            creatorName = "${doc.getString("firstName")} ${doc.getString("lastName")}".trim()
        }
    }

    fun onTitleChange(v: String) { _title.value = v }
    fun onDescriptionChange(v: String) { _description.value = v }
    fun onLocationChange(v: String) { _location.value = v }
    fun onTimeChange(v: String) { _time.value = v }

    fun handleLocalImage(uri: Uri) { _photoUrl.value = uri.toString() }

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
                        results.add(PlaceSuggestion(
                            fsqId = b.optString("id"),
                            name = b.optString("name"),
                            interest = "${b.optJSONArray("categories")?.optJSONObject(0)?.optString("title")} • ${b.optDouble("rating")} ⭐",
                            address = loc?.optString("address1", "") ?: "",
                            category = b.optJSONArray("categories")?.optJSONObject(0)?.optString("title") ?: "Activity",
                            photoUrl = b.optString("image_url")
                        ))
                    }
                }
                _placeSuggestions.value = results
            } catch (e: Exception) { Log.e("YELP", "${e.message}") }
            finally { _isLoadingPlaces.value = false }
        }
    }

    fun selectPlace(place: PlaceSuggestion) {
        _title.value = place.name
        _location.value = place.address
        _photoUrl.value = place.photoUrl
        _description.value = "Fetching factual history..."
        viewModelScope.launch {
            try {
                val response = client.get("https://en.wikipedia.org/api/rest_v1/page/summary/${place.name.replace(" ", "_")}")
                if (response.status.value == 200) {
                    _description.value = JSONObject(response.bodyAsText()).optString("extract")
                } else { _description.value = "A popular ${place.category} spot." }
            } catch (_: Exception) { _description.value = "Let's visit ${place.name}!" }
        }
    }

    fun createEvent(onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        _isCreating.value = true
        val data = hashMapOf(
            "title" to _title.value, "description" to _description.value,
            "location" to _location.value, "photoUrl" to _photoUrl.value,
            "createdBy" to uid, "createdByName" to creatorName,
            "createdAt" to FieldValue.serverTimestamp(), "upvotes" to emptyList<String>(),
            "downvotes" to emptyList<String>(), "commentCount" to 0
        )
        db.collection("groups").document(groupId).collection("events").add(data)
            .addOnSuccessListener { _isCreating.value = false; onSuccess() }
    }

    fun clearForm() {
        _title.value = ""
        _description.value = ""
        _location.value = ""
        _photoUrl.value = ""
    }

    fun clearSuggestions() {
        _placeSuggestions.value = emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val groupId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreateEventViewModel(groupId) as T
    }
}