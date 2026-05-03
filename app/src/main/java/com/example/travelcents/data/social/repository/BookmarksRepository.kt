package com.example.travelcents.data.social.repository

import android.util.Log
import com.example.travelcents.data.social.model.BookmarkedPlace
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookmarksRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeBookmarks(uid: String): Flow<List<BookmarkedPlace>> = callbackFlow {
        val reg = db.collection("users").document(uid)
            .collection("bookmarks")
            .orderBy("savedAtEpochMs", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(
                        "BookmarksRepository",
                        "bookmarks snapshot error for uid=$uid: ${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                val places = snap?.documents?.mapNotNull { it.toBookmarkedPlace() } ?: emptyList()
                Log.d("BookmarksRepository", "snapshot for uid=$uid count=${places.size}")
                trySend(places)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addBookmark(uid: String, place: BookmarkedPlace) {
        db.collection("users").document(uid)
            .collection("bookmarks").document(place.id)
            .set(place.toFirestoreMap())
            .await()
    }

    suspend fun removeBookmark(uid: String, placeId: String) {
        db.collection("users").document(uid)
            .collection("bookmarks").document(placeId)
            .delete()
            .await()
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toBookmarkedPlace(): BookmarkedPlace? {
    val id = id
    val name = getString("name") ?: return null
    return BookmarkedPlace(
        id = id,
        name = name,
        category = getString("category") ?: "",
        area = getString("area") ?: "",
        imageUrl = getString("imageUrl"),
        yelpUrl = getString("yelpUrl"),
        savedAtEpochMs = getLong("savedAtEpochMs") ?: 0L
    )
}

private fun BookmarkedPlace.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "category" to category,
    "area" to area,
    "imageUrl" to imageUrl,
    "yelpUrl" to yelpUrl,
    "savedAtEpochMs" to savedAtEpochMs
)
