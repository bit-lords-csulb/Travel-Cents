package com.example.travelcents.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.auth.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

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
    fun signUp(firstName: String, lastName: String, username: String, email: String, password: String) {
        if (!validateSignUpInputs(firstName, lastName, username, email, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _statusMessage.value = null

            val result = authRepository.createAccountWithEmailAndPassword(firstName, lastName, username, email, password)

            _isLoading.value = false
            result.fold(
                onSuccess = { message ->
                    _isAccountCreated.value = true
                    _statusMessage.value = message
                },
                onFailure = { error ->
                    Log.e("AuthViewModel", "Sign up failed", error)
                    _errorMessage.value = mapSignUpError(error)
                }
            )
        }
    }

    // Log in user with email or username
    fun logIn(emailOrUsername: String, password: String) {
        if (!validateLogInInputs(emailOrUsername, password)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authRepository.signInWithEmailOrUsername(emailOrUsername, password)

            _isLoading.value = false
            result.fold(
                onSuccess = { _isLoggedIn.value = true },
                onFailure = { error ->
                    Log.e("AuthViewModel", "Login failed", error)
                    _errorMessage.value = mapLoginError(error)
                }
            )
        }
    }

    // Log out and reset all state
    fun signOut() {
        authRepository.signOut()
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

    fun clearMessages() {
        _errorMessage.value = null
        _statusMessage.value = null
    }

    fun setStatusMessage(message: String?) {
        _statusMessage.value = message
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    // Validation

    private fun validateSignUpInputs(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String
    ): Boolean {
        if (firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = "Name fields cannot be empty"
            return false
        }
        if (username.isBlank()) {
            _errorMessage.value = "Username cannot be empty"
            return false
        }
        if (username.contains(" ")) {
            _errorMessage.value = "Username cannot contain spaces"
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

    private fun validateLogInInputs(emailOrUsername: String, password: String): Boolean {
        if (emailOrUsername.isBlank()) {
            _errorMessage.value = "Email or username cannot be empty"
            return false
        }
        if (password.isBlank()) {
            _errorMessage.value = "Password cannot be empty"
            return false
        }
        return true
    }
    // Log in user with Google ID Token
    fun logInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _statusMessage.value = null

            val result = authRepository.signInWithGoogle(idToken)

            _isLoading.value = false
            result.fold(
                onSuccess = {
                    _statusMessage.value = null
                    _isLoggedIn.value = true
                },
                onFailure = { error ->
                    Log.e("AuthViewModel", "Google login failed", error)
                    _statusMessage.value = null
                    _errorMessage.value = mapGoogleLoginError(error)
                }
            )
        }
    }

    private fun mapSignUpError(error: Throwable): String = when (error) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidCredentialsException -> "Please enter a valid email address."
        is FirebaseNetworkException -> "Sign-up needs a network connection."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
        else -> "Sign-up failed. Please try again."
    }

    private fun mapLoginError(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email/username or password."
        is FirebaseNetworkException -> "Network error. Please check your connection."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Username login is unavailable right now. Try your email address."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Network error. Please check your connection."
            else -> "Login failed. Please try again."
        }
        else -> when {
            error.message?.contains("No account found", ignoreCase = true) == true ->
                error.message ?: "No account found."
            error.message?.contains("network", ignoreCase = true) == true ->
                "Network error. Please check your connection."
            else -> "Login failed. Please try again."
        }
    }

    private fun mapGoogleLoginError(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidCredentialsException ->
            "Google sign-in is misconfigured for this app. Verify the web OAuth client, SHA fingerprints, and download a fresh google-services.json."
        is FirebaseAuthException -> when (error.errorCode.uppercase()) {
            "ERROR_OPERATION_NOT_ALLOWED" ->
                "Google sign-in is disabled in Firebase Authentication."
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "This email already exists with a different sign-in method."
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_INVALID_IDP_RESPONSE" ->
                "Google sign-in is misconfigured for this app. Verify the web OAuth client, SHA fingerprints, and download a fresh google-services.json."
            else -> error.message ?: "Google login failed. Please try again."
        }
        is FirebaseNetworkException -> "Google sign-in needs a network connection."
        is FirebaseTooManyRequestsException -> "Too many sign-in attempts. Please try again later."
        else -> error.message ?: "Google login failed. Please try again."
    }
}


