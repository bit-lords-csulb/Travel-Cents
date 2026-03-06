package com.example.travelcents.data

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore

class AuthModel {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db = Firebase.firestore

    // Create a new account with email and password
    fun createAccountWithEmailAndPassword(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Login Success: Save user to Firestore
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    // Save user to Firestore then Sign out user
                    // Make user resign in after creating account
                    saveUserToFirestore(user?.uid, firstName, lastName, email) { success, error ->
                        if (success) {
                            auth.signOut()
                            onResult(true, "Account created!, Please log in.")
                        } else {
                            onResult(false, error)
                        }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }

    // Save the user's information to Firestore DB
    fun saveUserToFirestore(
        uid: String?,
        firstName: String,
        lastName: String,
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
            "email" to email,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(userMap)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    // Log In
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                onResult(true, "Login Successful")
            } else {
                onResult(false, task.exception?.message)
            }
        }

    }

    // Sign out of user account
    fun signOut() {
        auth.signOut()
    }

}



