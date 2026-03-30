package com.example.travelcents.ui.main.newtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5

private val S5Blue = Color(0xFF64B5F6)
private val S5PrimaryContainer = Color(0xFF54A7E7)
private val S5ContainerHigh = Color(0xFF0B203D)
private val S5ContainerHighest = Color(0xFF102645)
private val S5OnSurfaceVariant = Color(0xFF9EABC8)

private data class InterestItem(val key: String, val label: String, val imageUrl: String)

private val interestItems = listOf(
    InterestItem("culture", "Culture & Museums",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBQgG5AV6ZRSd5warjbvwJHDDTnKcftnCkN-mS9PCK67sN-LYchbIgkFscXlPCRpYG2pK5dJoyHxuZHn-JQnIseChKlNYo1Mg5bVxtxfdmxX6b2ufgy8xWst7hBHXeR1knlvnoWOtT9exFCZwM4g_OhcURVrBK01rFbQaYbVbk5hBL0m39PFHp3D7IU9e7kDuu-KddeLPuFg4iMT4IDEbzgca-4bX6X_WWCkNNTgDVcsmh36VJtwH2n-n0ADTnUGdzJwihjDl4rmGRE"),
    InterestItem("food", "Food & Dining",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuBMrMnT2Ge9ZSVhIjCopBuHhaOHthQ3aV35al9kAqgeaIJpeCCkjkHodz9_NO0kvJjs8OGQHBq2o4QfM1SxrPRFtloGfvR1FAgzb2yezsFOjAeVSDdQHs0tMvDKHgkzdphr2N9bBfnfse99oyVLD-NBaclTUlAYuUSR2xSOKgozmK7tJX525dU4S8GNxIqryMKF5JeqnWPwxV6HKA8jc23xwt3xBku3fQ0tqSH-VGNycX3bXE3Q-NiYReVXzpzYGB3pOgHmTjKLyEqB"),
    InterestItem("nature", "Beach & Nature",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuC6TLVFs3HBW-_MVu9PzqXltSOJeYCzUy1ktJR0qy0n--Jg_c66NfoE10jh1JGM345I6I24WtRctgbPHmXs1EeS8RZ-egJ8yBgKARsS_JFTV0WPtVnD0BlBKRzxTbYr9z08333rVWNaOAzH3UiG2uT-XWW8WXw2bqdNX7zNYxKYappXeL-YFgAfA-DJWooD_iUVt9NhMycAkLGnitJ0_1sn_Hpx7NvjqC8zMMQ-B6qvZlZBCgaVbGoMGzOgT_WEjtrYgbFu1kZF4RqM"),
    InterestItem("adventure", "Adventure",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuA_Vr7JpAxTuGSL3ZTQ7Pnb-51_fZwstjzHeOOYKw_NWytPbzATei9XzvuVBkWJqCzLIeZoXNn1KQ2mb0gUPB58tn3YTjkneuam-NQy2-VwagCGkdDWeUH9DBU-BtmdtjH0kfkjrMwxycFOFTxW8SWyCNr-3QFaSfcHLyjgsaWnBU0S54yeStG6e9NrNgUo3PUGoQuq9yFOuG3-ZgnD-uKRpIKOh_TkWqF0k2XjeZwUKWmTlkTdzTnPo-nn12zpRZ-ZOGWE0D84IDDA"),
    InterestItem("nightlife", "Nightlife",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCUvffFNH42K-4EKTd-BDiP4OkmJTXzn8kc_fL8WLSqmNyzZx45-tmX6Lr-lcLsnUcQaOQOAqdY8g0ltkFNG78-hYk6oAchi5j_SJckauYGvf-fpu9JXL0lWQ85aeb7lCmf1RP4M1DgpCVXL_LBBuph3LbMIDUVKRBk0wa4zL66Y3f6nsmmiiAtux5i6SjgmpCtHYpXD6syUs_O1rwinmoXLkSI2Vz8MSigrCTbm9qyaG7JiSq2BTVAQFgSJtD_7x4uv4FC_4_4uItO"),
    InterestItem("shopping", "Shopping",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuDYTIgmOdI9DpaNeAQRshjgYQKlb6JRdcFy58XrdTnyxaOEQqe2q3g-s2wV8J0fs-BMEZUcXL1YjIsaqDGvBfJZw82CfByB9oy3JQizBJsGo7MZhOhuqA5Hlz_d1OuqC3lKrwTvoHmXzn5adzChryMq2xGUcUaa1UGDWOoD-XkMy97IVcWGjjmLz2yTLYByavPzAFBu6NWLXr1XD_Y-9z9QR5wHqIpE6zz5Pew3LH_STDg4e-9sGm9w2Ns0v4SG_wqSKBWM9FmwdtwL"),
    InterestItem("history", "History",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAt_wFi8zqCK5bIaoyBXCIqd2MDwUD5GJ3vpozasWYNSw3oVjojCiyA_8kXpL-sokF0fKh1Bg4Vir7zumxPEz3o4Io2xuISt46u8jtCASVAY3baRFNzkquvdvFeXY1uG6Z7X4sI22B3g6Nu4bFJlL1Qrc5Ot5aJBhwk5fMtvlqIr6_EDlajdCRD7YP4Q4stGE1v9Sgau_ZbvtytF5hqTvnsL6Y9o8y1fDxE4Ew2rbC08nHEtIQnj6RlMgokBomoo5upsZs4aomJkqgb")
)

@Composable
fun TripStep5InterestsPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onTripGenerated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is TripUiState.Loading
    val errorMessage = (uiState as? TripUiState.Error)?.message

    // Navigate to Current when trip is generated
    LaunchedEffect(uiState) {
        if (uiState is TripUiState.Success) {
            viewModel.resetState()
            onTripGenerated()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Top bar
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF010E24))) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = S5Blue)
                    }
                    Text("Step 5 of 5", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close", tint = S5Blue)
                }
            }
            // Full progress bar (100%)
            Box(
                modifier = Modifier.fillMaxWidth().height(3.dp)
                    .background(Brush.horizontalGradient(listOf(S5Blue, S5PrimaryContainer)))
            )
        }

        // Loading overlay
        if (isLoading) {
            val msg = (uiState as TripUiState.Loading).statusMessage
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = S5Blue, modifier = Modifier.size(48.dp))
                    Text(msg, color = S5OnSurfaceVariant, fontSize = 14.sp)
                }
            }
            return@Column
        }

        // Grid content
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress widget
            item(span = { GridItemSpan(2) }) {
                S5ProgressWidget()
            }

            // Hero
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        "PERSONALIZATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = S5OnSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "What interests you?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepSea5,
                        letterSpacing = (-1).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${viewModel.interests.size} selected",
                            fontSize = 12.sp,
                            color = if (viewModel.interests.size >= 1) S5Blue else S5OnSurfaceVariant
                        )
                        Text("Select at least 1", fontSize = 12.sp, color = S5OnSurfaceVariant)
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF9F0519).copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Text(errorMessage, color = Color(0xFFFF716C), fontSize = 13.sp)
                    }
                }
            }

            // Interest cards
            items(interestItems) { item ->
                val selected = item.key in viewModel.interests
                S5InterestCard(
                    item = item,
                    selected = selected,
                    onClick = { viewModel.toggleInterest(item.key) }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(4.dp))
            }
        }

        // Plan My Trip button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = {
                    if (viewModel.origin.isBlank()) viewModel.origin = "Not specified"
                    viewModel.generateTrip()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = viewModel.interests.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = S5Blue,
                    contentColor = Color(0xFF001627),
                    disabledContainerColor = S5Blue.copy(alpha = 0.25f),
                    disabledContentColor = DeepSea5.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue to Generate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun S5ProgressWidget() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(S5ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOUR PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = S5OnSurfaceVariant, letterSpacing = 1.5.sp)
                Box(
                    modifier = Modifier
                        .background(S5Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, S5Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("100%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = S5Blue)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(S5Blue)
                    )
                }
            }
        }
    }
}

@Composable
private fun S5InterestCard(
    item: InterestItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(S5ContainerHigh)
            .then(
                if (selected)
                    Modifier.border(2.dp, S5Blue.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = if (selected) 0.8f else 0.6f
        )
        // Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 80f,
                        endY = Float.MAX_VALUE
                    )
                )
        )
        // Selected tint
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(S5Blue.copy(alpha = 0.1f)))
        }
        // Label
        Text(
            text = item.label,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5
        )
        // Check indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) S5Blue else Color.White.copy(alpha = 0.1f))
                .border(1.5.dp, if (selected) S5Blue else Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Default.Check, null, tint = Color(0xFF001627), modifier = Modifier.size(12.dp))
            }
        }
    }
}
