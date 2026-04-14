package com.example.travelcents.ui.main.settings

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUserState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = true
) {
    val displayName: String get() = "$firstName $lastName".trim().ifEmpty { "User" }
}

class SettingsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userState = MutableStateFlow(SettingsUserState())
    val userState: StateFlow<SettingsUserState> = _userState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        val user = auth.currentUser ?: run {
            _userState.value = SettingsUserState(firstName = "Guest", isLoading = false)
            return
        }
        _userState.value = _userState.value.copy(email = user.email ?: "")
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                _userState.value = SettingsUserState(
                    firstName = doc.getString("firstName") ?: "",
                    lastName = doc.getString("lastName") ?: "",
                    username = doc.getString("username") ?: "",
                    email = user.email ?: "",
                    isLoading = false
                )
            }
            .addOnFailureListener {
                _userState.value = _userState.value.copy(isLoading = false)
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