package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.BuildConfig
import com.example.travelcents.data.debug.DebugPreferencesRepository
import com.example.travelcents.ui.theme.DeepSea4

@Composable
fun PreferencesTab() {
    Column(modifier = Modifier.fillMaxWidth()) {
        NotificationsSection()
        DisplaySection()
        if (BuildConfig.DEBUG) DeveloperSection()
        AboutSection()
    }
}

@Composable
private fun NotificationsSection() {
    var tripReminders by remember { mutableStateOf(true) }
    var groupChatNotifs by remember { mutableStateOf(true) }
    var appUpdates by remember { mutableStateOf(false) }

    SettingHeader("Notifications")
    SettingCard {
        SwitchSettingItem(
            title = "Trip Reminders",
            subtitle = "Get reminded before upcoming trip events",
            checked = tripReminders,
            onCheckedChange = { tripReminders = it }
        )
        SwitchSettingItem(
            title = "Group Chat Messages",
            subtitle = "Notifications for new messages in your group chats",
            checked = groupChatNotifs,
            onCheckedChange = { groupChatNotifs = it }
        )
        SwitchSettingItem(
            title = "App Updates",
            subtitle = "Be notified about new features and updates",
            checked = appUpdates,
            onCheckedChange = { appUpdates = it },
            showDivider = false
        )
    }
}

@Composable
private fun DisplaySection() {
    var darkMode by remember { mutableStateOf(true) }

    SettingHeader("Display")
    SettingCard {
        SwitchSettingItem(
            title = "Dark Mode",
            subtitle = "Use the DeepSea dark theme throughout the app",
            checked = darkMode,
            onCheckedChange = { darkMode = it },
            showDivider = false
        )
    }
}

@Composable
private fun DeveloperSection() {
    val showAiDebugUi by DebugPreferencesRepository.showAiDebugUi.collectAsState()

    SettingHeader("Developer")
    SettingCard {
        SwitchSettingItem(
            title = "AI Debug Overlay",
            subtitle = "Show source tags and decision logs in AI chat",
            checked = showAiDebugUi,
            onCheckedChange = { DebugPreferencesRepository.setShowAiDebugUi(it) },
            showDivider = false
        )
    }
}

@Composable
private fun AboutSection() {
    SettingHeader("About")
    SettingCard {
        SettingRow(label = "Version", value = "1.0.0")
        SettingRow(label = "Build", value = "2026.04 (beta)")
        SettingRow(label = "Team", value = "Bit Lords - CSULB")
        SettingRow(label = "Powered by", value = "AI planner + Firebase", showDivider = false)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Travel Cents helps you plan smarter trips with AI-generated itineraries tailored to your budget and interests.",
        color = DeepSea4,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

