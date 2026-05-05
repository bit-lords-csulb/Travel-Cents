package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiDestinationRecommendation
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.ui.components.MainBottomNavBar
import com.example.travelcents.ui.main.aichat.AiTripChatPage
import com.example.travelcents.ui.main.chats.chat.ChatsScreen
import com.example.travelcents.ui.main.current.CurrentDisplayMode
import com.example.travelcents.ui.main.current.CurrentTripRoutes
import com.example.travelcents.ui.main.current.CurrentTripScreen
import com.example.travelcents.ui.main.current.CurrentTripViewModel
import com.example.travelcents.ui.main.home.HomePage
import com.example.travelcents.ui.main.home.SavedPlacesPage
import com.example.travelcents.ui.main.current.PreviewSource
import com.example.travelcents.ui.main.newTrip.NewTripLandingPage
import com.example.travelcents.ui.main.newTrip.NewTripViewModel
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.main.newTrip.TripGeneratingPage
import com.example.travelcents.ui.main.newTrip.TripStep1DestinationPage
import com.example.travelcents.ui.main.newTrip.TripStep2DatesPage
import com.example.travelcents.ui.main.newTrip.TripStep3TravelersPage
import com.example.travelcents.ui.main.newTrip.TripStep4BudgetPage
import com.example.travelcents.ui.main.newTrip.TripStep5InterestsPage
import com.example.travelcents.ui.main.passes.MyPassesScreen
import com.example.travelcents.ui.main.settings.SettingsPage
import com.example.travelcents.ui.theme.DeepSea1

object MainRoutes {
    const val CURRENT = CurrentTripRoutes.ROOT
    const val CURRENT_ITINERARY = CurrentTripRoutes.ITINERARY
    const val CURRENT_DAY = CurrentTripRoutes.DAY
    const val CURRENT_WEEK = CurrentTripRoutes.WEEK
    const val CURRENT_TRIP_PREVIEW = "current_trip_preview"
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
    const val SAVED_PLACES = "saved_places"
    const val DOCUMENTS = "documents"
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
    var pendingPreview by remember { mutableStateOf<PreviewSource?>(null) }

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
                        },
                        onSavedPlacesClick = {
                            navController.navigate(MainRoutes.SAVED_PLACES)
                        },
                        onDocumentsClick = {
                            navController.navigate(MainRoutes.DOCUMENTS)
                        }
                    )
                }
                composable(MainRoutes.SAVED_PLACES) {
                    SavedPlacesPage(onBack = { navController.popBackStack() })
                }
                composable(MainRoutes.DOCUMENTS) {
                    MyPassesScreen(onBack = { navController.popBackStack() })
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
                        onBackClick = { navController.popBackStack() },
                        onOpenTrip = { tripKey ->
                            currentTripViewModel.loadTrip(tripKey)
                            navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                                launchSingleTop = true
                            }
                        },
                        onOpenPreviewTrip = {
                            // Re-load ensures the skeleton survives clearPreview() on back-press
                            pendingPreview?.let { currentTripViewModel.loadPreview(it) }
                            navController.navigate(MainRoutes.CURRENT_TRIP_PREVIEW) {
                                launchSingleTop = true
                            }
                        },
                        onStarterSelected = { starter, intakeProfile ->
                            val source = PreviewSource.CuratedStarter(
                                starter = starter,
                                intakeProfile = intakeProfile
                            )
                            pendingPreview = source
                            currentTripViewModel.loadPreview(source)
                            // Stay in chat — the banner is the user's path to the curated preview
                        },
                        onDestinationLocked = { recommendation, intakeProfile ->
                            val source = PreviewSource.DestinationLock(
                                destination = recommendation.destination,
                                intakeProfile = intakeProfile
                            )
                            pendingPreview = source
                            currentTripViewModel.loadPreview(source)
                            // Stay in chat — the banner is the user's path to the skeleton
                        },
                        onAddEventToPreview = { event ->
                            currentTripViewModel.addPreviewEvent(event)
                        }
                    )
                }
                composable(MainRoutes.CURRENT_TRIP_PREVIEW) {
                    val previewSource = pendingPreview
                    DisposableEffect(Unit) {
                        onDispose { currentTripViewModel.clearPreview() }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
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
                        when (previewSource) {
                            is PreviewSource.CuratedStarter -> PreviewActionBar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                onDiscard = {
                                    pendingPreview = null
                                    navController.popBackStack()
                                },
                                onCommit = {
                                    val toCommit = previewSource
                                    pendingPreview = null
                                    newTripViewModel.commitItinerary(
                                        starter = toCommit.starter,
                                        intakeProfile = toCommit.intakeProfile,
                                        onTripReady = { tripKey ->
                                            currentTripViewModel.loadTrip(tripKey)
                                            navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                                                popUpTo(MainRoutes.HOME) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            )

                            is PreviewSource.DestinationLock -> SkeletonPreviewBar(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                onBack = {
                                    navController.popBackStack()
                                }
                            )

                            null -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonPreviewBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TripWizardColors.ContainerLow)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Draft trip — keep chatting to fill it in.",
            color = DeepSea5.copy(alpha = 0.78f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "← Back to Chat")
        }
    }
}

@Composable
private fun PreviewActionBar(
    onDiscard: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TripWizardColors.ContainerLow)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Preview only — nothing is saved yet.",
            color = DeepSea5.copy(alpha = 0.78f),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Discard")
            }
            Button(
                onClick = onCommit,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripWizardColors.Blue,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Use this trip")
            }
        }
    }
}
