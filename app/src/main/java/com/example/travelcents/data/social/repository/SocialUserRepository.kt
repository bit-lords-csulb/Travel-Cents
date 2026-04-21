package com.example.travelcents.data.social.repository

import com.google.firebase.firestore.FirebaseFirestore

class SocialUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun fetchUserDisplayName(uid: String, onResult: (String) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                onResult("$first $last".trim().ifBlank { "Me" })
            }
    }
}
