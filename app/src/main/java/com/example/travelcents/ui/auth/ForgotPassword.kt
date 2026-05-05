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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.components.TcTextField
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

@Composable
fun ForgotPassword(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }

    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()
    val isPasswordResetEmailSent by authViewModel.isPasswordResetEmailSent.collectAsStateWithLifecycle()
    val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()
    val statusMessage by authViewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        authViewModel.resetForgotPasswordState()
    }

    LaunchedEffect(isPasswordResetEmailSent) {
        if (isPasswordResetEmailSent) {
            navController.popBackStack()
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
            text = "Reset Password",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5,
            fontFamily = TravelCentsFonts.Headline
        )

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .requiredWidth(126.dp)
                .height(3.dp)
                .background(DeepSea5)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = TravelCentsFonts.Body
        )

        Spacer(modifier = Modifier.height(28.dp))

        TcTextField(
            value = email,
            onValueChange = {
                email = it
                authViewModel.clearMessages()
            },
            label = "Email",
            placeholder = "name@example.com",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { authViewModel.sendPasswordResetEmail(email) }
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

        TcButton(
            onClick = { authViewModel.sendPasswordResetEmail(email) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.requiredSize(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Send Reset Email",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remembered it? ",
                color = Color.White,
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
    }
}
