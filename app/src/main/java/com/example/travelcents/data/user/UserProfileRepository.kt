package com.example.travelcents.data.user

import com.example.travelcents.data.user.model.CurrentUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeCurrentUserProfile(): Flow<CurrentUserProfile> = callbackFlow {
        val authUser = auth.currentUser
        if (authUser == null) {
            trySend(CurrentUserProfile(firstName = "Guest", isLoading = false))
            close()
            return@callbackFlow
        }

        trySend(profileFrom(authUser = authUser, snapshot = null, isLoading = true))

        val registration = db.collection(USERS_COLLECTION)
            .document(authUser.uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(
                    profileFrom(
                        authUser = authUser,
                        snapshot = snapshot,
                        isLoading = false
                    )
                )
            }

        awaitClose { registration.remove() }
    }

    suspend fun syncCurrentUserGoogleProfile() {
        val authUser = auth.currentUser ?: return
        val signedInWithGoogle = authUser.providerData.any { provider ->
            provider.providerId == GoogleAuthProvider.PROVIDER_ID
        }
        if (!signedInWithGoogle) return

        val document = db.collection(USERS_COLLECTION).document(authUser.uid).get().await()
        val photoUrl = authUser.googleProfilePhotoUrl()
        val (firstName, lastName) = authUser.displayNameParts()

        val updates = mutableMapOf<String, Any>()

        if (!document.exists() || document.getString("uid") != authUser.uid) {
            updates["uid"] = authUser.uid
        }

        authUser.email
            ?.takeIf { it.isNotBlank() && document.getString("email").isNullOrBlank() }
            ?.let { updates["email"] = it }

        if (document.getString("firstName").isNullOrBlank() && firstName.isNotBlank()) {
            updates["firstName"] = firstName
        }
        if (document.getString("lastName").isNullOrBlank() && lastName.isNotBlank()) {
            updates["lastName"] = lastName
        }
        if (!photoUrl.isNullOrBlank() && document.getString("profileImageUrl") != photoUrl) {
            updates["profileImageUrl"] = photoUrl
            updates["profileImageSource"] = GOOGLE_SOURCE
            updates["profileImageUpdatedAt"] = FieldValue.serverTimestamp()
        }

        if (updates.isNotEmpty()) {
            db.collection(USERS_COLLECTION)
                .document(authUser.uid)
                .set(updates, SetOptions.merge())
                .await()
        }
    }

    private fun profileFrom(
        authUser: FirebaseUser,
        snapshot: DocumentSnapshot?,
        isLoading: Boolean
    ): CurrentUserProfile {
        val (fallbackFirstName, fallbackLastName) = authUser.displayNameParts()
        return CurrentUserProfile(
            uid = authUser.uid,
            firstName = snapshot?.getString("firstName").orEmpty().ifBlank { fallbackFirstName },
            lastName = snapshot?.getString("lastName").orEmpty().ifBlank { fallbackLastName },
            username = snapshot?.getString("username").orEmpty(),
            email = snapshot?.getString("email").orEmpty().ifBlank { authUser.email.orEmpty() },
            profileImageUrl = snapshot?.getString("profileImageUrl")
                .orEmpty()
                .ifBlank { authUser.googleProfilePhotoUrl().orEmpty() },
            profileImageSource = snapshot?.getString("profileImageSource").orEmpty(),
            isLoading = isLoading
        )
    }

    private fun FirebaseUser.displayNameParts(): Pair<String, String> {
        val fullName = displayName.orEmpty().trim()
        if (fullName.isBlank()) return "" to ""

        val parts = fullName.split(" ", limit = 2)
        val firstName = parts.getOrNull(0).orEmpty()
        val lastName = parts.getOrNull(1).orEmpty()
        return firstName to lastName
    }

    private fun FirebaseUser.googleProfilePhotoUrl(): String? {
        val photo = photoUrl?.toString()?.trim().orEmpty()
        if (photo.isBlank()) return null
        return if (photo.contains("googleusercontent.com") && !photo.contains("=s")) {
            "${photo}=s512-c"
        } else {
            photo
        }
    }

    private companion object {
        private const val USERS_COLLECTION = "users"
        private const val GOOGLE_SOURCE = "google"
    }
}


