package com.example.travelcents.ui.main.current

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3

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
        color = DeepSea2.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, DeepSea3.copy(alpha = 0.75f))
    ) {
        content()
    }
}
