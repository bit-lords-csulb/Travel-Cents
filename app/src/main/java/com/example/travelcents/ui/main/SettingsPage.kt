package com.example.travelcents.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.FirestoreRepository
import com.example.travelcents.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsPage(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf("Preferences") }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "demo@student.csulb.edu"
    var userName by remember { mutableStateOf("Jon Snow") } // Default placeholder

    // Fetch user name from Firestore
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            Log.d("SettingsPage", "Fetching name for UID: $uid")
            FirestoreRepository().fetchUser(uid) { fetchedName ->
                Log.d("SettingsPage", "Fetched name: $fetchedName")
                if (fetchedName.isNotBlank() && fetchedName != "Me") {
                    userName = fetchedName
                }
            }
        } else {
            userName = "Guest User"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Section Header
        Spacer(modifier = Modifier.height(16.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, DeepSea3, CircleShape),
                color = DeepSea2
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    tint = DeepSea4,
                    modifier = Modifier.padding(24.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, DeepSea1, CircleShape),
                color = DeepSea3
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = DeepSea5,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userName,
            color = DeepSea5,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = userEmail,
            color = DeepSea4,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea2, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Profile", "Security", "Preferences")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) DeepSea3 else Color.Transparent)
                        .clickable { selectedTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) DeepSea5 else DeepSea4,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Content Area
        when (selectedTab) {
            "Preferences" -> PreferencesTab()
            "Profile" -> PlaceholderContent("Profile Settings Section")
            "Security" -> PlaceholderContent("Security Settings Section")
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PreferencesTab() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingHeader("Notifications")
        
        var emailNotif by remember { mutableStateOf(true) }
        var pushNotif by remember { mutableStateOf(false) }
        var weeklyDigest by remember { mutableStateOf(false) }

        SwitchSettingItem(
            title = "Email Notifications",
            subtitle = "Receive email updates about your activity",
            checked = emailNotif,
            onCheckedChange = { emailNotif = it }
        )
        SwitchSettingItem(
            title = "Push Notifications",
            subtitle = "Get push notifications on your device",
            checked = pushNotif,
            onCheckedChange = { pushNotif = it }
        )
        SwitchSettingItem(
            title = "Weekly Digest",
            subtitle = "Receive a weekly summary of your activity",
            checked = weeklyDigest,
            onCheckedChange = { weeklyDigest = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingHeader("Display")
        var darkMode by remember { mutableStateOf(true) }
        SwitchSettingItem(
            title = "Dark Mode",
            subtitle = "Use Dark theme throughout the app",
            checked = darkMode,
            onCheckedChange = { darkMode = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingHeader("Regional Settings")
        RegionalInputItem(label = "Language", value = "English")
        RegionalInputItem(label = "Timezone", value = "Pacific (PST)")
    }
}

@Composable
fun SettingHeader(text: String) {
    Text(
        text = text,
        color = DeepSea5,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = DeepSea4, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DeepSea5,
                checkedTrackColor = DeepSea3,
                uncheckedThumbColor = DeepSea4,
                uncheckedTrackColor = DeepSea2,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun RegionalInputItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = label, color = DeepSea4, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DeepSea3, thickness = 1.dp)
    }
}

@Composable
fun PlaceholderContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = DeepSea4, fontSize = 16.sp)
    }
}
