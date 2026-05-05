package com.example.travelcents.ui.main.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelcents.data.user.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    val doNotShareData: Boolean = false,
    val showWeeklySummary: Boolean = true,
    val country: String = "United States",
    val region: String = "California",
    val city: String = "Long Beach",
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
                    doNotShareData = profile.doNotShareData,
                    showWeeklySummary = profile.showWeeklySummary,
                    country = profile.country,
                    region = profile.region,
                    city = profile.city,
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

    fun setDoNotShareData(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val previous = _userState.value.doNotShareData
        _userState.value = _userState.value.copy(doNotShareData = enabled)
        db.collection("users").document(uid)
            .update("doNotShareData", enabled)
            .addOnFailureListener {
                _userState.value = _userState.value.copy(doNotShareData = previous)
            }
    }

    fun setShowWeeklySummary(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val previous = _userState.value.showWeeklySummary
        _userState.value = _userState.value.copy(showWeeklySummary = enabled)
        db.collection("users").document(uid)
            .update("showWeeklySummary", enabled)
            .addOnFailureListener {
                _userState.value = _userState.value.copy(showWeeklySummary = previous)
            }
    }

    fun setRegionalSettings(country: String, region: String, city: String) {
        val uid = auth.currentUser?.uid ?: return
        val previous = Triple(_userState.value.country, _userState.value.region, _userState.value.city)
        _userState.value = _userState.value.copy(
            country = country,
            region = region,
            city = city
        )
        db.collection("users").document(uid)
            .update(mapOf(
                "country" to country,
                "region" to region,
                "city" to city
            ))
            .addOnFailureListener {
                _userState.value = _userState.value.copy(
                    country = previous.first,
                    region = previous.second,
                    city = previous.third
                )
            }
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
