package com.example.travelcents.ui.main.chats.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.social.model.DirectChatPreview
import com.example.travelcents.data.social.model.Friend
import com.example.travelcents.data.social.model.Group
import com.example.travelcents.data.trip.model.Event
import com.example.travelcents.ui.main.chats.friends.AddFriendPage
import com.example.travelcents.ui.main.chats.friends.FriendRequestsPage
import com.example.travelcents.ui.main.chats.friends.FriendsPage
import com.example.travelcents.ui.main.chats.groups.NewTripChatPage
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val now    = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = timestamp.toDate() }
    return when {
        now.get(Calendar.DATE) == msgCal.get(Calendar.DATE) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
        now.get(Calendar.DATE) - msgCal.get(Calendar.DATE) == 1 -> "Yesterday"
        now.get(Calendar.WEEK_OF_YEAR) == msgCal.get(Calendar.WEEK_OF_YEAR) ->
            SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp.toDate())
        else -> "Last week"
    }
}

@Composable
fun ChatsPage(
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel = viewModel(),
    onNewChatClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    onGroupClick: (Group) -> Unit = {},
    onDirectChatClick: (DirectChatPreview) -> Unit = {},
    startTab: Int = 0
) {
    val searchQuery       by viewModel.searchQuery.collectAsState()
    val filteredGroups    by viewModel.filteredGroupsSearch.collectAsState()
    val filteredDMs       by viewModel.filteredDirectChats.collectAsState()
    var selectedTab       by remember { mutableIntStateOf(startTab) }
    val tabs               = listOf("Groups", "Direct Messages")

    Column(modifier = modifier.fillMaxSize().background(DeepSea1)) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(DeepSea2)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chats", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepSea3).clickable { onFriendsClick() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Group, contentDescription = "Friends", tint = DeepSea5) }
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(DeepSea3).clickable { onNewChatClick() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, contentDescription = "New Chat", tint = DeepSea5) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(48.dp)),
                    placeholder = { Text("Search conversations...", color = DeepSea5.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DeepSea5.copy(alpha = 0.7f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeepSea3, unfocusedContainerColor = DeepSea3,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = DeepSea5, focusedTextColor = DeepSea5, unfocusedTextColor = DeepSea5
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(48.dp))
                                .background(if (isSelected) DeepSea4 else DeepSea3)
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) DeepSea1 else DeepSea5.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Groups tab
                if (filteredGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No group chats yet" else "No results found",
                            color = DeepSea5.copy(alpha = 0.4f), fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredGroups, key = { it.id }) { group ->
                            GroupChatRow(group = group, onClick = { onGroupClick(group) })
                        }
                    }
                }
            }
            1 -> {
                // DMs tab
                if (filteredDMs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "No direct messages yet" else "No results found",
                            color = DeepSea5.copy(alpha = 0.4f), fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredDMs, key = { it.id }) { dm ->
                            DirectChatRow(dm = dm, onClick = { onDirectChatClick(dm) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupChatRow(group: Group, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DeepSea2).clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(DeepSea3),
            contentAlignment = Alignment.Center
        ) {
            Text(group.name.take(2).uppercase(), color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(group.lastMessage, color = DeepSea5.copy(alpha = 0.55f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(formatTimestamp(group.lastMessageTime), color = DeepSea5.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

@Composable
fun DirectChatRow(dm: DirectChatPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DeepSea2).clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(DeepSea3),
            contentAlignment = Alignment.Center
        ) {
            Text(dm.otherUserName.take(2).uppercase(), color = DeepSea5, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dm.otherUserName, color = DeepSea5, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(3.dp))
            Text(dm.lastMessage, color = DeepSea5.copy(alpha = 0.55f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(formatTimestamp(dm.lastMessageTime), color = DeepSea5.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

// Nav Wrapper
@Composable
fun ChatsScreen(
    modifier: Modifier = Modifier,
    onTripCardClick: (String, String) -> Unit = { _, _ -> }
) {
    var selectedGroup   by remember { mutableStateOf<Group?>(null) }
    var showNewTrip     by remember { mutableStateOf(false) }
    var showFriends     by remember { mutableStateOf(false) }
    var selectedFriend  by remember { mutableStateOf<Friend?>(null) }
    var showAddFriend   by remember { mutableStateOf(false) }
    var showRequests    by remember { mutableStateOf(false) }
    var selectedDM      by remember { mutableStateOf<DirectChatPreview?>(null) }
    var activeTab       by remember { mutableIntStateOf(0) }
    var showEvents      by remember { mutableStateOf(false) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    when {
        // Viewing a specific event's comments
        selectedGroup != null && selectedEvent != null ->
            com.example.travelcents.ui.main.chats.voting.EventCommentsPage(
                event       = selectedEvent!!,
                groupId     = selectedGroup!!.id,
                onBackClick = { selectedEvent = null }
            )

        // Creating a new event
        selectedGroup != null && showCreateEvent ->
            com.example.travelcents.ui.main.chats.voting.CreateEventPage(
                group          = selectedGroup!!,
                onBackClick    = { showCreateEvent = false },
                onEventCreated = { showCreateEvent = false }
            )

        // Viewing the events list
        selectedGroup != null && showEvents ->
            com.example.travelcents.ui.main.chats.voting.EventsPage(
                group        = selectedGroup!!,
                onBackClick  = { showEvents = false },
                onNewEvent   = { showCreateEvent = true },
                onEventClick = { event -> selectedEvent = event }
            )

        // Viewing the group chat
        selectedGroup != null -> ChatPage(
            group         = selectedGroup!!,
            onBackClick   = { selectedGroup = null },
            onEventsClick = { showEvents = true },
            onTripCardClick = onTripCardClick
        )

        // DMs and Friends
        selectedFriend != null -> DirectChatPage(
            friend      = selectedFriend!!,
            onBackClick = { selectedFriend = null },
            onTripCardClick = onTripCardClick
        )
        selectedDM != null -> DirectChatPage(
            friend      = Friend(uid = selectedDM!!.otherUid, displayName = selectedDM!!.otherUserName),
            onBackClick = { selectedDM = null; activeTab = 1 },
            onTripCardClick = onTripCardClick
        )
        showAddFriend -> AddFriendPage(onBackClick = { showAddFriend = false })
        showRequests  -> FriendRequestsPage(onBackClick = { showRequests = false })
        showNewTrip   -> NewTripChatPage(
            onBackClick   = { showNewTrip = false },
            onTripCreated = { newGroup -> showNewTrip = false; selectedGroup = newGroup }
        )
        showFriends -> FriendsPage(
            onBackClick          = { showFriends = false },
            onMessageFriendClick = { friend -> showFriends = false; selectedFriend = friend },
            onAddFriendClick     = { showAddFriend = true },
            onRequestsClick      = { showRequests = true }
        )
        else -> ChatsPage(
            modifier          = modifier,
            startTab          = activeTab,
            onNewChatClick    = { showNewTrip = true },
            onFriendsClick    = { showFriends = true },
            onGroupClick      = { group -> selectedGroup = group; activeTab = 0 },
            onDirectChatClick = { dm -> selectedDM = dm; activeTab = 1 }
        )
    }
}

