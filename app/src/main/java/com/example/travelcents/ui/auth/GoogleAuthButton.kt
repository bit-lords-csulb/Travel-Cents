package com.example.travelcents.ui.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.travelcents.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape

private const val GOOGLE_SERVER_CLIENT_ID =
    "805994740096-d31i7240546h0701vh3m2kphrtsqqqtt.apps.googleusercontent.com"

@Composable
fun GoogleAuthButton(
    text: String,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            authViewModel.clearMessages()
            authViewModel.setStatusMessage("Google sign-in button pressed. Opening account picker...")
            Log.d("GoogleAuth", "Google sign-in button pressed")
            coroutineScope.launch {
                val credentialManager = CredentialManager.create(context)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GOOGLE_SERVER_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                try {
                    Log.d("GoogleAuth", "Launching Credential Manager")
                    val result = credentialManager.getCredential(
                        request = request,
                        context = context,
                    )

                    handleCredentialResult(result, authViewModel)
                } catch (e: GetCredentialCancellationException) {
                    Log.w("GoogleAuth", "Google Sign-In canceled", e)
                    authViewModel.setStatusMessage("Google sign-in canceled.")
                } catch (e: NoCredentialException) {
                    Log.e("GoogleAuth", "No Google credential available", e)
                    authViewModel.setErrorMessage("No Google account was returned. Check the picker and try again.")
                } catch (e: GetCredentialException) {
                    Log.e("GoogleAuth", "Google Sign-In failed", e)
                    val details = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                    authViewModel.setErrorMessage("Google sign-in failed before Firebase: $details")
                } catch (t: Throwable) {
                    Log.e("GoogleAuth", "Unexpected Google Sign-In failure", t)
                    val details = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
                    authViewModel.setErrorMessage("Unexpected Google sign-in error: $details")
                }
            }
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_google_g),
            contentDescription = null,
            modifier = Modifier.requiredSize(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun handleCredentialResult(
    result: GetCredentialResponse,
    authViewModel: AuthViewModel
) {
    val credential = result.credential
    Log.d("GoogleAuth", "Credential Manager returned type='${credential.type}'")

    if (
        credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        authViewModel.setStatusMessage("Google account selected. Signing in...")
        authViewModel.logInWithGoogle(googleIdTokenCredential.idToken)
        return
    }

    authViewModel.setErrorMessage("Google sign-in returned an unsupported credential: ${credential.type}")
}
