package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.user.model.RegionalData
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun PreferencesTab(viewModel: SettingsViewModel = viewModel()) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        NotificationsSection()
        RegionalSettingsSection(
            country = userState.country,
            region = userState.region,
            city = userState.city,
            onSettingsChange = { c, r, city -> viewModel.setRegionalSettings(c, r, city) }
        )
        FeaturesSection(
            showWeeklySummary = userState.showWeeklySummary,
            onWeeklySummaryChange = { viewModel.setShowWeeklySummary(it) }
        )
        DisplaySection()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionalSettingsSection(
    country: String,
    region: String,
    city: String,
    onSettingsChange: (String, String, String) -> Unit
) {
    var showCountryDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    val selectedCountryInfo = remember(country) { RegionalData.getCountry(country) }
    val selectedRegionInfo = remember(selectedCountryInfo, region) {
        selectedCountryInfo?.regions?.find { it.name == region }
    }

    SettingHeader("Regional Settings")
    SettingCard {
        ClickableSettingItem(
            title = "Country",
            value = country,
            onClick = { showCountryDialog = true }
        )
        ClickableSettingItem(
            title = "State / Province",
            value = region,
            onClick = { if (selectedCountryInfo != null) showRegionDialog = true }
        )
        ClickableSettingItem(
            title = "City",
            value = city,
            onClick = { if (selectedRegionInfo != null) showCityDialog = true },
            showDivider = false
        )
    }

    if (showCountryDialog) {
        SelectionDialog(
            title = "Select Country",
            options = RegionalData.countries.map { it.name },
            onDismiss = { showCountryDialog = false },
            onSelect = { newCountry ->
                val newCountryInfo = RegionalData.getCountry(newCountry)
                val newRegion = newCountryInfo?.regions?.firstOrNull()?.name ?: ""
                val newCity = newCountryInfo?.regions?.firstOrNull()?.cities?.firstOrNull()?.name ?: ""
                onSettingsChange(newCountry, newRegion, newCity)
                showCountryDialog = false
            }
        )
    }

    if (showRegionDialog && selectedCountryInfo != null) {
        SelectionDialog(
            title = "Select Region",
            options = selectedCountryInfo.regions.map { it.name },
            onDismiss = { showRegionDialog = false },
            onSelect = { newRegion ->
                val newRegionInfo = selectedCountryInfo.regions.find { it.name == newRegion }
                val newCity = newRegionInfo?.cities?.firstOrNull()?.name ?: ""
                onSettingsChange(country, newRegion, newCity)
                showRegionDialog = false
            }
        )
    }

    if (showCityDialog && selectedRegionInfo != null) {
        SelectionDialog(
            title = "Select City",
            options = selectedRegionInfo.cities.map { it.name },
            onDismiss = { showCityDialog = false },
            onSelect = { newCity ->
                onSettingsChange(country, region, newCity)
                showCityDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = DeepSea1
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    color = DeepSea5,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(options) { option ->
                        Text(
                            text = option,
                            color = DeepSea5,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturesSection(
    showWeeklySummary: Boolean,
    onWeeklySummaryChange: (Boolean) -> Unit
) {
    SettingHeader("Features")
    SettingCard {
        SwitchSettingItem(
            title = "Weekly Summary",
            subtitle = "Receive a weekly summary of your events and spending",
            checked = showWeeklySummary,
            onCheckedChange = onWeeklySummaryChange,
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
