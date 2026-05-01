package com.example.travelcents.ui.main.newTrip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts


@Composable
fun TripStep3TravelersPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    var hasChildren by remember { mutableStateOf(viewModel.children > 0) }
    var hasPets by remember { mutableStateOf(viewModel.pets > 0) }

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
        WizardHeader(
            currentStep = 3,
            title = "How many travelers?",
            onBack = onBackClick,
            onClose = onCloseClick
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "TRAVELER DETAILS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TripWizardColors.OnSurfaceVariant,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Large guest stepper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(TripWizardColors.ContainerHigh)
                    .padding(horizontal = 32.dp, vertical = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minus button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TripWizardColors.SurfaceBright)
                            .border(1.dp, Color(0xFF3B4861).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .clickable { if (viewModel.adults > 1) viewModel.adults-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, null, tint = TripWizardColors.Blue, modifier = Modifier.size(24.dp))
                    }

                    // Count display
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (viewModel.adults + viewModel.children).toString(),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepSea5,
                            lineHeight = 72.sp
                        )
                        Text(
                            "TRAVELERS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TripWizardColors.OnSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        if (viewModel.pets > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "+ ${viewModel.pets} pet${if (viewModel.pets != 1) "s" else ""}",
                                fontSize = 12.sp,
                                color = TripWizardColors.OnSurfaceVariant
                            )
                        }
                    }

                    // Plus button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TripWizardColors.Blue)
                            .clickable { viewModel.adults++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF001627), modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Children toggle + stepper
            S3ToggleRow(
                icon = Icons.Default.ChildCare,
                title = "Children",
                subtitle = "Ages 2 – 12",
                checked = hasChildren,
                onCheckedChange = {
                    hasChildren = it
                    viewModel.children = if (it) 1 else 0
                }
            )
            if (hasChildren) {
                Spacer(Modifier.height(8.dp))
                S3InlineStepper(
                    count = viewModel.children,
                    onDecrement = { if (viewModel.children > 1) viewModel.children-- },
                    onIncrement = { viewModel.children++ },
                    label = "children"
                )
            }
            Spacer(Modifier.height(12.dp))

            // Pets toggle + stepper
            S3ToggleRow(
                icon = Icons.Default.Pets,
                title = "Pets",
                subtitle = "Not counted in travelers",
                checked = hasPets,
                onCheckedChange = {
                    hasPets = it
                    viewModel.pets = if (it) 1 else 0
                }
            )
            if (hasPets) {
                Spacer(Modifier.height(8.dp))
                S3InlineStepper(
                    count = viewModel.pets,
                    onDecrement = { if (viewModel.pets > 1) viewModel.pets-- },
                    onIncrement = { viewModel.pets++ },
                    label = "pets"
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Next button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TcButton(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue to Budget", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
}

@Composable
private fun S3InlineStepper(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerLow)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$count $label",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DeepSea5
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TripWizardColors.SurfaceBright)
                    .clickable { onDecrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, null, tint = TripWizardColors.Blue, modifier = Modifier.size(18.dp))
            }
            Text(
                count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepSea5,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TripWizardColors.Blue)
                    .clickable { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color(0xFF001627), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun S3ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TripWizardColors.ContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(TripWizardColors.ContainerHighest, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = TripWizardColors.PrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepSea5)
                Text(subtitle, fontSize = 10.sp, color = TripWizardColors.OnSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF001627),
                checkedTrackColor = TripWizardColors.Blue,
                uncheckedThumbColor = TripWizardColors.OnSurfaceVariant,
                uncheckedTrackColor = TripWizardColors.ContainerHighest
            )
        )
    }
}

