package com.example.travelcents.ui.main.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.components.TcTextField
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SecurityTab() {
    Column(modifier = Modifier.fillMaxWidth()) {
        ChangePasswordSection()
        PrivacySection()
    }
}

@Composable
private fun ChangePasswordSection() {
    val auth = FirebaseAuth.getInstance()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // Each field has its own visibility toggle
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    SettingHeader("Change Password")
    SettingCard {
        Spacer(modifier = Modifier.height(8.dp))

        TcTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; error = null; successMessage = null },
            label = "Current Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TripWizardColors.Blue)
            },
            trailingIcon = {
                IconButton(onClick = { showCurrent = !showCurrent }) {
                    Icon(
                        imageVector = if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TripWizardColors.OnSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TcTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null; successMessage = null },
            label = "New Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TripWizardColors.Blue)
            },
            trailingIcon = {
                IconButton(onClick = { showNew = !showNew }) {
                    Icon(
                        imageVector = if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TripWizardColors.OnSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TcTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; error = null; successMessage = null },
            label = "Confirm New Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TripWizardColors.Blue)
            },
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        imageVector = if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TripWizardColors.OnSurfaceVariant
                    )
                }
            }
        )

        error?.let {
            Text(
                text = it,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }
        successMessage?.let {
            Text(
                text = it,
                color = TripWizardColors.Blue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (newPassword != confirmPassword) {
                    error = "Passwords do not match."
                    return@Button
                }
                if (newPassword.length < 8) {
                    error = "Password must be at least 8 characters."
                    return@Button
                }
                val user = auth.currentUser ?: return@Button
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                error = null
                                successMessage = "Password updated successfully."
                            } else {
                                error = updateTask.exception?.message ?: "Update failed."
                            }
                        }
                    } else {
                        error = "Incorrect current password."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TripWizardColors.Blue,
                contentColor = Color(0xFF001627)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Update Password", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PrivacySection() {
    var doNotShareData by remember { mutableStateOf(false) }
    var showDataPrivacyInfo by remember { mutableStateOf(false) }

    SettingHeader("Privacy & Security")
    SettingCard {
        SwitchSettingItem(
            title = "Do Not Share My Data",
            subtitle = "Opt out of data sharing with third parties",
            checked = doNotShareData,
            onCheckedChange = { doNotShareData = it }
        )
        // Tappable row that expands/collapses the privacy info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDataPrivacyInfo = !showDataPrivacyInfo }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Data & Privacy", color = DeepSea5, fontSize = 15.sp)
            Text(
                text = if (showDataPrivacyInfo) "▲" else "▼",
                color = DeepSea4,
                fontSize = 12.sp
            )
        }

        if (showDataPrivacyInfo) {
            HorizontalDivider(color = DeepSea3.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Travel Cents collects only the information needed to provide its services — your account details, trip plans, and chat messages.",
                color = DeepSea4,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We do not sell your personal data to third parties. Enabling \"Do Not Share My Data\" opts you out of any optional analytics sharing.",
                color = DeepSea4,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showDataPrivacyInfo = false }) {
                Text("Close", color = TripWizardColors.Blue)
            }
        }
    }
}
