package com.example.travelcents.ui.main.chats.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
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

    // UIDs of users we've already sent a request to
    private val _sentRequestUids = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestUids: StateFlow<Set<String>> = _sentRequestUids.asStateFlow()

    init {
        // Debounce search so we don't query on every keystroke
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .collect { query -> if (query.isNotBlank()) searchUsers(query) else _searchResults.value = emptyList() }
        }
        loadSentRequests()
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    private fun searchUsers(query: String) {
        if (currentUid.isEmpty()) return
        _isSearching.value = true

        val trimmed = query.trim().lowercase()

        // Search by email exact match first, then by displayName prefix
        db.collection("users")
            .whereEqualTo("email", trimmed)
            .get()
            .addOnSuccessListener { emailSnap ->
                val byEmail = emailSnap.documents
                    .filter { it.id != currentUid }
                    .mapNotNull { doc ->
                        val first = doc.getString("firstName") ?: ""
                        val last  = doc.getString("lastName")  ?: ""
                        Friend(
                            uid = doc.id,
                            displayName = "$first $last".trim().ifBlank { "Unknown" },
                            email = doc.getString("email") ?: ""
                        )
                    }

                // Also search by firstName prefix
                val endStr = trimmed.replaceRange(trimmed.length - 1, trimmed.length,
                    (trimmed.last() + 1).toString())

                db.collection("users")
                    .whereGreaterThanOrEqualTo("firstName", trimmed)
                    .whereLessThan("firstName", endStr)
                    .get()
                    .addOnSuccessListener { nameSnap ->
                        val byName = nameSnap.documents
                            .filter { it.id != currentUid }
                            .mapNotNull { doc ->
                                val first = doc.getString("firstName") ?: ""
                                val last  = doc.getString("lastName")  ?: ""
                                Friend(
                                    uid = doc.id,
                                    displayName = "$first $last".trim().ifBlank { "Unknown" },
                                    email = doc.getString("email") ?: ""
                                )
                            }

                        // Merge, deduplicate, exclude existing friends
                        val merged = (byEmail + byName)
                            .distinctBy { it.uid }
                            .filter { it.uid !in _sentRequestUids.value }

                        _searchResults.value = merged
                        _isSearching.value   = false
                    }
                    .addOnFailureListener { _isSearching.value = false }
            }
            .addOnFailureListener { _isSearching.value = false }
    }

    // Load UIDs we've already sent requests to so UI reflects pending state
    private fun loadSentRequests() {
        if (currentUid.isEmpty()) return
        db.collection("users")
            .document(currentUid)
            .collection("friends")
            .whereEqualTo("direction", "sent")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                _sentRequestUids.value = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
            }
    }

    fun sendFriendRequest(theirUid: String) {
        if (currentUid.isEmpty()) return

        val db = Firebase.firestore
        val batch = db.batch()

        // My side: sent + pending
        val myRef = db.collection("users").document(currentUid)
            .collection("friends").document(theirUid)
        batch.set(myRef, mapOf("status" to "pending", "direction" to "sent"))

        // Their side: received + pending
        val theirRef = db.collection("users").document(theirUid)
            .collection("friends").document(currentUid)
        batch.set(theirRef, mapOf("status" to "pending", "direction" to "received"))

        batch.commit().addOnSuccessListener {
            _sentRequestUids.value = _sentRequestUids.value + theirUid
        }
    }

    fun cancelFriendRequest(theirUid: String) {
        if (currentUid.isEmpty()) return

        val db = Firebase.firestore
        val batch = db.batch()

        batch.delete(db.collection("users").document(currentUid).collection("friends").document(theirUid))
        batch.delete(db.collection("users").document(theirUid).collection("friends").document(currentUid))

        batch.commit().addOnSuccessListener {
            _sentRequestUids.value = _sentRequestUids.value - theirUid
        }
    }
}