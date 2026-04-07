package com.example.travelcents.ui.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.view.View
import androidx.core.content.edit
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea4

private const val LOGIN_PREFS = "login_preferences"
private const val KEY_REMEMBER_ME = "remember_me"
private const val KEY_SAVED_EMAIL = "saved_email_or_username"
private const val KEY_SAVED_PASSWORD = "saved_password"

@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val sharedPreferences = remember(context) {
        context.getSharedPreferences(LOGIN_PREFS, Context.MODE_PRIVATE)
    }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMeState by remember { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Suppress Android autofill overlay (clipboard/saved credentials popup)
    val view = LocalView.current
    DisposableEffect(Unit) {
        val previous = view.importantForAutofill
        view.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        onDispose { view.importantForAutofill = previous }
    }

    // Collect StateFlows as Compose state
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by authViewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(sharedPreferences) {
        val shouldRemember = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
        rememberMeState = shouldRemember
        if (shouldRemember) {
            email = sharedPreferences.getString(KEY_SAVED_EMAIL, "").orEmpty()
            password = sharedPreferences.getString(KEY_SAVED_PASSWORD, "").orEmpty()
        }
    }

    // Navigate to the home screen if the user is logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            sharedPreferences.edit {
                if (rememberMeState) {
                    putBoolean(KEY_REMEMBER_ME, true)
                    putString(KEY_SAVED_EMAIL, email.trim())
                    putString(KEY_SAVED_PASSWORD, password)
                } else {
                    remove(KEY_REMEMBER_ME)
                    remove(KEY_SAVED_EMAIL)
                    remove(KEY_SAVED_PASSWORD)
                }
            }
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(180.dp))

        // LOG IN TITLE
        Text(
            text = "Log In",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .requiredWidth(74.dp)
                .height(3.dp)
                .background(DeepSea4)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // EMAIL / USERNAME SECTION
        Text(text = "Email or Username", color = Color.White, fontSize = 16.sp)
        TextField(
            value = email,
            onValueChange = {
                email = it
                authViewModel.clearError()
            },
            placeholder = { Text("email or username", color = Color.Gray, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .focusRequester(emailFocusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                            focusManager.moveFocus(
                                if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                            )
                            true
                        }
                        event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                authViewModel.logIn(email, password)
                            } else {
                                passwordFocusRequester.requestFocus()
                            }
                            true
                        }
                        event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                authViewModel.logIn(email, password)
                            } else {
                                passwordFocusRequester.requestFocus()
                            }
                            true
                        }
                        else -> false
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = DeepSea4
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PASSWORD SECTION
        Text(text = "Password", color = Color.White, fontSize = 16.sp)
        TextField(
            value = password,
            onValueChange = {
                password = it
                authViewModel.clearError()
            },
            placeholder = { Text("enter password", color = Color.Gray, fontSize = 14.sp) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .focusRequester(passwordFocusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                            focusManager.moveFocus(
                                if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
                            )
                            true
                        }
                        event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                            authViewModel.logIn(email, password)
                            true
                        }
                        else -> false
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { authViewModel.logIn(email, password) }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = DeepSea4
            )
        )

        // REMEMBER ME & FORGOT PASSWORD
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMeState = !rememberMeState }
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.5.dp, DeepSea4, RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (rememberMeState) {
                        Box(
                            modifier = Modifier
                                .requiredSize(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DeepSea4)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Remember Me",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(onClick = { navController.navigate("forgot_password") }) {
                Text("Forgot Password?", color = DeepSea4, fontSize = 12.sp)
            }
        }

        // SUCCESS MESSAGE (e.g. "Account created! Please log in.")
        statusMessage?.let { message ->
            Text(
                text = message,
                color = Color.Green,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // ERROR MESSAGE
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // LOGIN BUTTON
        Button(
            onClick = { authViewModel.logIn(email, password) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepSea2)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.requiredSize(24.dp))
            } else {
                Text("Login", color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an Account? ",
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = "Sign Up",
                color = DeepSea4,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = !isLoading) {
                    navController.navigate("signup") {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
