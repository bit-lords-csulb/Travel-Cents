package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private val tabs = listOf("Account", "Security", "Preferences")

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    onLoggedOut: () -> Unit = {}
) {
    val viewModel: SettingsViewModel = viewModel()
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    // Start on Account (leftmost / primary tab)
    var selectedTab by remember { mutableStateOf("Account") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Profile header — tapping opens Account tab
        ProfileHeader(
            displayName = userState.displayName,
            username = userState.username,
            email = userState.email,
            isLoading = userState.isLoading,
            onClick = { selectedTab = "Account" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab selector
        TabSelector(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tab content
        when (selectedTab) {
            "Account" -> AccountTab(viewModel = viewModel, onLoggedOut = onLoggedOut)
            "Security" -> SecurityTab()
            "Preferences" -> PreferencesTab()
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    username: String,
    email: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Avatar
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, DeepSea3, CircleShape),
                color = DeepSea2
            ) {
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TripWizardColors.Blue, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        tint = DeepSea4,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isLoading) "Loading…" else displayName,
            color = DeepSea5,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (username.isNotEmpty()) {
            Text(
                text = "@$username",
                color = TripWizardColors.Blue,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = email,
            color = DeepSea4,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TabSelector(
    tabs: List<String>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSea2, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) TripWizardColors.SurfaceBright else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .then(
                        if (isSelected) Modifier.border(1.dp, TripWizardColors.Blue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    color = if (isSelected) DeepSea5 else DeepSea4,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}