package com.example.travelcents.ui.main.current

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.model.Itinerary
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun CurrentTripHeader(
    tripTitle: String,
    dateRange: String,
    currentTripId: String?,
    allTrips: List<Itinerary>,
    canAdd: Boolean,
    isReorderActive: Boolean,
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
    var editableTitle by remember { mutableStateOf(tripTitle) }
    var isEditingTitle by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(tripTitle) {
        editableTitle = tripTitle
        isEditingTitle = false
    }

    LaunchedEffect(isEditingTitle) {
        if (isEditingTitle) titleFocusRequester.requestFocus()
    }

    if (showDeleteDialog && currentTripId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DeepSea2,
            title = { Text("Delete trip?", color = DeepSea5, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"$tripTitle\" and all its events will be permanently deleted.",
                    color = DeepSea4,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTrip(currentTripId)
                        showDeleteDialog = false
                    }
                ) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditingTitle) {
                        BasicTextField(
                            value = editableTitle,
                            onValueChange = { new -> editableTitle = new.filter { it != '\n' && it != '\t' } },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = DeepSea5,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            cursorBrush = SolidColor(DeepSea5),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(titleFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused && isEditingTitle) {
                                        isEditingTitle = false
                                        onRenameTrip(editableTitle)
                                    }
                                }
                        )
                    } else {
                        Text(
                            text = editableTitle,
                            color = DeepSea5,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditingTitle = true }
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { switcherExpanded = true },
                            enabled = allTrips.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch trip",
                                tint = if (allTrips.isNotEmpty()) DeepSea4 else DeepSea4.copy(alpha = 0.35f)
                            )
                        }

                        DropdownMenu(
                            expanded = switcherExpanded,
                            onDismissRequest = { switcherExpanded = false },
                            modifier = Modifier
                                .background(DeepSea2)
                                .widthIn(min = 220.dp)
                        ) {
                            allTrips.forEach { trip ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = trip.tripName,
                                            color = if (trip.itineraryId == currentTripId) DeepSea5 else DeepSea4,
                                            fontWeight = if (trip.itineraryId == currentTripId) FontWeight.Bold else FontWeight.Medium
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
                }

                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = dateRange,
                    color = DeepSea4,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (canAdd) DeepSea3 else DeepSea3.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(enabled = canAdd, onClick = onAddClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add plan",
                            tint = if (canAdd) DeepSea5 else DeepSea5.copy(alpha = 0.45f)
                        )
                    }
                }

                Box {
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
        }

        Spacer(modifier = Modifier.size(18.dp))
        controlsContent()
        Spacer(modifier = Modifier.size(16.dp))
    }
}
