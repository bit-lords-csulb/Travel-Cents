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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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

private val S1Blue = Color(0xFF64B5F6)
private val S1ContainerHigh = Color(0xFF0B203D)
private val S1ContainerHighest = Color(0xFF102645)
private val S1SurfaceBright = Color(0xFF152C4E)
private val S1OnSurfaceVariant = Color(0xFF9EABC8)

private data class PopularDestination(val name: String, val tagline: String, val imageUrl: String)

private val popularDestinations = listOf(
    PopularDestination(
        "Paris", "The city of light and romance.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuDiCmefTHq1BCrwr3b6njBkFlxiKcEQ0YeBwSddT5WXZyF90wgq5bX5f1PWrSa_gHQqrM4bNWPUFtA8lb1DKNXsvQ4tWvoxOEfBULjVBtTIvmAgfBmwIk6p1tZmGKvLwtqwQPGesRzTX3fWCmXQg_s00Qo_DFJ-QoGfX6XV5HGjEULyFJY_YzMXJ7UsVo4rmAMVWIFBZqPFt16ZT1y-NfuGN_e336jc-H26J_t5PcEwsc2CSpwhNukIXqz_Mfwa4D1AgLqsl7rbiMCQ"
    ),
    PopularDestination(
        "Tokyo", "Futuristic energy meets tradition.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuDPB2FwB6hef7979dsv5jlkAbyeYyESpofiu-Z-RINWg0FkeicdccIq7XrVtSDQQFrN0900ARwyjIohqBuIwn2Kyej6j9UpQOBk8IFqxm9r8FvvUnLd9Kovjdfkli5iCS_lqJdFqQ_y_ZIv10oI0vg7pWFj6nFgHrMoGFMkqka415JQrHbK2_hM45pFIiCHVuLvfXnhqFAWqPVD54TpNM-yr3n3k3E9tkDf411TuMRm0R4fROJvllK_6yLsG5oALnKJecakUCWYuYJj"
    ),
    PopularDestination(
        "Bali", "Serene island paradise.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuCUq1iWawno-rDroE79eDJY5L79NpluXGbiIs3HE02JwWbCrBIzb37MgRd_XIjScol0hnsx0sTqOX_cQLYcZVD8iO_flmoUVlpyd7Kbn681PFdgDLroIixD1Kq1kw2MnLgxWjhovvBdk8ZDmguhUKB_PdKiuBBPyNDecVxuj7hn3PFUmd7B9ernr-duZdxyPN2TMfo5N3ldbRjlxw6RmFblDviMO1FUC8duOcX1hNBQ8MPI7SldGviYsJiZN7j5s-nshM0867-cW4Dh"
    ),
    PopularDestination(
        "New York", "The city that never sleeps.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuDYTIgmOdI9DpaNeAQRshjgYQKlb6JRdcFy58XrdTnyxaOEQqe2q3g-s2wV8J0fs-BMEZUcXL1YjIsaqDGvBfJZw82CfByB9oy3JQizBJsGo7MZhOhuqA5Hlz_d1OuqC3lKrwTvoHmXzn5adzChryMq2xGUcUaa1UGDWOoD-XkMy97IVcWGjjmLz2yTLYByavPzAFBu6NWLXr1XD_Y-9z9QR5wHqIpE6zz5Pew3LH_STDg4e-9sGm9w2Ns0v4SG_wqSKBWM9FmwdtwL"
    ),
    PopularDestination(
        "London", "A blend of history and modernity.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAt_wFi8zqCK5bIaoyBXCIqd2MDwUD5GJ3vpozasWYNSw3oVjojCiyA_8kXpL-sokF0fKh1Bg4Vir7zumxPEz3o4Io2xuISt46u8jtCASVAY3baRFNzkquvdvFeXY1uG6Z7X4sI22B3g6Nu4bFJlL1Qrc5Ot5aJBhwk5fMtvlqIr6_EDlajdCRD7YP4Q4stGE1v9Sgau_ZbvtytF5hqTvnsL6Y9o8y1fDxE4Ew2rbC08nHEtIQnj6RlMgokBomoo5upsZs4aomJkqgb"
    ),
    PopularDestination(
        "Dubai", "Sky-high luxury and desert gold.",
        "https://lh3.googleusercontent.com/aida-public/AB6AXuAo7ZFFq2eoQpRJGsya7Z9s-TnZS9W2FwvPkUjzLR3NVF7dmED6a2G4UmfEa8XydlV_G0-9BcMtulEyV2tQMN3AVfAeINhUW-1UbtkR7gD4uW-S-J8Mvy8IkVXPVGP7649DCmBxDS8xaxTxHFaxLixTRmpym5rS_qBDRIOMEh9A5DmEZ6z9zNBw0-eHOEN-ZAURd92ot96AC7P9_8QSk6Wlta4DXHs3ShvZjt7Yii05qUjYrEHvljrjWXwghxeNnOY-BqDntHwi0H0h"
    )
)

@Composable
fun TripStep1DestinationPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        // Top bar + progress strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF010E24))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = S1Blue)
                }
                Text("Step 1 of 5", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = S1Blue)
                }
            }
            // Thin progress bar at 20%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(S1ContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(listOf(S1Blue, S1Blue.copy(alpha = 0.7f)))
                        )
                )
            }
        }

        // Scrollable content
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress widget
            item(span = { GridItemSpan(2) }) {
                ProgressSummaryWidget()
            }

            // Hero
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "DESTINATION SELECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = S1Blue,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Where to?",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepSea5,
                        letterSpacing = (-1.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose your dream destination — TravelCents will handle the rest.",
                        fontSize = 14.sp,
                        color = S1OnSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Search field
            item(span = { GridItemSpan(2) }) {
                TextField(
                    value = viewModel.destination,
                    onValueChange = { viewModel.destination = it },
                    placeholder = {
                        Text(
                            "Search for a destination...",
                            color = S1OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = S1Blue,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = DeepSea5,
                        unfocusedTextColor = DeepSea5,
                        cursorColor = S1Blue,
                        focusedContainerColor = S1SurfaceBright,
                        unfocusedContainerColor = S1SurfaceBright,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            // Popular destinations header
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Popular Destinations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSea5
                    )
                    Text(
                        text = "Curated picks for your next adventure",
                        fontSize = 12.sp,
                        color = S1OnSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Destination cards
            items(popularDestinations) { dest ->
                DestinationCard(
                    destination = dest,
                    isSelected = viewModel.destination == dest.name,
                    onClick = { viewModel.destination = dest.name }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Sticky continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = viewModel.destination.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = S1Blue,
                    contentColor = Color(0xFF001627),
                    disabledContainerColor = S1Blue.copy(alpha = 0.25f),
                    disabledContentColor = DeepSea5.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProgressSummaryWidget() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(S1ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR PROGRESS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = S1OnSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Box(
                    modifier = Modifier
                        .background(S1Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, S1Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "20%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = S1Blue)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (index == 0) S1Blue else S1ContainerHighest)
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: PopularDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(S1ContainerHigh)
            .then(
                if (isSelected) Modifier.border(2.dp, S1Blue, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = destination.imageUrl,
            contentDescription = destination.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 300f,
                        endY = Float.MAX_VALUE
                    )
                )
        )
        // Selected tint
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(S1Blue.copy(alpha = 0.15f))
            )
        }
        // Labels
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = destination.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSea5
            )
            Text(
                text = destination.tagline,
                fontSize = 10.sp,
                color = S1OnSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}