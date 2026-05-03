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
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.travelcents.MainActivity
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.social.model.DirectChatPreview
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.trip.model.Event
import com.example.travelcents.notification.ChatNotificationTarget
import com.example.travelcents.notification.NotificationHelper
import com.example.travelcents.ui.components.MainBottomNavBar
import com.example.travelcents.ui.main.aichat.AiTripChatPage
import com.example.travelcents.ui.main.chats.chat.*
import com.example.travelcents.ui.main.chats.friends.*
import com.google.firebase.auth.FirebaseAuth
import com.example.travelcents.ui.main.chats.groups.*
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
import com.example.travelcents.ui.main.settings.SettingsPage
import com.example.travelcents.ui.theme.DeepSea1
import com.google.firebase.firestore.FirebaseFirestore

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
    val context = LocalContext.current
    val activity = context as? MainActivity

    val groupsRepository = remember { com.example.travelcents.data.social.repository.GroupsRepository() }
    val socialUserRepository = remember { com.example.travelcents.data.social.repository.SocialUserRepository() }

    var pendingPreview by remember { mutableStateOf<PreviewSource.CuratedStarter?>(null) }

    // Chat navigation states
    var selectedGroup   by remember { mutableStateOf<Group?>(null) }
    var showNewTrip     by remember { mutableStateOf(false) }
    var showFriends     by remember { mutableStateOf(false) }
    var selectedFriend  by remember { mutableStateOf<Friend?>(null) }
    var showAddFriend   by remember { mutableStateOf(false) }
    var showRequests    by remember { mutableStateOf(false) }
    var selectedDM      by remember { mutableStateOf<DirectChatPreview?>(null) }
    var selectedDirectChatId by remember { mutableStateOf<String?>(null) }
    var activeTab       by remember { mutableIntStateOf(0) }
    var showEvents      by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var selectedEvent   by remember { mutableStateOf<Event?>(null) }
    var showEditChat    by remember { mutableStateOf(false) }

    fun resetChatDrillIn() {
        selectedGroup = null
        selectedFriend = null
        selectedDM = null
        selectedDirectChatId = null
        showNewTrip = false
        showFriends = false
        showAddFriend = false
        showRequests = false
        showEvents = false
        showCreateEvent = false
        selectedEvent = null
        showEditChat = false
    }

    // Observe navigation requests from notifications
    val pendingChatTarget by activity?.pendingChatTarget?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pendingChatTarget) {
        val target = pendingChatTarget ?: return@LaunchedEffect
        activity?.clearPendingChatTarget()
        resetChatDrillIn()

        navController.navigate(MainRoutes.CHATS) {
            launchSingleTop = true
        }

        when (target.chatType) {
            ChatNotificationTarget.TYPE_GROUP -> {
                activeTab = 0
                groupsRepository.fetchGroup(target.chatId) { group ->
                    if (group != null) {
                        selectedGroup = group
                    }
                }
            }
            ChatNotificationTarget.TYPE_DIRECT -> {
                activeTab = 1
                FirebaseFirestore.getInstance()
                    .collection("directChats")
                    .document(target.chatId)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val members = (doc.get("members") as? List<*>)
                                ?.filterIsInstance<String>()
                                .orEmpty()
                            val otherUid = members.firstOrNull {
                                it != FirebaseAuth.getInstance().currentUser?.uid
                            }
                            if (otherUid != null) {
                                socialUserRepository.fetchUserDisplayName(otherUid) { name ->
                                    selectedDirectChatId = target.chatId
                                    selectedDM = DirectChatPreview(
                                        id = target.chatId,
                                        otherUid = otherUid,
                                        otherUserName = name
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.HOME
    val currentTopLevelRoute = selectedBottomRoute(currentRoute)

    LaunchedEffect(currentRoute, selectedGroup?.id, selectedFriend?.uid, selectedDM?.id, selectedDirectChatId) {
        NotificationHelper.activeChatTarget = when {
            currentRoute != MainRoutes.CHATS -> null
            selectedGroup != null -> ChatNotificationTarget(
                chatType = ChatNotificationTarget.TYPE_GROUP,
                chatId = selectedGroup!!.id
            )
            (selectedFriend != null || selectedDM != null) && !selectedDirectChatId.isNullOrBlank() ->
                ChatNotificationTarget(
                    chatType = ChatNotificationTarget.TYPE_DIRECT,
                    chatId = selectedDirectChatId!!
                )
            else -> null
        }
    }

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
                        }
                    )
                }
                composable(MainRoutes.SAVED_PLACES) {
                    SavedPlacesPage(onBack = { navController.popBackStack() })
                }
                composable(MainRoutes.CHATS) {
                    val onTripCardClick: (String, String) -> Unit = { tripId, ownerUid ->
                        currentTripViewModel.loadTrip(TripKey(ownerUid = ownerUid, tripId = tripId))
                        navController.navigate(MainRoutes.CURRENT_ITINERARY) {
                            launchSingleTop = true
                        }
                    }

                    when {
                        selectedGroup != null && selectedEvent != null ->
                            com.example.travelcents.ui.main.chats.voting.EventCommentsPage(
                                event       = selectedEvent!!,
                                groupId     = selectedGroup!!.id,
                                onBackClick = { selectedEvent = null }
                            )

                        selectedGroup != null && showCreateEvent ->
                            com.example.travelcents.ui.main.chats.voting.CreateEventPage(
                                group          = selectedGroup!!,
                                onBackClick    = { showCreateEvent = false },
                                onEventCreated = { showCreateEvent = false }
                            )

                        selectedGroup != null && showEvents ->
                            com.example.travelcents.ui.main.chats.voting.EventsPage(
                                group        = selectedGroup!!,
                                onBackClick  = { showEvents = false },
                                onNewEvent   = { showCreateEvent = true },
                                onEventClick = { event -> selectedEvent = event }
                            )

                        selectedGroup != null && showEditChat ->
                            EditChatPage(
                                group             = selectedGroup!!,
                                onBackClick       = { showEditChat = false },
                                onNavigateToChats = {
                                    showEditChat = false
                                    selectedGroup = null
                                }
                            )

                        selectedGroup != null -> ChatPage(
                            group           = selectedGroup!!,
                            onBackClick     = { selectedGroup = null },
                            onEventsClick   = { showEvents = true },
                            onEditClick     = { showEditChat = true },
                            onTripCardClick = onTripCardClick
                        )

                        selectedFriend != null -> DirectChatPage(
                            friend      = selectedFriend!!,
                            onBackClick = {
                                selectedFriend = null
                                selectedDirectChatId = null
                            },
                            onTripCardClick = onTripCardClick,
                            onChatResolved = { selectedDirectChatId = it }
                        )
                        selectedDM != null -> DirectChatPage(
                            friend      = Friend(uid = selectedDM!!.otherUid, displayName = selectedDM!!.otherUserName),
                            onBackClick = {
                                selectedDM = null
                                selectedDirectChatId = null
                                activeTab = 1
                            },
                            onTripCardClick = onTripCardClick,
                            onChatResolved = { selectedDirectChatId = it }
                        )
                        showAddFriend -> AddFriendPage(onBackClick = { showAddFriend = false })
                        showRequests  -> FriendRequestsPage(onBackClick = { showRequests = false })
                        showNewTrip   -> NewTripChatPage(
                            onBackClick   = { showNewTrip = false },
                            onTripCreated = { newGroup ->
                                showNewTrip = false
                                selectedDirectChatId = null
                                selectedGroup = newGroup
                            }
                        )
                        showFriends -> FriendsPage(
                            onBackClick          = { showFriends = false },
                            onMessageFriendClick = { friend ->
                                showFriends = false
                                selectedDirectChatId = null
                                selectedFriend = friend
                            },
                            onAddFriendClick     = { showAddFriend = true },
                            onRequestsClick      = { showRequests = true }
                        )
                        else -> ChatsPage(
                            modifier          = Modifier.fillMaxSize(),
                            startTab          = activeTab,
                            onNewChatClick    = { showNewTrip = true },
                            onFriendsClick    = { showFriends = true },
                            onGroupClick      = { group ->
                                selectedDirectChatId = null
                                selectedGroup = group
                                activeTab = 0
                            },
                            onDirectChatClick = { dm ->
                                selectedDirectChatId = dm.id
                                selectedDM = dm
                                activeTab = 1
                            }
                        )
                    }
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
                        onCreateDraftTrip = { starter, intakeProfile ->
                            val source = PreviewSource.CuratedStarter(
                                starter = starter,
                                intakeProfile = intakeProfile
                            )
                            pendingPreview = source
                            currentTripViewModel.loadPreview(source)
                            navController.navigate(MainRoutes.CURRENT_TRIP_PREVIEW) {
                                launchSingleTop = true
                            }
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
                        if (previewSource != null) {
                            PreviewActionBar(
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
                        }
                    }
                }
            }
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
