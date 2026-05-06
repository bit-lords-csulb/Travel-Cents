package com.example.travelcents.ui.main.current.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.data.trip.model.Itinerary
import com.example.travelcents.ui.main.current.TripMemberUi
import com.example.travelcents.ui.modules.formatDayOfWeekFull
import com.example.travelcents.ui.modules.formatHeroDate
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts

private val SurfaceContainerHigh = Color(0xFF222A3D)
private val SwitcherButtonShape = RoundedCornerShape(18.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTripHeader(
    tripTitle: String,
    heroDate: String,
    currentTripId: String?,
    currentTripOwnerUid: String?,
    viewerUid: String?,
    allTrips: List<Itinerary>,
    canAdd: Boolean,
    canEditTrip: Boolean,
    canManageTrip: Boolean,
    isReorderActive: Boolean,
    isInCalendarMode: Boolean,
    isWeekMode: Boolean,
    selectedDate: String,
    sortedDates: List<String>,
    onDateSelected: (String) -> Unit,
    members: List<TripMemberUi>,
    onCalendarClick: () -> Unit,
    onBackClick: () -> Unit,
    onDoneReordering: () -> Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
    onToggleReorder: () -> Unit,
    onArchiveTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onSwitchTrip: (TripKey) -> Unit,
    onRenameTrip: (String) -> Unit,
    controlsContent: @Composable () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var switcherExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }
    var editableTitle by remember { mutableStateOf(tripTitle) }
    val displayTitle = tripTitle.ifBlank { "My Trip" }
    val titleSidePadding = if (allTrips.size > 1) 60.dp else 64.dp

    LaunchedEffect(tripTitle, showRenameDialog) {
        if (!showRenameDialog) {
            editableTitle = tripTitle
        }
    }

    fun submitTitleEdit() {
        val trimmed = editableTitle.trim()
        if (trimmed.isBlank()) {
            editableTitle = displayTitle
            return
        }

        editableTitle = trimmed
        if (trimmed != tripTitle) {
            onRenameTrip(trimmed)
        }
    }

    if (showDeleteDialog && currentTripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepSea2,
            title = { Text("Delete trip?", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"$displayTitle\" and all its events will be permanently deleted.",
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

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                editableTitle = tripTitle
            },
            containerColor = DeepSea2,
            title = { Text("Rename trip", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = editableTitle,
                    onValueChange = { newValue ->
                        editableTitle = newValue.filter { it != '\n' && it != '\t' }
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = displayTitle,
                            color = DeepSea4
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = DeepSea5,
                        unfocusedTextColor = DeepSea5,
                        focusedContainerColor = SurfaceContainerHigh,
                        unfocusedContainerColor = SurfaceContainerHigh,
                        disabledContainerColor = SurfaceContainerHigh,
                        focusedIndicatorColor = CurrentTripHeroAccent,
                        unfocusedIndicatorColor = DeepSea3,
                        cursorColor = CurrentTripHeroAccent
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        submitTitleEdit()
                        showRenameDialog = false
                    },
                    enabled = editableTitle.trim().isNotBlank()
                ) {
                    Text("Save", color = CurrentTripHeroAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDialog = false
                    editableTitle = tripTitle
                }) {
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val selectorMaxWidth = (maxWidth - 124.dp).coerceAtLeast(180.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = titleSidePadding, end = titleSidePadding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (allTrips.size > 1) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = selectorMaxWidth)
                            .clip(SwitcherButtonShape)
                            .background(SurfaceContainerHigh.copy(alpha = 0.72f))
                            .border(1.dp, DeepSea3.copy(alpha = 0.72f), SwitcherButtonShape)
                            .clickable(
                                role = Role.Button,
                                onClick = { switcherExpanded = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            color = CurrentTripHeroAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = TravelCentsFonts.Headline,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch trip",
                            tint = DeepSea5,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Text(
                        text = displayTitle,
                        color = CurrentTripHeroAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = TravelCentsFonts.Headline,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = selectorMaxWidth)
                    )
                }

                DropdownMenu(
                    expanded = switcherExpanded,
                    onDismissRequest = { switcherExpanded = false },
                    shape = SwitcherButtonShape,
                    modifier = Modifier.widthIn(min = 200.dp),
                    containerColor = DeepSea2
                ) {
                    allTrips.forEach { trip ->
                        val isCurrentTrip = trip.itineraryId == currentTripId && trip.ownerUid == currentTripOwnerUid
                        val tripLabel = buildString {
                            append(trip.tripName)
                            if (trip.status.equals("archived", ignoreCase = true)) {
                                append(" · Archived")
                            } else if (!viewerUid.isNullOrBlank() && trip.ownerUid != viewerUid) {
                                append(" · Shared")
                            }
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = tripLabel,
                                    color = if (isCurrentTrip) DeepSea5 else DeepSea4,
                                    fontWeight = if (isCurrentTrip) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                switcherExpanded = false
                                if (!isCurrentTrip) {
                                    onSwitchTrip(
                                        TripKey(
                                            ownerUid = trip.ownerUid,
                                            tripId = trip.itineraryId
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val showDoneButton = !isInCalendarMode && isReorderActive
            HeaderModeButton(
                icon = when {
                    showDoneButton -> Icons.Default.CheckCircle
                    isInCalendarMode -> Icons.AutoMirrored.Filled.ArrowBack
                    else -> Icons.Default.DateRange
                },
                label = when {
                    showDoneButton -> "Done"
                    isInCalendarMode -> "Itinerary"
                    else -> "Calendar"
                },
                contentDescription = when {
                    showDoneButton -> "Finish reordering"
                    isInCalendarMode -> "Back to itinerary"
                    else -> "Open calendar view"
                },
                onClick = when {
                    showDoneButton -> onDoneReordering
                    isInCalendarMode -> onBackClick
                    else -> onCalendarClick
                },
                highlightColor = if (showDoneButton) Color(0xFF79E2A0) else CurrentTripHeroAccent,
                modifier = Modifier.align(Alignment.TopStart)
            )

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
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
                    shape = SwitcherButtonShape,
                    containerColor = DeepSea2
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
                        },
                        enabled = canManageTrip && currentTripId != null
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
                        },
                        enabled = canEditTrip && currentTripId != null
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = DeepSea4) },
                        onClick = {
                            menuExpanded = false
                            editableTitle = displayTitle
                            showRenameDialog = true
                        },
                        enabled = canManageTrip && currentTripId != null
                    )
                    DropdownMenuItem(
                        text = { Text("Archive Trip", color = DeepSea5, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null, tint = DeepSea4) },
                        onClick = {
                            currentTripId?.let(onArchiveTrip)
                            menuExpanded = false
                        },
                        enabled = canManageTrip && currentTripId != null
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Trip", color = Color(0xFFE77D90), fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFE77D90)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        },
                        enabled = canManageTrip && currentTripId != null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (members.isNotEmpty()) 24.dp else 0.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.BottomStart
            ) {
                when {
                    isInCalendarMode && isWeekMode -> WeekDateHero(
                        selectedDate = selectedDate,
                        sortedDates = sortedDates,
                        onDateSelected = onDateSelected
                    )
                    isInCalendarMode -> DayDateHero(
                        selectedDate = selectedDate,
                        sortedDates = sortedDates,
                        onDateSelected = onDateSelected
                    )
                    else -> CurrentTripHeroLayout {
                        Text(
                            text = formatDayOfWeekFull(heroDate),
                            color = DeepSea4,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            lineHeight = 16.sp,
                            fontFamily = TravelCentsFonts.Body
                        )
                        Text(
                            text = formatHeroDate(heroDate),
                            color = DeepSea5,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 50.sp,
                            fontFamily = TravelCentsFonts.Headline
                        )
                    }
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
private fun HeaderModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .clip(buttonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(buttonShape)
                .background(SurfaceContainerHigh)
                .border(1.dp, highlightColor.copy(alpha = 0.45f), buttonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = highlightColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = highlightColor,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            fontFamily = TravelCentsFonts.Body
        )
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
                    color = CurrentTripHeroAccent,
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
                    color = CurrentTripHeroAccent,
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
                color = CurrentTripHeroAccent,
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
                            color = CurrentTripHeroAccent,
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
