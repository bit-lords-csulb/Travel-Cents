package com.example.travelcents.ui.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.AuthModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.Result
import kotlin.runCatching

class AuthViewModel : ViewModel() {
    private val authModel = AuthModel()

    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
    }

    // State to track if we are talking to Firebase
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State to track if the account was created successfully
    private val _isAccountCreated = MutableStateFlow(false)
    val isAccountCreated: StateFlow<Boolean> = _isAccountCreated.asStateFlow()

    // State to track log in success
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Error messages (failures only)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success / info messages (non-error feedback)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Sign up a new user with email and password
    fun signUp(firstName: String, lastName: String, email: String, password: String) {
        if (!validateSignUpInputs(firstName, lastName, email, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _statusMessage.value = null

            val result = authModel.createAccountWithEmailAndPassword(firstName, lastName, email, password)

            _isLoading.value = false
            result.fold(
                onSuccess = { message ->
                    _isAccountCreated.value = true
                    _statusMessage.value = message
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "An unknown error occurred"
                }
            )
        }
    }

    // Log in user
    fun logIn(email: String, password: String) {
        if (!validateLogInInputs(email, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authModel.signInWithEmailAndPassword(email, password)

            _isLoading.value = false
            result.fold(
                onSuccess = { _isLoggedIn.value = true },
                onFailure = { error ->
                    _errorMessage.value = when {
                        error.message?.contains("credential is incorrect", ignoreCase = true) == true ||
                                error.message?.contains("malformed", ignoreCase = true) == true ||
                                error.message?.contains("expired", ignoreCase = true) == true
                            -> "Incorrect email or password."
                        error.message?.contains("network", ignoreCase = true) == true
                            -> "Network error. Please check your connection."
                        error.message?.contains("too-many-requests", ignoreCase = true) == true
                            -> "Too many attempts. Please try again later."
                        else -> "Login failed. Please try again."
                    }
                }
            )
        }
    }

    // Log out and reset all state
    fun signOut() {
        authModel.signOut()
        _isLoggedIn.value = false
        _isAccountCreated.value = false
        _errorMessage.value = null
        _statusMessage.value = null
    }

    // Reset sign-up flow state (e.g. after navigating away)
    fun resetSignUpState() {
        _isAccountCreated.value = false
        _errorMessage.value = null
        _statusMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Validation

    private fun validateSignUpInputs(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Boolean {
        if (firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = "Name fields cannot be empty"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Invalid email address"
            return false
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _errorMessage.value = "Password must be at least $MIN_PASSWORD_LENGTH characters"
            return false
        }
        return true
    }

    private fun validateLogInInputs(email: String, password: String): Boolean {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Invalid email address"
            return false
        }
        if (password.isBlank()) {
            _errorMessage.value = "Password cannot be empty"
            return false
        }
        return true
    }
}