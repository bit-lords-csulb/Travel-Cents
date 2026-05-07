package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.components.TcButton

internal enum class ItineraryActionEmphasis {
    Primary,
    Secondary
}

@Composable
internal fun ItineraryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasis: ItineraryActionEmphasis = ItineraryActionEmphasis.Primary,
    accent: Color = CardLavender,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
) {
    TcButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        border = if (emphasis == ItineraryActionEmphasis.Secondary) {
            BorderStroke(1.dp, accent.copy(alpha = 0.55f))
        } else {
            null
        },
        colors = if (emphasis == ItineraryActionEmphasis.Secondary) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = CardSurfaceHigh,
                contentColor = accent
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = CardBackground,
                disabledContainerColor = accent.copy(alpha = 0.45f),
                disabledContentColor = CardBackground.copy(alpha = 0.7f)
            )
        },
        contentPadding = contentPadding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
