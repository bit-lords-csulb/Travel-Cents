package com.example.travelcents.ui.main.newtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5


private const val S4BudgetMin = 500
private const val S4BudgetMax = 20000

// Round to nearest $50 increment within the allowed range
private fun snapTo50(value: Int): Int =
    (((value - S4BudgetMin + 25) / 50) * 50 + S4BudgetMin).coerceIn(S4BudgetMin, S4BudgetMax)

private fun budgetToTier(budget: Int): Triple<String, String, Color> = when {
    budget <= 3000 -> Triple("Budget", "Hostels, street food, public transport", Color(0xFF66BB6A))
    budget <= 8000 -> Triple("Comfort", "3-star hotels, local restaurants, mix of transport", TripWizardColors.Blue)
    else -> Triple("Luxury", "Premium hotels, fine dining, exclusive experiences", Color(0xFFB5A0FF))
}

@Composable
fun TripStep4BudgetPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val initialBudget = snapTo50(viewModel.budgetTotal.toIntOrNull() ?: 5000)
    var budgetInt by remember { mutableIntStateOf(initialBudget) }
    var textInput by remember { mutableStateOf(initialBudget.toString()) }
    val (tierName, tierDesc, tierColor) = budgetToTier(budgetInt)

    // Sync to ViewModel
    viewModel.budgetTotal = budgetInt.toString()
    viewModel.currency = "USD"
    viewModel.travelStyle = tierName.lowercase()

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TripWizardColors.Blue)
                    }
                    Text("Step 4 of 5", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, "Close", tint = TripWizardColors.Blue)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(TripWizardColors.ContainerHigh)) {
                Box(
                    modifier = Modifier.fillMaxWidth(0.8f).height(3.dp)
                        .background(Brush.horizontalGradient(listOf(TripWizardColors.Blue, TripWizardColors.Blue.copy(alpha = 0.7f))))
                )
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Progress widget
            S4ProgressWidget(stepsComplete = 4)
            Spacer(Modifier.height(20.dp))

            // Headline
            Text(
                "What's your budget?",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepSea5,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "TravelCents will create the perfect itinerary within your budget",
                fontSize = 14.sp,
                color = TripWizardColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))

            // Main budget card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(TripWizardColors.ContainerHigh.copy(alpha = 0.6f), Color(0xFF02132B).copy(alpha = 0.4f))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Dollar icon badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(TripWizardColors.PrimaryContainer.copy(alpha = 0.3f), TripWizardColors.Blue.copy(alpha = 0.1f))
                                )
                            )
                            .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AttachMoney, null, tint = TripWizardColors.Blue, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(24.dp))

                    // Large price — tap to edit directly
                    BasicTextField(
                        value = textInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            textInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed >= S4BudgetMin) {
                                budgetInt = snapTo50(parsed.coerceIn(S4BudgetMin, S4BudgetMax))
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepSea5,
                            letterSpacing = (-2).sp,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(TripWizardColors.Blue),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "$",
                                    fontSize = 52.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DeepSea5,
                                    letterSpacing = (-2).sp
                                )
                                innerTextField()
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))

                    // Tier badge
                    Box(
                        modifier = Modifier
                            .background(TripWizardColors.SurfaceBright, RoundedCornerShape(999.dp))
                            .border(1.dp, Color(0xFF3B4861).copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            tierName.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(Modifier.height(28.dp))

                    // Slider with $50 increments ($500–$20,000)
                    Slider(
                        value = budgetInt.toFloat(),
                        onValueChange = { value ->
                            budgetInt = snapTo50(value.toInt())
                            textInput = budgetInt.toString()
                        },
                        valueRange = S4BudgetMin.toFloat()..S4BudgetMax.toFloat(),
                        steps = 389,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = TripWizardColors.Blue,
                            inactiveTrackColor = TripWizardColors.ContainerHighest
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$500", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.OnSurfaceVariant, letterSpacing = 1.sp)
                        Text("$20,000", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.OnSurfaceVariant, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Tier info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TripWizardColors.ContainerHigh.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tierColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.TrendingUp, null, tint = tierColor, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("$tierName Experience", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                    Text(tierDesc, fontSize = 11.sp, color = TripWizardColors.OnSurfaceVariant, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Bottom button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripWizardColors.Blue,
                    contentColor = Color(0xFF001627)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue to Interests", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun S4ProgressWidget(stepsComplete: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("YOUR PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.OnSurfaceVariant, letterSpacing = 1.5.sp)
                    Text("Step $stepsComplete of 5", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                }
                Box(
                    modifier = Modifier
                        .background(TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("${stepsComplete * 20}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TripWizardColors.Blue)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i < stepsComplete) TripWizardColors.Blue else TripWizardColors.ContainerHighest)
                    )
                }
            }
        }
    }
}
