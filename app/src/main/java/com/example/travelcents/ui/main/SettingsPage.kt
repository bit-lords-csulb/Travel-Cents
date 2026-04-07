package com.example.travelcents.ui.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.FirestoreRepository
import com.example.travelcents.ui.auth.AuthViewModel
import com.example.travelcents.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onLoggedOut: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Preferences") }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "demo@student.csulb.edu"
    var userName by remember { mutableStateOf("Loading...") }
    var refreshTrigger by remember { mutableStateOf(0) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Fetch user name from Firestore
    LaunchedEffect(currentUser?.uid, refreshTrigger) {
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = DeepSea2,
            titleContentColor = DeepSea5,
            textContentColor = DeepSea4,
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.signOut()
                    showLogoutDialog = false
                    onLoggedOut()
                }) {
                    Text("Yes, I'm sure", color = Color.Red)
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
            text = { Text("Are you sure? All data will be lost permanently. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.deleteAccount { success ->
                        if (success) {
                            showDeleteDialog = false
                            onLoggedOut()
                        }
                    }
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { showLogoutDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = DeepSea4)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = Color.Red.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
            "Profile" -> ProfileTab(onProfileUpdated = { refreshTrigger++ })
            "Security" -> SecurityTab()
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileTab(onProfileUpdated: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                firstName = doc.getString("firstName") ?: ""
                lastName = doc.getString("lastName") ?: ""
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DeepSea3)
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Personal Information",
                color = DeepSea5,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!isEditing) {
                Button(
                    onClick = { isEditing = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Edit", color = DeepSea5)
                }
            } else {
                Row {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel", color = DeepSea4)
                    }
                    Button(
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.uid?.let { uid ->
                                val updates = mapOf(
                                    "firstName" to firstName,
                                    "lastName" to lastName
                                )
                                db.collection("users").document(uid).update(updates)
                                    .addOnSuccessListener {
                                        isEditing = false
                                        onProfileUpdated()
                                        message = "Profile updated!"
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", color = DeepSea5)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileInputField(label = "First Name", value = firstName, onValueChange = { firstName = it }, enabled = isEditing)
        ProfileInputField(label = "Last Name", value = lastName, onValueChange = { lastName = it }, enabled = isEditing)
        ProfileInputField(label = "Email", value = email, onValueChange = { email = it }, enabled = false, icon = Icons.Default.Mail)

        if (message != null) {
            Text(
                text = message!!,
                color = DeepSea4,
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ProfileInputField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, color = DeepSea4, fontSize = 14.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = DeepSea4, modifier = Modifier.size(18.dp).padding(end = 8.dp))
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = DeepSea3,
                    unfocusedIndicatorColor = DeepSea2,
                    disabledIndicatorColor = DeepSea2,
                    focusedTextColor = DeepSea5,
                    unfocusedTextColor = DeepSea5,
                    disabledTextColor = DeepSea5.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun SecurityTab() {
    val auth = FirebaseAuth.getInstance()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    if (showSuccess) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFF4CAF50).copy(alpha = 0.2f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person, // Use a check icon if available
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Password Updated!", color = DeepSea5, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Your password has been successfully changed.", color = DeepSea4, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { showSuccess = false },
                colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continue", color = DeepSea5)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Change Password", color = DeepSea5, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        SecurityInputField(
            label = "Current Password",
            value = currentPassword,
            onValueChange = { currentPassword = it },
            isVisible = isPasswordVisible,
            onToggleVisibility = { isPasswordVisible = !isPasswordVisible }
        )
        SecurityInputField(
            label = "New Password",
            value = newPassword,
            onValueChange = { newPassword = it },
            isVisible = isPasswordVisible,
            onToggleVisibility = { isPasswordVisible = !isPasswordVisible }
        )
        SecurityInputField(
            label = "Confirm Password",
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            isVisible = isPasswordVisible,
            onToggleVisibility = { isPasswordVisible = !isPasswordVisible }
        )

        if (error != null) {
            Text(error!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (newPassword != confirmPassword) {
                    error = "Passwords do not match"
                    return@Button
                }
                val user = auth.currentUser
                val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)
                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                showSuccess = true
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                error = null
                            } else {
                                error = updateTask.exception?.message
                            }
                        }
                    } else {
                        error = "Incorrect current password"
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = DeepSea3),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Update Password", color = DeepSea5)
        }
    }
}

@Composable
fun SecurityInputField(label: String, value: String, onValueChange: (String) -> Unit, isVisible: Boolean, onToggleVisibility: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, color = DeepSea4, fontSize = 14.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = DeepSea4
                    )
                }
            },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = DeepSea4, modifier = Modifier.size(18.dp))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = DeepSea3,
                unfocusedIndicatorColor = DeepSea2,
                focusedTextColor = DeepSea5,
                unfocusedTextColor = DeepSea5
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
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
