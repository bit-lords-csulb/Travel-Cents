package com.example.travelcents.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.travelcents.ui.auth.AuthViewModel
import com.example.travelcents.ui.auth.ForgotPassword
import com.example.travelcents.ui.auth.LoginPage
import com.example.travelcents.ui.auth.SignUpPage
import com.example.travelcents.ui.main.MainScaffold

@Composable
fun TravelCentsNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login", builder = {
        composable("login") {
            LoginPage(modifier, navController, authViewModel)
        }

        composable("signup") {
            SignUpPage(modifier, navController, authViewModel)
        }

        composable("home") {
            MainScaffold(
                modifier = modifier,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("forgot_password") {
            ForgotPassword(modifier, navController, authViewModel)
        }

    })

}
