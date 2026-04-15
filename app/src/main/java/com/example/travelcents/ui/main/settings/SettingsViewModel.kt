package com.example.travelcents.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.user.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUserState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val isLoading: Boolean = true
) {
    val displayName: String get() = "$firstName $lastName".trim().ifEmpty { "User" }
}

class SettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userProfileRepository = UserProfileRepository(auth = auth, db = db)

    private val _userState = MutableStateFlow(SettingsUserState())
    val userState: StateFlow<SettingsUserState> = _userState.asStateFlow()

    init {
        observeUser()
        viewModelScope.launch {
            userProfileRepository.syncCurrentUserGoogleProfile()
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            userProfileRepository.observeCurrentUserProfile().collect { profile ->
                _userState.value = SettingsUserState(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    username = profile.username,
                    email = profile.email,
                    profileImageUrl = profile.profileImageUrl,
                    isLoading = profile.isLoading
                )
            }
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update(mapOf("firstName" to firstName, "lastName" to lastName))
            .addOnSuccessListener {
                _userState.value = _userState.value.copy(
                    firstName = firstName,
                    lastName = lastName
                )
                onSuccess()
            }
            .addOnFailureListener { e -> onError(e.message ?: "Update failed") }
    }

    fun signOut(onComplete: () -> Unit) {
        auth.signOut()
        _userState.value = SettingsUserState(firstName = "Guest", isLoading = false)
        onComplete()
    }

    fun deleteAccount(onComplete: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        user.delete()
            .addOnSuccessListener {
                db.collection("users").document(uid).delete()
                onComplete()
            }
            .addOnFailureListener { e -> onError(e.message ?: "Delete failed") }
    }
}

