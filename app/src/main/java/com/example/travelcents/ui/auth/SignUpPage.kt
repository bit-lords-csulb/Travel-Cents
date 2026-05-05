package com.example.travelcents.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelcents.ui.components.TcTextField
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun SignUpPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val isAccountCreated by authViewModel.isAccountCreated.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by authViewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.resetSignUpState()
    }

    LaunchedEffect(isAccountCreated) {
        if (isAccountCreated) {
            navController.popBackStack()
        }
    }

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 34.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(108.dp))

        Text(
            text = "Sign Up",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5,
            fontFamily = TravelCentsFonts.Headline
        )

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .requiredWidth(92.dp)
                .height(3.dp)
                .background(DeepSea5)
        )

        Spacer(modifier = Modifier.height(32.dp))

        TcTextField(
            value = firstName,
            onValueChange = { firstName = it; authViewModel.clearError() },
            label = "First Name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body
        )

        Spacer(modifier = Modifier.height(18.dp))

        TcTextField(
            value = lastName,
            onValueChange = { lastName = it; authViewModel.clearError() },
            label = "Last Name",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body
        )

        Spacer(modifier = Modifier.height(18.dp))

        TcTextField(
            value = username,
            onValueChange = { username = it; authViewModel.clearError() },
            label = "Username",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = TripWizardColors.Blue
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        TcTextField(
            value = email,
            onValueChange = { email = it; authViewModel.clearError() },
            label = "Email",
            placeholder = "demo@student.csulb.edu",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body,
            placeholderFontFamily = TravelCentsFonts.Body,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = TripWizardColors.Blue
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        TcTextField(
            value = password,
            onValueChange = { password = it; authViewModel.clearError() },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = TripWizardColors.Blue
                )
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = TripWizardColors.Blue
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        TcTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; authViewModel.clearError() },
            label = "Confirm Password",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            textFontFamily = TravelCentsFonts.Body,
            labelFontFamily = TravelCentsFonts.Body,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = TripWizardColors.Blue
                )
            },
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (isConfirmPasswordVisible) "Hide confirm password" else "Show confirm password",
                        tint = TripWizardColors.Blue
                    )
                }
            }
        )

        statusMessage?.let { message ->
            Text(
                text = message,
                color = Color.Green,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 14.dp),
                fontFamily = TravelCentsFonts.Body
            )
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 14.dp),
                fontFamily = TravelCentsFonts.Body
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (password != confirmPassword) {
                    authViewModel.setErrorMessage("Passwords do not match")
                } else {
                    authViewModel.signUp(firstName, lastName, username, email, password)
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TripWizardColors.ContainerHigh,
                contentColor = DeepSea5
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.requiredSize(22.dp),
                    color = DeepSea5,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign Up",
                    color = DeepSea5,
                    fontSize = 18.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GoogleAuthButton(
            text = "Signup with Google",
            authViewModel = authViewModel,
            enabled = !isLoading,
            fontFamily = TravelCentsFonts.Body
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an Account? ",
                color = DeepSea5,
                fontSize = 12.sp,
                fontFamily = TravelCentsFonts.Body
            )
            Text(
                text = "Log In",
                color = DeepSea4,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TravelCentsFonts.Body,
                modifier = Modifier.clickable(enabled = !isLoading) {
                    navController.popBackStack()
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

