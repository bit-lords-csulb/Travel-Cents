package com.example.travelcents.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthModel {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun createAccount(email: String, password: String, onResult: (FirebaseUser?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(auth.currentUser)
                } else {
                    onResult(null)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }
}