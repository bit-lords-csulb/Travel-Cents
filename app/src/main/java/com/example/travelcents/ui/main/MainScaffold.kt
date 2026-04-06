package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.travelcents.ui.main.itinerary.EditPlanScreen
import com.example.travelcents.ui.main.itinerary.FinalPlanPage
import com.example.travelcents.ui.main.itinerary.ItineraryScreen
import com.example.travelcents.ui.main.itinerary.ItineraryViewModel
import com.example.travelcents.ui.main.newtrip.NewTripLandingPage
import com.example.travelcents.ui.main.newtrip.NewTripViewModel
import com.example.travelcents.ui.main.newtrip.TripGeneratingPage
import com.example.travelcents.ui.main.newtrip.TripStep1DestinationPage
import com.example.travelcents.ui.main.newtrip.TripStep2DatesPage
import com.example.travelcents.ui.main.newtrip.TripStep3TravelersPage
import com.example.travelcents.ui.main.newtrip.TripStep4BudgetPage
import com.example.travelcents.ui.main.newtrip.TripStep5InterestsPage
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

object MainRoutes {
    const val CURRENT = "current"
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

    const val EDIT_PLAN = "edit_plan/{tripId}/{eventId}"
    const val AI_TRIP_CHAT = "ai_trip_chat"
    const val FINAL_PLAN = "final_plan"
}

private val bottomNavRoutes = setOf(
    MainRoutes.CURRENT,
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
    MainRoutes.SETTINGS,
    MainRoutes.EDIT_PLAN,
    MainRoutes.FINAL_PLAN
)

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(modifier: Modifier = Modifier, onLogout: () -> Unit = {}) {
    val newTripViewModel: NewTripViewModel = viewModel()
    val sharedItineraryViewModel: ItineraryViewModel = viewModel()
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.HOME
    val selectedBottomRoute = when (currentRoute) {
        MainRoutes.EDIT_PLAN -> MainRoutes.CURRENT
        MainRoutes.NEW_TRIP,
        MainRoutes.NEW_TRIP_STEP_1,
        MainRoutes.NEW_TRIP_STEP_2,
        MainRoutes.NEW_TRIP_STEP_3,
        MainRoutes.NEW_TRIP_STEP_4,
        MainRoutes.NEW_TRIP_STEP_5,
        MainRoutes.TRIP_GENERATING,
        MainRoutes.AI_TRIP_CHAT,
        MainRoutes.FINAL_PLAN -> MainRoutes.NEW_TRIP
        else -> currentRoute
    }
    val itineraryUiState by sharedItineraryViewModel.uiState.collectAsState()

    // Load trip once on mount so currentTripId is available before any tab is visited
    LaunchedEffect(Unit) { sharedItineraryViewModel.loadTrip() }

    val items = listOf(
        BottomNavItem(MainRoutes.CURRENT, "CURRENT", Icons.Outlined.CalendarToday),
        BottomNavItem(MainRoutes.NEW_TRIP, "NEW TRIP", Icons.Outlined.AutoAwesome),
        BottomNavItem(MainRoutes.HOME, "HOME", Icons.Outlined.Home),
        BottomNavItem(MainRoutes.CHATS, "CHATS", Icons.Outlined.ChatBubbleOutline),
        BottomNavItem(MainRoutes.SETTINGS, "SETTINGS", Icons.Outlined.Settings)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainRoutes.CURRENT) {
                    LaunchedEffect(Unit) { sharedItineraryViewModel.loadTrip() }
                    ItineraryScreen(
                        viewModel = sharedItineraryViewModel,
                        onEditEventClick = { clickedEventId ->
                            itineraryUiState.currentTripId?.let { tripId ->
                                navController.navigate("edit_plan/$tripId/$clickedEventId")
                            }
                        },
                        onAddEventClick = {
                            itineraryUiState.currentTripId?.let { tripId ->
                                navController.navigate("edit_plan/$tripId/new")
                            }
                        }
                    )
                }

                composable(
                    route = MainRoutes.EDIT_PLAN,
                    arguments = listOf(
                        navArgument("tripId") { type = NavType.StringType },
                        navArgument("eventId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId")
                    val eventId = backStackEntry.arguments?.getString("eventId")

                    EditPlanScreen(
                        tripId = tripId,
                        eventId = eventId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(MainRoutes.NEW_TRIP) {
                    NewTripLandingPage(
                        modifier = Modifier.fillMaxSize(),
                        onPlanTripClick = { navController.navigate(MainRoutes.NEW_TRIP_STEP_1) },
                        onAiChatClick = { navController.navigate(MainRoutes.AI_TRIP_CHAT) },
                        onViewLastTripClick = if (itineraryUiState.currentTripId != null) {
                            { navController.navigate(MainRoutes.FINAL_PLAN) }
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
                            navController.navigate(MainRoutes.FINAL_PLAN) {
                                popUpTo(MainRoutes.HOME) { inclusive = false }
                            }
                        }
                    )
                }
                composable(MainRoutes.HOME) { HomePage(modifier = Modifier.fillMaxSize()) }
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
                    LaunchedEffect(Unit) { sharedItineraryViewModel.loadTrip() }
                    FinalPlanPage(
                        viewModel = sharedItineraryViewModel,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = {
                            navController.navigate(MainRoutes.CURRENT) {
                                popUpTo(MainRoutes.HOME) { inclusive = false }
                            }
                        }
                    )
                }
            }
        }

        if (currentRoute in bottomNavRoutes) {
            BottomNavBar(
                items = items,
                currentRoute = selectedBottomRoute,
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
}

@Composable
private fun BottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemSelected: (String) -> Unit
) {
    Column {
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
                val selected = currentRoute == item.route
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
