package com.example.travelcents.data.social.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SocialUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeUserPresence(uid: String, onUpdate: (Boolean) -> Unit): ListenerRegistration {
        return db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                onUpdate(snapshot?.isOnlineNow() ?: false)
            }
    }
    fun fetchUserDisplayName(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                onResult("$first $last".trim().ifBlank { "Me" })
            }
    }

    fun fetchUserFullProfile(uid: String, onResult: (String, String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                val name = "$first $last".trim().ifBlank { "Unknown" }
                val photo = doc.getString("profileImageUrl") ?: ""
                onResult(name, photo)
            }
    }
}
