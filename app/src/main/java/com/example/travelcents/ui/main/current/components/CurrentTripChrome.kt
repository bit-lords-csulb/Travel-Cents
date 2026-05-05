package com.example.travelcents.ui.main.current

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.travelcents.ui.main.newTrip.TripWizardColors

internal val CurrentTripPageShape = RoundedCornerShape(24.dp)
internal val CurrentTripInnerShape = RoundedCornerShape(18.dp)

@Composable
internal fun CurrentTripPageSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CurrentTripPageShape,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = TripWizardColors.ContainerLow.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, TripWizardColors.OnSurfaceVariant.copy(alpha = 0.18f))
    ) {
        content()
    }
}
