package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

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
    const val FINAL_PLAN = "final_plan"
    const val FINAL_PLAN_BY_ID = "final_plan/{tripId}"
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
        route.startsWith(MainRoutes.NEW_TRIP) ||
        route.startsWith("final_plan")
}

private fun selectedBottomRoute(route: String): String {
    return when {
        route == MainRoutes.CURRENT || route.startsWith("${MainRoutes.CURRENT}/") || route.startsWith("final_plan") ->
            MainRoutes.CURRENT
        route == MainRoutes.NEW_TRIP || route.startsWith(MainRoutes.NEW_TRIP) || route == MainRoutes.AI_TRIP_CHAT ->
            MainRoutes.NEW_TRIP
        else -> route
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(modifier: Modifier = Modifier, onLogout: () -> Unit = {}) {
    val newTripViewModel: NewTripViewModel = viewModel()
    val currentTripViewModel: CurrentTripViewModel = viewModel()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.HOME
    val currentTopLevelRoute = selectedBottomRoute(currentRoute)
    val itineraryUiState by currentTripViewModel.uiState.collectAsState()

    // Load trip once on mount so currentTripId is available before any tab is visited
    LaunchedEffect(Unit) { currentTripViewModel.loadTrip() }

    val items = listOf(
        BottomNavItem(MainRoutes.CURRENT, "CURRENT", Icons.Outlined.CalendarToday),
        BottomNavItem(MainRoutes.NEW_TRIP, "NEW TRIP", Icons.Outlined.AutoAwesome),
        BottomNavItem(MainRoutes.HOME, "HOME", Icons.Outlined.Home),
        BottomNavItem(MainRoutes.CHATS, "CHATS", Icons.Outlined.ChatBubbleOutline),
        BottomNavItem(MainRoutes.SETTINGS, "SETTINGS", Icons.Outlined.Settings)
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1),
        containerColor = DeepSea1,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shouldShowBottomNav(currentRoute)) {
                BottomNavBar(
                    items = items,
                    selectedRoute = currentTopLevelRoute,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
                        onAiChatClick = { navController.navigate(MainRoutes.AI_TRIP_CHAT) },
                        onViewLastTripClick = if (itineraryUiState.currentTripId != null) {
                            { navController.navigate(MainRoutes.CURRENT_ITINERARY) }
                        } else null
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
                            navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                                popUpTo(MainRoutes.HOME) { inclusive = false }
                            }
                        }
                    )
                }
                composable(MainRoutes.HOME) {
                    HomePage(
                        modifier = Modifier.fillMaxSize(),
                        onTripClick = { tripId ->
                            currentTripViewModel.loadTrip(tripId)
                            navController.navigate(MainRoutes.CURRENT_ITINERARY)
                        },
                        onProfileClick = {
                            navController.navigate(MainRoutes.SETTINGS) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(MainRoutes.CHATS) { ChatsScreen(modifier = Modifier.fillMaxSize()) }
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
                composable(MainRoutes.FINAL_PLAN) {
                    LaunchedEffect(Unit) {
                        currentTripViewModel.loadTrip()
                        navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                            popUpTo(MainRoutes.HOME) { inclusive = false }
                        }
                    }
                }
                composable(
                    route = MainRoutes.FINAL_PLAN_BY_ID,
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId")
                    LaunchedEffect(tripId) {
                        currentTripViewModel.loadTrip(tripId)
                        navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                            popUpTo(MainRoutes.HOME) { inclusive = false }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSea2)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = DeepSea3, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(DeepSea2)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!selected) {
                                onItemSelected(item.route)
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (selected) DeepSea3 else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) DeepSea5 else DeepSea4
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        color = if (selected) DeepSea5 else DeepSea4,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }
    }
}
