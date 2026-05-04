package com.example.travelcents.ui.main.aichat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.data.ai.chat.AiChatCardOption
import com.example.travelcents.data.ai.chat.AiChatItem
import com.example.travelcents.data.ai.chat.AiCuratedTripStarter
import com.example.travelcents.data.ai.chat.AiTripIntakeProfile
import com.example.travelcents.data.trip.TripKey
import com.example.travelcents.ui.main.aichat.components.AddToTripBottomSheet
import com.example.travelcents.ui.main.aichat.components.AiChatLockedDestinationBanner
import com.example.travelcents.ui.main.aichat.components.AiChatBubble
import com.example.travelcents.ui.main.aichat.components.AiChatComposer
import com.example.travelcents.ui.main.aichat.components.AiChatHistoryScreen
import com.example.travelcents.ui.main.aichat.components.AiPromptCardGrid
import com.example.travelcents.ui.main.aichat.components.AiRecommendationRow
import com.example.travelcents.ui.main.aichat.components.AiResponseCardGroup
import com.example.travelcents.ui.main.aichat.components.AiSingleEventCard
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts
import kotlinx.coroutines.delay
import com.example.travelcents.ui.main.aichat.components.AiCuratedTripRow as AiCuratedTripRowSection

@Composable
fun AiTripChatPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onOpenTrip: (TripKey) -> Unit = {},
    onOpenPreviewTrip: () -> Unit = {},
    onCreateDraftTrip: (AiCuratedTripStarter, AiTripIntakeProfile) -> Unit = { _, _ -> },
    viewModel: AiChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showHistory by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = LocalContext.current
    val userMessageSettleOffset = with(density) { 92.dp.roundToPx() }
    val responseSettleOffset = with(density) { 132.dp.roundToPx() }
    val restoreSettleOffset = with(density) { 72.dp.roundToPx() }
    val selectedDraftOptionIds = uiState.selectedDraftOptions.mapTo(mutableSetOf()) { option ->
        option.id
    }

    val isFreshChat by remember(uiState.items, uiState.isLoading) {
        derivedStateOf {
            !uiState.isLoading && uiState.items.none { it is AiChatItem.TextMessage }
        }
    }

    LaunchedEffect(
        uiState.anchorMessageId,
        uiState.activeResponseCardGroupId,
        uiState.activeDestinationRecommendationRowId,
        uiState.activeCuratedTripRowId,
        uiState.activePlaceRecommendationRowId,
        uiState.activeSingleEventCardId,
        uiState.isLoading,
        uiState.items.size,
        isFreshChat
    ) {
        if (isFreshChat) return@LaunchedEffect

        when {
            uiState.isLoading && uiState.anchorMessageId != null -> {
                val anchorIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.anchorMessageId
                }
                if (anchorIndex >= 0) {
                    delay(90)
                    listState.animateScrollToItem(
                        index = anchorIndex,
                        scrollOffset = -userMessageSettleOffset
                    )
                }
            }

            !uiState.isLoading && uiState.activeResponseCardGroupId != null -> {
                val responseIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.activeResponseCardGroupId
                }
                if (responseIndex >= 0) {
                    delay(140)
                    listState.animateScrollToItem(
                        index = responseIndex,
                        scrollOffset = -responseSettleOffset
                    )
                }
            }

            !uiState.isLoading && uiState.activeDestinationRecommendationRowId != null -> {
                val destinationRowIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.activeDestinationRecommendationRowId
                }
                if (destinationRowIndex >= 0) {
                    delay(140)
                    listState.animateScrollToItem(
                        index = destinationRowIndex,
                        scrollOffset = -responseSettleOffset
                    )
                }
            }

            !uiState.isLoading && uiState.activeSingleEventCardId != null -> {
                val eventIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.activeSingleEventCardId
                }
                if (eventIndex >= 0) {
                    delay(140)
                    listState.animateScrollToItem(
                        index = eventIndex,
                        scrollOffset = -responseSettleOffset
                    )
                }
            }

            !uiState.isLoading && uiState.activePlaceRecommendationRowId != null -> {
                val placeRowIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.activePlaceRecommendationRowId
                }
                if (placeRowIndex >= 0) {
                    delay(140)
                    listState.animateScrollToItem(
                        index = placeRowIndex,
                        scrollOffset = -responseSettleOffset
                    )
                }
            }

            !uiState.isLoading && uiState.activeCuratedTripRowId != null -> {
                val curatedRowIndex = uiState.items.indexOfFirst { item ->
                    item.id == uiState.activeCuratedTripRowId
                }
                if (curatedRowIndex >= 0) {
                    delay(140)
                    listState.animateScrollToItem(
                        index = curatedRowIndex,
                        scrollOffset = -responseSettleOffset
                    )
                }
            }

            uiState.items.isNotEmpty() -> {
                listState.animateScrollToItem(
                    index = uiState.items.lastIndex.coerceAtLeast(0),
                    scrollOffset = -restoreSettleOffset
                )
            }
        }
    }

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
            AiChatBackdrop()

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AiChatHeader(
                    onBackClick = onBackClick,
                    onHistoryClick = { showHistory = true },
                    onNewChatClick = { viewModel.startNewChat() }
                )

                AiChatLockedDestinationBanner(
                    destination = uiState.lockedDestination,
                    imageUrl = uiState.lockedDestinationImageUrl,
                    onOpenTrip = onOpenPreviewTrip
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    if (isFreshChat && uiState.starterCards.isNotEmpty()) {
                        item("starter_welcome") {
                            AiStarterLanding(
                                options = uiState.starterCards,
                                selectedOptionIds = selectedDraftOptionIds,
                                onOptionClick = viewModel::toggleStarterCard
                            )
                        }
                    }
                    items(uiState.items, key = { it.id }) { item ->
                        when (item) {
                            is AiChatItem.TextMessage -> {
                                AiChatBubble(
                                    text = item.text,
                                    sender = item.sender,
                                    tags = item.tags
                                )
                            }

                            is AiChatItem.SystemStatus -> {
                                AiSystemStatusCard(
                                    title = item.title,
                                    detail = item.detail
                                )
                            }

                            is AiChatItem.ResponseCardGroup -> {
                                AiResponseCardGroup(
                                    group = item.group,
                                    selectedOptionIds = selectedDraftOptionIds,
                                    enabled = uiState.activeResponseCardGroupId == item.group.id && !uiState.isLoading,
                                    onOptionClick = { option ->
                                        viewModel.toggleResponseCard(option, item.group)
                                    }
                                )
                            }

                            is AiChatItem.DestinationRecommendationRow -> {
                                AiRecommendationRow(
                                    title = item.row.title,
                                    subtitle = item.row.subtitle,
                                    recommendations = item.row.recommendations.map { recommendation ->
                                        com.example.travelcents.ui.main.aichat.components.AiRecommendationCardModel(
                                            id = recommendation.id,
                                            headline = recommendation.destination,
                                            supporting = recommendation.summary,
                                            meta = recommendation.matchReason,
                                            kind = "Destination",
                                            imageUrl = recommendation.imageUrl
                                        )
                                    },
                                    onRecommendationSelected = { recommendationId ->
                                        item.row.recommendations
                                            .firstOrNull { recommendation -> recommendation.id == recommendationId }
                                            ?.let { recommendation ->
                                                viewModel.selectDestinationRecommendation(recommendation)
                                            }
                                    }
                                )
                            }

                            is AiChatItem.CuratedTripRow -> {
                                AiCuratedTripRowSection(
                                    row = item.row,
                                    onDurationSelected = { starter, durationDays ->
                                        viewModel.updateCuratedStarterDuration(
                                            starterId = starter.id,
                                            durationDays = durationDays
                                        )
                                    },
                                    onTripSelected = { starter ->
                                        viewModel.handleRecommendedStarterSelection(
                                            starter = starter,
                                            onOpenTrip = onOpenTrip,
                                            onCreateDraftTrip = onCreateDraftTrip
                                        )
                                    }
                                )
                            }

                            is AiChatItem.SingleEventCard -> {
                                val card = item.card
                                AiSingleEventCard(
                                    suggestion = card,
                                    onAddToTrip = { viewModel.requestAddSingleEventToTrip(card) },
                                    onOpenTickets = {
                                        card.bookingUrl?.let { url ->
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(url)
                                            )
                                            runCatching { context.startActivity(intent) }
                                        }
                                    },
                                    onDismiss = { viewModel.dismissSingleEventCard(card.id) }
                                )
                            }

                            is AiChatItem.PlaceRecommendationRow -> {
                                AiRecommendationRow(
                                    title = item.row.title,
                                    subtitle = item.row.subtitle,
                                    recommendations = item.row.recommendations.map { recommendation ->
                                        com.example.travelcents.ui.main.aichat.components.AiRecommendationCardModel(
                                            id = recommendation.id,
                                            headline = recommendation.name,
                                            supporting = recommendation.summary.ifBlank { recommendation.area },
                                            meta = recommendation.matchReason,
                                            kind = recommendation.category,
                                            actionLabels = item.row.actionLabels,
                                            actionsEnabled = item.row.actionsEnabled,
                                            imageUrl = recommendation.imageUrl
                                        )
                                    },
                                    onRecommendationSelected = { recommendationId ->
                                        item.row.recommendations
                                            .firstOrNull { recommendation -> recommendation.id == recommendationId }
                                            ?.let { recommendation ->
                                                viewModel.selectPlaceRecommendation(
                                                    name = recommendation.name,
                                                    category = recommendation.category,
                                                    area = recommendation.area
                                                )
                                            }
                                    }
                                )
                            }
                        }
                    }

                    if (uiState.isLoading) {
                        item("loading") {
                            AiLoadingCard()
                        }
                    }

                    item("bottom_anchor_spacer") {
                        Spacer(modifier = Modifier.fillParentMaxHeight(0.9f))
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = Color(0xFF041329).copy(alpha = 0.96f)
                ) {
                    AiChatComposer(
                        value = uiState.draftText,
                        onValueChange = viewModel::updateDraftText,
                        onSendClick = viewModel::sendDraft,
                        canSend = if (uiState.requiresTypedDraft) {
                            uiState.draftText.isNotBlank()
                        } else {
                            uiState.draftText.isNotBlank() || uiState.selectedDraftOptions.isNotEmpty()
                        },
                        isSending = uiState.isLoading,
                        selectedDraftOptions = uiState.selectedDraftOptions,
                        helperText = uiState.composerHint,
                        onRemoveDraftOption = viewModel::removeDraftOption
                    )
                }
            }

            uiState.pendingAddToTripEvent?.let { pendingEvent ->
                AddToTripBottomSheet(
                    suggestion = pendingEvent,
                    trips = uiState.availableTrips,
                    onTripSelected = { trip -> viewModel.confirmAddSingleEventToTrip(trip) },
                    onDismiss = { viewModel.dismissAddToTripSheet() }
                )
            }

            if (showHistory) {
                AiChatHistoryScreen(
                    entries = uiState.historyEntries,
                    onBackClick = { showHistory = false },
                    onSessionSelected = { sessionId ->
                        showHistory = false
                        viewModel.loadSession(sessionId)
                    },
                    onDeleteSession = viewModel::deleteSession,
                    onClearAllHistory = viewModel::clearAllHistory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BoxScope.AiChatBackdrop() {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = 104.dp)
            .size(320.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        TripWizardColors.Blue.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
    )

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = (-88).dp, y = 40.dp)
            .size(220.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFB875).copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun AiStarterLanding(
    options: List<AiChatCardOption>,
    selectedOptionIds: Set<String>,
    onOptionClick: (AiChatCardOption) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val panelModifier = if (maxWidth < 420.dp) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 420.dp)
        }

        Surface(
            modifier = panelModifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            color = Color(0xE610223C),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Where to next?",
                        color = DeepSea5,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = TravelCentsFonts.Headline
                    )
                    Text(
                        text = "Pick one or more trip directions, then add your own words below.",
                        color = DeepSea4,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = TravelCentsFonts.Body
                    )
                }

                AiPromptCardGrid(
                    title = "",
                    subtitle = "",
                    options = options,
                    selectedOptionIds = selectedOptionIds,
                    onOptionClick = onOptionClick
                )
            }
        }
    }
}

@Composable
private fun AiChatHeader(
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF010E24))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TripWizardColors.Blue
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TravelCents AI",
                    color = DeepSea5,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = TravelCentsFonts.Headline
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.clickable(onClick = onHistoryClick),
                    color = TripWizardColors.ContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "History",
                            tint = TripWizardColors.Blue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.clickable(onClick = onNewChatClick),
                    color = TripWizardColors.ContainerHigh,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New chat",
                            tint = TripWizardColors.Blue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            TripWizardColors.Blue,
                            TripWizardColors.Blue.copy(alpha = 0.35f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AiLoadingCard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp),
            color = TripWizardColors.ContainerLow,
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = 8.dp,
                bottomEnd = 22.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TripWizardColors.Blue,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "TravelCents AI is thinking...",
                    color = DeepSea4,
                    fontSize = 13.sp,
                    fontFamily = TravelCentsFonts.Body
                )
            }
        }
    }
}

@Composable
private fun AiSystemStatusCard(
    title: String,
    detail: String
) {
    Surface(
        color = TripWizardColors.ContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = DeepSea5,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TravelCentsFonts.Body
            )
            Text(
                text = detail,
                color = DeepSea4,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontFamily = TravelCentsFonts.Body
            )
        }
    }
}
