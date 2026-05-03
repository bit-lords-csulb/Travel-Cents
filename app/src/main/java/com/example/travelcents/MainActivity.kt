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
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.travelcents.notification.ChatNotificationTarget
import com.example.travelcents.ui.TravelCentsNavigation
import com.example.travelcents.ui.auth.AuthViewModel
import com.example.travelcents.ui.theme.TravelCentsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private val _pendingChatTarget = MutableStateFlow<ChatNotificationTarget?>(null)
    val pendingChatTarget = _pendingChatTarget.asStateFlow()

    fun clearPendingChatTarget() {
        _pendingChatTarget.value = null
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
        val authViewModel : AuthViewModel by viewModels()
        setContent {
            TravelCentsTheme {
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

    override fun onStop() {
        super.onStop()
        // Clear active chat when minimized so background notifications aren't suppressed
        com.example.travelcents.notification.NotificationHelper.activeChatTarget = null
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
