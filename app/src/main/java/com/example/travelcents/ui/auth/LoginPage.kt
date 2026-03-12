package com.example.travelcents.ui.auth

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import kotlin.Result
import kotlin.runCatching
@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMeState by remember { mutableStateOf(false) }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    // Collect StateFlows as Compose state
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val isAccountCreated by authViewModel.isAccountCreated.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by authViewModel.statusMessage.collectAsStateWithLifecycle()

    // Navigate to the home screen if the user is logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
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

        // EMAIL SECTION
        Text(text = "Email", color = Color.White, fontSize = 16.sp)
        TextField(
            value = email,
            onValueChange = { newValue ->
                // Ignore tab characters
                if (!newValue.contains('\t')) {
                    email = newValue
                    authViewModel.clearError()
                }
            },
            placeholder = { Text("demo@student.csulb.edu", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .focusRequester(emailFocusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Tab && event.type == KeyEventType.KeyDown && !event.isShiftPressed -> {
                            passwordFocusRequester.requestFocus()
                            true // consume the event
                        }

                        else -> false
                    }
                },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = DeepSea4,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // PASSWORD SECTION
        Text(text = "Password", color = Color.White, fontSize = 16.sp)
        TextField(
            value = password,
            onValueChange = { newValue ->
                // Ignore tab characters
                if (!newValue.contains('\t')) {
                    password = newValue
                    authViewModel.clearError()
                }
            },
            placeholder = { Text("enter password", color = Color.Gray, fontSize = 14.sp) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .focusRequester(passwordFocusRequester)
                .onPreviewKeyEvent { event ->
                    when {
                        event.key == Key.Tab && event.type == KeyEventType.KeyDown && event.isShiftPressed -> {
                            emailFocusRequester.requestFocus()
                            true // consume the event
                        }

                        event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                            true // consume tab, nowhere to go forward
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
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = DeepSea4,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // REMEMBER ME & FORGOT PASSWORD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .requiredSize(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .border(1.5.dp, DeepSea4, RoundedCornerShape(3.dp))
                        .clickable { rememberMeState = !rememberMeState },
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
                CircularProgressIndicator(color = DeepSea5, modifier = Modifier.requiredSize(24.dp))
            } else {
                Text("Login", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}