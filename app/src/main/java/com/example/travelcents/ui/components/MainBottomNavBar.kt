package com.example.travelcents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.main.MainRoutes
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

private data class MainBottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainBottomNavItems = listOf(
    MainBottomNavItem(MainRoutes.CURRENT, "CURRENT", Icons.Outlined.CalendarToday),
    MainBottomNavItem(MainRoutes.NEW_TRIP, "NEW TRIP", Icons.Outlined.AutoAwesome),
    MainBottomNavItem(MainRoutes.HOME, "HOME", Icons.Outlined.Home),
    MainBottomNavItem(MainRoutes.CHATS, "CHATS", Icons.Outlined.ChatBubbleOutline),
    MainBottomNavItem(MainRoutes.SETTINGS, "SETTINGS", Icons.Outlined.Settings)
)

@Composable
fun MainBottomNavBar(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(DeepSea2)
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
            mainBottomNavItems.forEach { item ->
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

