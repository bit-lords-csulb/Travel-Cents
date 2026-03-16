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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.travelcents.ui.main.chats.ChatsScreen
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

object MainRoutes {
    const val Current = "current"
    const val NewTrip = "new_trip"
    const val Home = "home"
    const val Chats = "chats"
    const val Settings = "settings"
    const val AiTripChat = "ai_trip_chat"
}

private val bottomNavRoutes = setOf(
    MainRoutes.Current,
    MainRoutes.NewTrip,
    MainRoutes.Home,
    MainRoutes.Chats,
    MainRoutes.Settings
)

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val newTripViewModel: NewTripViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: MainRoutes.Home

    val items = listOf(
        BottomNavItem(MainRoutes.Current, "CURRENT", Icons.Outlined.CalendarToday),
        BottomNavItem(MainRoutes.NewTrip, "NEW TRIP", Icons.Outlined.AutoAwesome),
        BottomNavItem(MainRoutes.Home, "HOME", Icons.Outlined.Home),
        BottomNavItem(MainRoutes.Chats, "CHATS", Icons.Outlined.ChatBubbleOutline),
        BottomNavItem(MainRoutes.Settings, "SETTINGS", Icons.Outlined.Settings)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = navController,
                startDestination = MainRoutes.Home,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(MainRoutes.Current) { ItineraryScreen() }
                composable(MainRoutes.NewTrip) {
                    NewTripPage(
                        modifier = Modifier.fillMaxSize(),
                        viewModel = newTripViewModel,
                        onChatClick = { navController.navigate(MainRoutes.AiTripChat) }
                    )
                }
                composable(MainRoutes.Home) { HomePage(modifier = Modifier.fillMaxSize()) }
                composable(MainRoutes.Chats) { ChatsScreen(modifier = Modifier.fillMaxSize()) }
                composable(MainRoutes.Settings) { SettingsPage(modifier = Modifier.fillMaxSize()) }
                composable(MainRoutes.AiTripChat) {
                    AiTripChatPage(
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        // Hide bottom nav on the AI chat screen
        if (currentRoute in bottomNavRoutes) {
            BottomNavBar(
                items = items,
                currentRoute = currentRoute,
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