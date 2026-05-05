package com.example.travelcents

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travelcents.notification.ChatNotificationTarget
import com.example.travelcents.data.preferences.ThemePreferencesRepository
import com.example.travelcents.ui.TravelCentsNavigation
import com.example.travelcents.ui.auth.AuthViewModel
import com.example.travelcents.ui.theme.TravelCentsTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val _pendingChatTarget = MutableStateFlow<ChatNotificationTarget?>(null)
    val pendingChatTarget = _pendingChatTarget.asStateFlow()

    fun clearPendingChatTarget() {
        _pendingChatTarget.value = null
    }

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var lastPresenceUid: String? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { authState ->
        val currentUid = authState.currentUser?.uid

        if (currentUid != null && currentUid != lastPresenceUid) {
            updateOnlineStatus(currentUid, true)
        }

        lastPresenceUid = currentUid
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        requestNotificationPermissionIfNeeded()
        
        auth.addAuthStateListener(authStateListener)

        val authViewModel : AuthViewModel by viewModels()
        val themePreferences = ThemePreferencesRepository.getInstance(applicationContext)
        setContent {
            val darkModeEnabled by themePreferences.darkModeEnabled.collectAsStateWithLifecycle()
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkModeEnabled
                controller.isAppearanceLightNavigationBars = !darkModeEnabled
            }
            TravelCentsTheme(darkTheme = darkModeEnabled, dynamicColor = false) {
                TravelCentsNavigation(
                    modifier = Modifier.fillMaxSize(),
                    authViewModel = authViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        updateCurrentUserOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateCurrentUserOnlineStatus(false)
        // Clear active chat when minimized so background notifications aren't suppressed
        com.example.travelcents.notification.NotificationHelper.activeChatTarget = null
    }

    override fun onDestroy() {
        auth.removeAuthStateListener(authStateListener)
        super.onDestroy()
    }

    private fun updateCurrentUserOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        updateOnlineStatus(uid, isOnline)
    }

    private fun updateOnlineStatus(uid: String, isOnline: Boolean) {
        val updates = mapOf(
            "isOnline" to isOnline,
            "lastSeen" to FieldValue.serverTimestamp()
        )
        db.collection("users")
            .document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener { Log.d("Presence", "Status updated: $isOnline") }
            .addOnFailureListener { Log.e("Presence", "Status update failed", it) }
    }

    private fun handleIntent(intent: Intent?) {
        val target = ChatNotificationTarget.fromExtras(
            chatType = intent?.getStringExtra(ChatNotificationTarget.EXTRA_CHAT_TYPE)
                ?: intent?.getStringExtra("chatType"),
            chatId = intent?.getStringExtra(ChatNotificationTarget.EXTRA_CHAT_ID)
                ?: intent?.getStringExtra("chatId")
        )
        Log.d("MainActivity", "handleIntent: target=$target")
        if (target != null) {
            _pendingChatTarget.value = target
        }
    }

    fun requestNotificationPermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        return hasPermission
    }
}
