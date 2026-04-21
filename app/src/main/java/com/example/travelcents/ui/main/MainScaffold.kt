package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.travelcents.ui.components.MainBottomNavBar
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.ui.main.aichat.AiTripChatPage
import com.example.travelcents.ui.main.chats.chat.ChatsScreen
import com.example.travelcents.ui.main.current.CurrentDisplayMode
import com.example.travelcents.ui.main.current.CurrentTripScreen
import com.example.travelcents.ui.main.current.CurrentTripRoutes
import com.example.travelcents.ui.main.current.CurrentTripViewModel
import com.example.travelcents.ui.main.newTrip.NewTripLandingPage
import com.example.travelcents.ui.main.newTrip.NewTripViewModel
import com.example.travelcents.ui.main.newTrip.TripGeneratingPage
import com.example.travelcents.ui.main.newTrip.TripStep1DestinationPage
import com.example.travelcents.ui.main.newTrip.TripStep2DatesPage
import com.example.travelcents.ui.main.newTrip.TripStep3TravelersPage
import com.example.travelcents.ui.main.home.HomePage
import com.example.travelcents.ui.main.newTrip.TripStep4BudgetPage
import com.example.travelcents.ui.main.settings.SettingsPage
import com.example.travelcents.ui.main.newTrip.TripStep5InterestsPage
import com.example.travelcents.ui.theme.DeepSea1

object MainRoutes {
    const val CURRENT = CurrentTripRoutes.ROOT
    const val CURRENT_ITINERARY = CurrentTripRoutes.ITINERARY
    const val CURRENT_DAY = CurrentTripRoutes.DAY
    const val CURRENT_WEEK = CurrentTripRoutes.WEEK
    const val NEW_TRIP = "new_trip"
    const val NEW_TRIP_STEP_1 = "new_trip_step1"
    const val NEW_TRIP_STEP_2 = "new_trip_step2"
    const val NEW_TRIP_STEP_3 = "new_trip_step3"
    const val NEW_TRIP_STEP_4 = "new_trip_step4"
    const val NEW_TRIP_STEP_5 = "new_trip_step5"
    const val TRIP_GENERATING = "trip_generating"
    const val HOME = "home"
    const val CHATS = "chats"
    const val SETTINGS = "settings"

    const val AI_TRIP_CHAT = "ai_trip_chat"
}

private val bottomNavRoutes = setOf(
    MainRoutes.CURRENT,
    MainRoutes.CURRENT_ITINERARY,
    MainRoutes.CURRENT_DAY,
    MainRoutes.CURRENT_WEEK,
    MainRoutes.NEW_TRIP,
    MainRoutes.NEW_TRIP_STEP_1,
    MainRoutes.NEW_TRIP_STEP_2,
    MainRoutes.NEW_TRIP_STEP_3,
    MainRoutes.NEW_TRIP_STEP_4,
    MainRoutes.NEW_TRIP_STEP_5,
    MainRoutes.TRIP_GENERATING,
    MainRoutes.AI_TRIP_CHAT,
    MainRoutes.HOME,
    MainRoutes.CHATS,
    MainRoutes.SETTINGS
)

private fun shouldShowBottomNav(route: String): Boolean {
    return route in bottomNavRoutes ||
        route.startsWith("${MainRoutes.CURRENT}/") ||
        route.startsWith(MainRoutes.NEW_TRIP)
}

private fun selectedBottomRoute(route: String): String {
    return when {
        route == MainRoutes.CURRENT || route.startsWith("${MainRoutes.CURRENT}/") ->
            MainRoutes.CURRENT
        route == MainRoutes.NEW_TRIP || route.startsWith(MainRoutes.NEW_TRIP) || route == MainRoutes.AI_TRIP_CHAT ->
            MainRoutes.NEW_TRIP
        else -> route
    }
}

@Composable
fun MainScaffold(modifier: Modifier = Modifier, onLogout: () -> Unit = {}) {
    val newTripViewModel: NewTripViewModel = viewModel()
    val currentTripViewModel: CurrentTripViewModel = viewModel()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.HOME
    val currentTopLevelRoute = selectedBottomRoute(currentRoute)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1),
        containerColor = DeepSea1,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomNav(currentRoute)) {
                MainBottomNavBar(
                    selectedRoute = currentTopLevelRoute,
                    onItemSelected = { route ->
                        if (route == currentTopLevelRoute) return@MainBottomNavBar
                        if (route == MainRoutes.HOME) {
                            navController.popBackStack(MainRoutes.HOME, inclusive = false)
                        } else {
                            navController.navigate(route) {
                                popUpTo(MainRoutes.HOME) {
                                    inclusive = false
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainRoutes.CURRENT) {
                    LaunchedEffect(Unit) {
                        if (currentTripViewModel.uiState.value.currentTripId == null) {
                            currentTripViewModel.loadTrip()
                        }
                        navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                            popUpTo(MainRoutes.CURRENT) { inclusive = true }
                        }
                    }
                }

                composable(MainRoutes.CURRENT_ITINERARY) {
                    CurrentTripScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = currentTripViewModel,
                        displayMode = CurrentDisplayMode.ITINERARY,
                        autoLoadTrip = false,
                        onNavigateToMode = { mode ->
                            navController.navigate(CurrentTripRoutes.routeFor(mode)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(MainRoutes.CURRENT_DAY) {
                    CurrentTripScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = currentTripViewModel,
                        displayMode = CurrentDisplayMode.DAY,
                        autoLoadTrip = false,
                        onNavigateToMode = { mode ->
                            navController.navigate(CurrentTripRoutes.routeFor(mode)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(MainRoutes.CURRENT_WEEK) {
                    CurrentTripScreen(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = currentTripViewModel,
                        displayMode = CurrentDisplayMode.WEEK,
                        autoLoadTrip = false,
                        onNavigateToMode = { mode ->
                            navController.navigate(CurrentTripRoutes.routeFor(mode)) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(MainRoutes.NEW_TRIP) {
                    NewTripLandingPage(
                        modifier = Modifier.fillMaxSize(),
                        onPlanTripClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_1) },
                        onAiChatClick = { navController.navigate(MainRoutes.AI_TRIP_CHAT) }
                    )
                }
                composable(MainRoutes.NEW_TRIP_STEP_1) {
                    TripStep1DestinationPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onBackClick = { navController.popBackStack() },
                        onCloseClick = { navController.popBackStack(MainRoutes.NEW_TRIP, false) },
                        onContinueClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_2) }
                    )
                }
                composable(MainRoutes.NEW_TRIP_STEP_2) {
                    TripStep2DatesPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onBackClick = { navController.popBackStack() },
                        onCloseClick = { navController.popBackStack(MainRoutes.NEW_TRIP, false) },
                        onContinueClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_3) }
                    )
                }
                composable(MainRoutes.NEW_TRIP_STEP_3) {
                    TripStep3TravelersPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onBackClick = { navController.popBackStack() },
                        onCloseClick = { navController.popBackStack(MainRoutes.NEW_TRIP, false) },
                        onContinueClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_4) }
                    )
                }
                composable(MainRoutes.NEW_TRIP_STEP_4) {
                    TripStep4BudgetPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onBackClick = { navController.popBackStack() },
                        onCloseClick = { navController.popBackStack(MainRoutes.NEW_TRIP, false) },
                        onContinueClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_5) }
                    )
                }
                composable(MainRoutes.NEW_TRIP_STEP_5) {
                    TripStep5InterestsPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onBackClick = { navController.popBackStack() },
                        onCloseClick = { navController.popBackStack(MainRoutes.NEW_TRIP, false) },
                        onTripGenerated = {
                            navController.navigate(MainRoutes.TRIP_GENERATING)
                        }
                    )
                }
                composable(MainRoutes.TRIP_GENERATING) {
                    TripGeneratingPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onTripReady = {
                            currentTripViewModel.loadTrip()
                            navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                                popUpTo(MainRoutes.HOME) { inclusive = false }
                            }
                        }
                    )
                }
                composable(MainRoutes.HOME) {
                    HomePage(
                        modifier = Modifier.fillMaxSize(),
                        onTripClick = { tripKey ->
                            currentTripViewModel.loadTrip(tripKey)
                            navController.navigate(MainRoutes.CURRENT_ITINERARY)
                        },
                        onProfileClick = {
                            navController.navigate(MainRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(MainRoutes.CHATS) {
                    ChatsScreen(
                        modifier = Modifier.fillMaxSize(),
                        onTripCardClick = { tripId, ownerUid ->
                            currentTripViewModel.loadTrip(TripKey(ownerUid = ownerUid, tripId = tripId))
                            navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    SettingsPage(
                        modifier = Modifier.fillMaxSize(),
                        onLoggedOut = onLogout
                    )
                }
                composable(MainRoutes.AI_TRIP_CHAT) {
                    AiTripChatPage(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

