package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.travelcents.ui.components.TcTextField
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun AccountTab(viewModel: SettingsViewModel, onLoggedOut: () -> Unit) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DeepSea2,
            titleContentColor = DeepSea5,
            textContentColor = DeepSea4,
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut(onLoggedOut) }) {
                    Text("Log Out", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = DeepSea5)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepSea2,
            titleContentColor = DeepSea5,
            textContentColor = DeepSea4,
            title = { Text("Delete Account") },
            text = { Text("All your data will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(
                        onComplete = onLoggedOut,
                        onError = { showDeleteDialog = false }
                    )
                }) {
                    Text("Delete Permanently", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = DeepSea5)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        // Personal Info section
        SettingHeader("Personal Information")
        SettingCard {
            SettingRow(label = "Display Name", value = userState.displayName)
            if (userState.username.isNotEmpty()) {
                SettingRow(label = "Username", value = "@${userState.username}")
            }
            SettingRow(label = "Email", value = userState.email, showDivider = false)
        }

        // Profile edit section
        SettingHeader("Edit Profile")
        SettingCard {
            if (!isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Update your name", color = DeepSea4, fontSize = 14.sp)
                    Button(
                        onClick = {
                            editFirstName = userState.firstName
                            editLastName = userState.lastName
                            isEditing = true
                            saveMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Edit", color = DeepSea5, fontSize = 14.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                TcTextField(
                    value = editFirstName,
                    onValueChange = { editFirstName = it },
                    label = "First Name",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(12.dp))
                TcTextField(
                    value = editLastName,
                    onValueChange = { editLastName = it },
                    label = "Last Name",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Email field (read-only)
                TcTextField(
                    value = userState.email,
                    onValueChange = {},
                    label = "Email",
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel", color = DeepSea4)
                    }
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                firstName = editFirstName.trim(),
                                lastName = editLastName.trim(),
                                onSuccess = {
                                    isEditing = false
                                    saveMessage = "Profile updated."
                                },
                                onError = { saveMessage = "Update failed. Try again." }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TripWizardColors.Blue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = DeepSea5, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        saveMessage?.let {
            Text(
                text = it,
                color = if (it.startsWith("Profile")) TripWizardColors.Blue else Color.Red,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }

        // Danger Zone
        SettingHeader("Danger Zone")
        SettingCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Log Out", color = DeepSea5, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Sign out of your account", color = DeepSea4, fontSize = 12.sp)
                }
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Log Out", color = DeepSea5, fontSize = 13.sp)
                }
            }
            androidx.compose.material3.HorizontalDivider(
                color = androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Delete Account", color = Color.Red, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Permanently remove all data", color = DeepSea4, fontSize = 12.sp)
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.Red, fontSize = 13.sp)
                }
            }
        }
    }
}
