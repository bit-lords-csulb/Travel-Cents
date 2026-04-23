package com.example.travelcents.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun createAccountWithEmailAndPassword(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                    saveUserToFirestore(user?.uid, firstName, lastName, username, email) { success, error ->
                        if (success) {
                            auth.signOut()
                            continuation.resume(Result.success("Account created! Please log in."))
                        } else {
                            continuation.resume(Result.failure(Exception(error)))
                        }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    continuation.resume(
                        Result.failure(
                            task.exception ?: Exception("Unknown error")
                        )
                    )
                }
            }
    }

    private fun saveUserToFirestore(
        uid: String?,
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        profileImageUrl: String = "",
        profileImageSource: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        if (uid == null) {
            onResult(false, "Failed to retrieve User ID")
            return
        }
        val userMap = hashMapOf(
            "uid" to uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "username" to username.trim().lowercase(),
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (profileImageUrl.isNotBlank()) {
            userMap["profileImageUrl"] = profileImageUrl
            userMap["profileImageSource"] = profileImageSource
            userMap["profileImageUpdatedAt"] = FieldValue.serverTimestamp()
        }
        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener {
                db.collection("usernames").document(username.trim().lowercase())
                    .set(mapOf("email" to email))
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { e -> onResult(false, e.message) }
            }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    suspend fun signInWithEmailOrUsername(
        emailOrUsername: String,
        password: String
    ): Result<String> {
        val email = if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrUsername).matches()) {
            emailOrUsername
        } else {
            lookupEmailByUsername(emailOrUsername).getOrElse { return Result.failure(it) }
        }
        return signInWithEmailAndPassword(email, password)
    }

    private suspend fun lookupEmailByUsername(username: String): Result<String> {
        val normalized = username.trim().lowercase()

        return suspendCancellableCoroutine { continuation ->
            db.collection("usernames").document(normalized)
                .get()
                .addOnSuccessListener { doc ->
                    val email = doc.getString("email")
                    if (email != null) {
                        continuation.resume(Result.success(email))
                    } else {
                        continuation.resume(
                            Result.failure(Exception("No account found with that username."))
                        )
                    }
                }
                .addOnFailureListener { e -> continuation.resume(Result.failure(e)) }
        }
    }

    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    backfillUsernameIndexForCurrentUser {
                        continuation.resume(Result.success("Login Successful"))
                    }
                } else {
                    continuation.resume(Result.failure(task.exception ?: Exception("Login failed")))
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun signInWithGoogle(idToken: String): Result<String> = suspendCancellableCoroutine { continuation ->
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser && user != null) {
                        val fullName = user.displayName ?: ""
                        val nameParts = fullName.split(" ", limit = 2)
                        val firstName = nameParts.getOrNull(0) ?: "Google"
                        val lastName = nameParts.getOrNull(1) ?: "User"
                        val profileImageUrl = user.photoUrl?.toString().orEmpty()

                        val email = user.email ?: ""
                        val generatedUsername = email.substringBefore("@")

                        saveUserToFirestore(
                            uid = user.uid,
                            firstName = firstName,
                            lastName = lastName,
                            username = generatedUsername,
                            email = email,
                            profileImageUrl = profileImageUrl,
                            profileImageSource = if (profileImageUrl.isNotBlank()) "google" else ""
                        ) { success, error ->
                            if (success) {
                                continuation.resume(Result.success("Google account synced to Firestore!"))
                            } else {
                                Log.e(TAG, "Google sign-in user bootstrap failed: $error")
                                continuation.resume(Result.failure(Exception(error ?: "Firestore sync failed")))
                            }
                        }
                    } else if (user != null) {
                        syncExistingGoogleProfile(user.uid, user.email.orEmpty(), user.photoUrl?.toString().orEmpty()) {
                            backfillUsernameIndexForCurrentUser {
                                continuation.resume(Result.success("Login Successful"))
                            }
                        }
                    } else {
                        continuation.resume(Result.success("Login Successful"))
                    }
                } else {
                    Log.e(TAG, "signInWithGoogle:failure", task.exception)
                    continuation.resume(Result.failure(task.exception ?: Exception("Google Login failed")))
                }
            }
    }

    private fun syncExistingGoogleProfile(
        uid: String,
        email: String,
        profileImageUrl: String,
        onComplete: () -> Unit
    ) {
        val updates = mutableMapOf<String, Any>("uid" to uid)
        if (email.isNotBlank()) {
            updates["email"] = email
        }
        if (profileImageUrl.isNotBlank()) {
            updates["profileImageUrl"] = profileImageUrl
            updates["profileImageSource"] = "google"
            updates["profileImageUpdatedAt"] = FieldValue.serverTimestamp()
        }

        db.collection("users").document(uid)
            .set(updates, SetOptions.merge())
            .addOnCompleteListener { onComplete() }
    }

    private fun backfillUsernameIndexForCurrentUser(onComplete: () -> Unit) {
        val user = auth.currentUser ?: run {
            onComplete()
            return
        }

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username")?.trim()?.lowercase().orEmpty()
                val email = doc.getString("email")?.trim().orEmpty()
                if (username.isBlank() || email.isBlank()) {
                    onComplete()
                    return@addOnSuccessListener
                }

                db.collection("usernames").document(username)
                    .set(mapOf("email" to email), SetOptions.merge())
                    .addOnCompleteListener { onComplete() }
            }
            .addOnFailureListener { onComplete() }
    }
}
