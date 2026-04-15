package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.social.model.Friend
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AddFriendViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Friend>>(emptyList())
    val searchResults: StateFlow<List<Friend>> = _searchResults.asStateFlow()

    private val _isSearching   = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _sentRequestUids = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestUids: StateFlow<Set<String>> = _sentRequestUids.asStateFlow()

    private val _friendUids = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .collect { query ->
                    if (query.isNotBlank()) searchUsers(query.trim())
                    else _searchResults.value = emptyList()
                }
        }
        loadSentRequests()
        loadExistingFriends()
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    private fun searchUsers(query: String) {
        if (currentUid.isEmpty()) return
        _isSearching.value = true

        val results = mutableMapOf<String, Friend>()

        // Split query into parts, "user two" → ["user", "two"]
        val parts = query.split(" ").filter { it.isNotBlank() }
        val firstPart  = parts.getOrNull(0) ?: query
        val secondPart = parts.getOrNull(1)

        fun addDoc(doc: com.google.firebase.firestore.DocumentSnapshot) {
            val first = doc.getString("firstName") ?: ""
            val last  = doc.getString("lastName")  ?: ""
            val fullName = "$first $last".trim()
            // If multi-word query, filter client-side to ensure full name matches
            if (query.contains(" ")) {
                if (!fullName.contains(query, ignoreCase = true)) return
            }
            results[doc.id] = Friend(
                uid         = doc.id,
                displayName = fullName.ifBlank { "Unknown" },
                email       = doc.getString("email") ?: ""
            )
        }

        // Build list of queries to run
        // For "user two": search firstName="User" AND lastName prefix="Two"
        // We fetch all users matching firstName prefix, then filter by lastName client-side
        val queries = mutableListOf<com.google.firebase.firestore.Query>()

        // firstName prefix variants
        listOf(firstPart, firstPart.replaceFirstChar { it.uppercaseChar() }).forEach { term ->
            queries.add(
                db.collection("users")
                    .whereGreaterThanOrEqualTo("firstName", term)
                    .whereLessThanOrEqualTo("firstName", term + "\uf8ff")
            )
        }

        // lastName prefix variants (catches "Two" when searching "two")
        listOf(firstPart, firstPart.replaceFirstChar { it.uppercaseChar() }).forEach { term ->
            queries.add(
                db.collection("users")
                    .whereGreaterThanOrEqualTo("lastName", term)
                    .whereLessThanOrEqualTo("lastName", term + "\uf8ff")
            )
        }

        // Email exact match
        queries.add(db.collection("users").whereEqualTo("email", query.lowercase()))

        var pending = queries.size

        fun finish() {
            pending--
            if (pending <= 0) {
                _searchResults.value = results.values
                    .filter { it.uid != currentUid }
                    .filter { it.uid !in _friendUids.value }
                    .toList()
                _isSearching.value = false
            }
        }

        queries.forEach { q ->
            q.get()
                .addOnSuccessListener { snap -> snap.documents.forEach { addDoc(it) }; finish() }
                .addOnFailureListener { finish() }
        }
    }

    private fun loadExistingFriends() {
        if (currentUid.isEmpty()) return
        db.collection("users").document(currentUid).collection("friends")
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snap, _ ->
                _friendUids.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            }
    }

    private fun loadSentRequests() {
        if (currentUid.isEmpty()) return
        db.collection("users").document(currentUid).collection("friends")
            .whereEqualTo("direction", "sent")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                _sentRequestUids.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            }
    }

    fun sendFriendRequest(theirUid: String) {
        if (currentUid.isEmpty()) return
        val batch = db.batch()
        batch.set(
            db.collection("users").document(currentUid).collection("friends").document(theirUid),
            mapOf("status" to "pending", "direction" to "sent")
        )
        batch.set(
            db.collection("users").document(theirUid).collection("friends").document(currentUid),
            mapOf("status" to "pending", "direction" to "received")
        )
        batch.commit().addOnSuccessListener {
            _sentRequestUids.value += theirUid
        }
    }

    fun cancelFriendRequest(theirUid: String) {
        if (currentUid.isEmpty()) return
        val batch = db.batch()
        batch.delete(db.collection("users").document(currentUid).collection("friends").document(theirUid))
        batch.delete(db.collection("users").document(theirUid).collection("friends").document(currentUid))
        batch.commit().addOnSuccessListener {
            _sentRequestUids.value -= theirUid
        }
    }
}
