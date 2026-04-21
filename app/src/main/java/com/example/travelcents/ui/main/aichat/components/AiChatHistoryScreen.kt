package com.example.travelcents.ui.main.aichat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.ai.chat.AiChatHistoryEntry
import com.example.travelcents.ui.components.TcCompactTextField
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AiChatHistoryScreen(
    entries: List<AiChatHistoryEntry>,
    onBackClick: () -> Unit,
    onSessionSelected: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var showHeaderMenu by remember { mutableStateOf(false) }
    val filteredEntries = remember(entries, query) {
        if (query.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                entry.title.contains(query, ignoreCase = true) ||
                    entry.snippet.contains(query, ignoreCase = true)
            }
        }
    }
    val groupedEntries = remember(filteredEntries) {
        filteredEntries.groupBy(::historyBucketLabel)
            .toSortedMap(compareBy(::historyBucketRank))
    }

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
            Surface(
                color = DeepSea1.copy(alpha = 0.92f),
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TripWizardColors.Blue
                        )
                    }
                    Text(
                        text = "Chat History",
                        color = DeepSea5,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TravelCentsFonts.Headline,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { showHeaderMenu = true },
                            enabled = entries.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = "History options",
                                tint = if (entries.isNotEmpty()) TripWizardColors.Blue else DeepSea4
                            )
                        }

                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Clear all history",
                                        fontFamily = TravelCentsFonts.Body
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null
                                    )
                                },
                                enabled = entries.isNotEmpty(),
                                onClick = {
                                    showHeaderMenu = false
                                    onClearAllHistory()
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TcCompactTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search conversations...",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = DeepSea4,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    textStyle = TextStyle(
                        color = DeepSea5,
                        fontSize = 14.sp,
                        fontFamily = TravelCentsFonts.Body
                    ),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = TripWizardColors.ContainerHighest,
                    placeholderColor = DeepSea4,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    )
                )

                if (filteredEntries.isEmpty()) {
                    EmptyHistoryState(
                        hasSearchQuery = query.isNotBlank()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        groupedEntries.forEach { (label, itemsForBucket) ->
                            item(key = "header_$label") {
                                Text(
                                    text = label,
                                    color = DeepSea4,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = TravelCentsFonts.Body
                                )
                            }
                            items(itemsForBucket, key = { it.sessionId }) { entry ->
                                HistoryRow(
                                    entry = entry,
                                    onClick = { onSessionSelected(entry.sessionId) },
                                    onDeleteClick = { onDeleteSession(entry.sessionId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: AiChatHistoryEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember(entry.sessionId) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = TripWizardColors.ContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.title,
                    color = DeepSea5,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = TravelCentsFonts.Headline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.snippet,
                    color = DeepSea4,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = TravelCentsFonts.Body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = historyTimestampLabel(entry.updatedAtEpochMs),
                    color = DeepSea4,
                    fontSize = 11.sp,
                    fontFamily = TravelCentsFonts.Body
                )

                Box {
                    Surface(
                        modifier = Modifier.clickable { showMenu = true },
                        color = TripWizardColors.ContainerHighest,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = "Conversation options",
                                tint = TripWizardColors.Blue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Open chat",
                                    fontFamily = TravelCentsFonts.Body
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete chat",
                                    fontFamily = TravelCentsFonts.Body
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    hasSearchQuery: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = TripWizardColors.ContainerHigh,
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = TripWizardColors.Blue,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                text = if (hasSearchQuery) "No conversations match that search." else "No saved AI conversations yet.",
                color = DeepSea5,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TravelCentsFonts.Headline
            )
            Text(
                text = if (hasSearchQuery) {
                    "Try a broader search."
                } else {
                    "Start planning a trip and your AI chats will appear here."
                },
                color = DeepSea4,
                fontSize = 13.sp,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}

private fun historyBucketLabel(entry: AiChatHistoryEntry): String {
    val zoneId = ZoneId.systemDefault()
    val entryDate = Instant.ofEpochMilli(entry.updatedAtEpochMs)
        .atZone(zoneId)
        .toLocalDate()
    val today = LocalDate.now(zoneId)

    return when {
        entryDate == today -> "Today"
        entryDate == today.minusDays(1) -> "Yesterday"
        entryDate.isAfter(today.minusDays(7)) -> "Last Week"
        else -> "Older"
    }
}

private fun historyBucketRank(label: String): Int {
    return when (label) {
        "Today" -> 0
        "Yesterday" -> 1
        "Last Week" -> 2
        else -> 3
    }
}

private fun historyTimestampLabel(timestamp: Long): String {
    val zoneId = ZoneId.systemDefault()
    val entryTime = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    val now = Instant.now().atZone(zoneId)

    return when {
        entryTime.toLocalDate() == now.toLocalDate() -> {
            val hours = java.time.Duration.between(entryTime, now).toHours()
            if (hours <= 0) "Just now" else "${hours}h ago"
        }
        entryTime.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
        else -> entryTime.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}
