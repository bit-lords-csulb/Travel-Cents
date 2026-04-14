package com.example.travelcents.ui.main.current.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.ui.main.current.TripMemberUi
import com.example.travelcents.ui.modules.formatDayOfWeekShort
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import androidx.compose.material.icons.outlined.Archive

private val PrimaryAccent = Color(0xFFCEBDFF)
private val SurfaceContainerHigh = Color(0xFF222A3D)

private fun formatHeroDate(dateFrom: String): String {
    val parts = dateFrom.split("-")
    if (parts.size < 3) return dateFrom.ifBlank { "—" }
    val month = when (parts[1].toIntOrNull()) {
        1 -> "JAN"; 2 -> "FEB"; 3 -> "MAR"; 4 -> "APR"
        5 -> "MAY"; 6 -> "JUN"; 7 -> "JUL"; 8 -> "AUG"
        9 -> "SEP"; 10 -> "OCT"; 11 -> "NOV"; 12 -> "DEC"
        else -> return dateFrom
    }
    val day = parts[2].toIntOrNull() ?: return dateFrom
    val suffix = when {
        day in 11..13 -> "TH"
        day % 10 == 1 -> "ST"
        day % 10 == 2 -> "ND"
        day % 10 == 3 -> "RD"
        else -> "TH"
    }
    return "$month $day$suffix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTripHeader(
    destination: String,
    dateFrom: String,
    currentTripId: String?,
    allTrips: List<Itinerary>,
    canAdd: Boolean,
    isReorderActive: Boolean,
    isInCalendarMode: Boolean,
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit,
    members: List<TripMemberUi>,
    onCalendarClick: () -> Unit,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleReorder: () -> Unit,
    onArchiveTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onSwitchTrip: (String) -> Unit,
    onRenameTrip: (String) -> Unit,
    controlsContent: @Composable () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var switcherExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }

    if (showDeleteDialog && currentTripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepSea2,
            title = { Text("Delete trip?", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This trip and all its events will be permanently deleted.",
                    color = DeepSea4,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTrip(currentTripId)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color(0xFFE77D90), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = DeepSea4)
                }
            }
        )
    }

    if (showMembersSheet && members.isNotEmpty()) {
        MemberListSheet(
            members = members,
            onDismiss = { showMembersSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        // Top app bar: left icon | destination (centered) | overflow menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Title centered in the full width
            Box {
                Text(
                    text = destination.uppercase().ifBlank { "MY TRIP" },
                    color = PrimaryAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable(enabled = allTrips.size > 1) { switcherExpanded = true }
                )
                DropdownMenu(
                    expanded = switcherExpanded,
                    onDismissRequest = { switcherExpanded = false },
                    modifier = Modifier
                        .background(DeepSea2)
                        .widthIn(min = 200.dp)
                ) {
                    allTrips.forEach { trip ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = trip.tripName,
                                    color = if (trip.itineraryId == currentTripId) DeepSea5 else DeepSea4,
                                    fontWeight = if (trip.itineraryId == currentTripId) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                switcherExpanded = false
                                if (trip.itineraryId != currentTripId) onSwitchTrip(trip.itineraryId)
                            }
                        )
                    }
                }
            }

            // Left: back arrow (calendar mode) or calendar icon (itinerary landing)
            IconButton(
                onClick = if (isInCalendarMode) onBackClick else onCalendarClick,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = if (isInCalendarMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.DateRange,
                    contentDescription = if (isInCalendarMode) "Back to itinerary" else "Calendar view",
                    tint = PrimaryAccent
                )
            }

            // Right: overflow menu
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Trip actions",
                        tint = DeepSea4
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DeepSea2)
                ) {
                    if (canAdd) {
                        DropdownMenuItem(
                            text = { Text("Add Event", color = DeepSea5, fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onAddClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = DeepSea4) },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isReorderActive) "Done Reordering" else "Reorder Events",
                                color = DeepSea5,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.DragHandle, contentDescription = null, tint = DeepSea4) },
                        onClick = {
                            menuExpanded = false
                            onToggleReorder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                        onClick = {
                            menuExpanded = false
                            // Rename via a simple dialog could be added; for now trigger with empty to surface rename affordance
                            onRenameTrip(destination)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Archive Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null, tint = DeepSea4) },
                        onClick = {
                            currentTripId?.let(onArchiveTrip)
                            menuExpanded = false
                        },
                        enabled = currentTripId != null
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Trip", color = Color(0xFFE77D90), fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFE77D90)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        enabled = currentTripId != null
                    )
                }
            }
        }

        // Hero section: label + big date (with nav arrows in calendar mode) + member avatars
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isInCalendarMode) {
                val activeDate = selectedDate.takeIf { it.isNotBlank() } ?: sortedDates.firstOrNull().orEmpty()
                val activeIndex = sortedDates.indexOf(activeDate).coerceAtLeast(0)
                val previousDate = sortedDates.getOrNull(activeIndex - 1)
                val nextDate = sortedDates.getOrNull(activeIndex + 1)

                Column {
                    Text(
                        text = "SCHEDULED ITINERARY",
                        color = PrimaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDayOfWeekShort(activeDate),
                        color = DeepSea4,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { previousDate?.let(onDateSelected) },
                            enabled = previousDate != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous day",
                                tint = if (previousDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = formatHeroDate(activeDate),
                            color = DeepSea5,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 52.sp
                        )
                        IconButton(
                            onClick = { nextDate?.let(onDateSelected) },
                            enabled = nextDate != null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next day",
                                tint = if (nextDate != null) DeepSea5 else DeepSea4.copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                Column {
                    Text(
                        text = "SCHEDULED ITINERARY",
                        color = PrimaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatHeroDate(dateFrom),
                        color = DeepSea5,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 52.sp
                    )
                }
            }

            if (members.isNotEmpty()) {
                MemberAvatarRow(
                    members = members,
                    onClick = { showMembersSheet = true }
                )
            }
        }

        if (isInCalendarMode) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                controlsContent()
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MemberAvatarRow(members: List<TripMemberUi>, onClick: () -> Unit) {
    val display = members.take(2)
    val overflow = members.size - display.size

    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        display.forEach { member ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, DeepSea1, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.initial.uppercaseChar().toString(),
                    color = PrimaryAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, DeepSea1, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    color = PrimaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberListSheet(members: List<TripMemberUi>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSea2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "TRIP MEMBERS",
                color = PrimaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            members.forEach { member ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initial.uppercaseChar().toString(),
                            color = PrimaryAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = member.displayName,
                        color = DeepSea5,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
