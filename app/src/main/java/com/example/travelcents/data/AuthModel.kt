package com.example.travelcents.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.firebase.auth.GoogleAuthProvider


class AuthModel {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthModel"
    }

    // Create a new account with email and password
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

    // Save the user's information to Firestore DB
    private fun saveUserToFirestore(
        uid: String?,
        firstName: String,
        lastName: String,
        username: String,
        email: String,
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
        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    // Log in with email or username
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

    // Resolve username to email via Firestore
    private suspend fun lookupEmailByUsername(username: String): Result<String> =
        suspendCancellableCoroutine { continuation ->
            db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    val email = snapshot.documents.firstOrNull()?.getString("email")
                    if (email != null) {
                        continuation.resume(Result.success(email))
                    } else {
                        continuation.resume(Result.failure(Exception("No account found with that username.")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }

    // Log In
    suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(Result.success("Login Successful"))
                } else {
                    continuation.resume(Result.failure(task.exception ?: Exception("Login failed")))
                }
            }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Delete account
    suspend fun deleteAccount(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(Result.failure(Exception("No user logged in")))
            return@suspendCancellableCoroutine
        }

        val uid = user.uid
        // Delete from Firestore first
        db.collection("users").document(uid).delete()
            .addOnSuccessListener {
                // Then delete from Auth
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(Unit))
                        } else {
                            continuation.resume(Result.failure(task.exception ?: Exception("Failed to delete user account")))
                        }
                    }
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    // Log in with Google ID Token
    suspend fun signInWithGoogle(idToken: String): Result<String> = suspendCancellableCoroutine { continuation ->
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false

                    if (isNewUser && user != null) {
                        // Extract names from Google Profile
                        val fullName = user.displayName ?: ""
                        val nameParts = fullName.split(" ", limit = 2)
                        val firstName = nameParts.getOrNull(0) ?: "Google"
                        val lastName = nameParts.getOrNull(1) ?: "User"

                        // Generate a temporary username from email
                        val email = user.email ?: ""
                        val generatedUsername = email.substringBefore("@")

                        // Save to Firestore using your existing helper
                        saveUserToFirestore(
                            uid = user.uid,
                            firstName = firstName,
                            lastName = lastName,
                            username = generatedUsername,
                            email = email
                        ) { success, error ->
                            if (success) {
                                continuation.resume(Result.success("Google account synced to Firestore!"))
                            } else {
                                continuation.resume(Result.failure(Exception(error ?: "Firestore sync failed")))
                            }
                        }
                    } else {
                        continuation.resume(Result.success("Login Successful"))
                    }
                } else {
                    continuation.resume(Result.failure(task.exception ?: Exception("Google Login failed")))
                }
            }
    }
}
