package com.example.travelcents.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.travelcents.data.AuthModel

class AuthViewModel() : ViewModel() {

    private val authModel = AuthModel()

    // State to track if we are talking to Firebase
    var isLoading = mutableStateOf(false)
        private set

    // State to store error messages to show in the UI
    var errorMessage = mutableStateOf<String?>(null)
        private set

    // State to track if the account was created successfully
    var isAccountCreated = mutableStateOf(false)
        private set

    // State to track log in success
    var isLoggedIn = mutableStateOf(false)
        private set

    // Sign up a new user with email and password
    fun signUp(firstName: String, lastName: String, email: String, password: String) {
        isLoading.value = true
        errorMessage.value = null

        authModel.createAccountWithEmailAndPassword(firstName, lastName, email, password) { success, message ->
            isLoading.value = false
            if (success) {
                isAccountCreated.value = true
                errorMessage.value = message
            } else {
                errorMessage.value = message ?: "An unknown error occurred"
            }
        }
    }

    // Log in user
    fun logIn(email: String, password: String) {
        isLoading.value = true
        errorMessage.value = null

        authModel.signInWithEmailAndPassword(email, password) { success, message ->
            isLoading.value = false
            if (success) {
                isLoggedIn.value = true
            } else {
                errorMessage.value = message ?: "Login failed"
            }
        }
    }

    // Log Out
    fun signOut() {
        authModel.signOut()
        isLoggedIn.value = false
    }

    // A reset function for the Sign-Up flow
    fun resetSignUpState() {
        isAccountCreated.value = false
        errorMessage.value = null
    }

    fun cleanError() {
        errorMessage.value = null
    }

}