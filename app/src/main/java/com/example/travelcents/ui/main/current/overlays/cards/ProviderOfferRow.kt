package com.example.travelcents.ui.main.current.overlays.cards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import java.util.Locale

@Composable
fun ProviderOfferRow(
    source: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CardLavender,
    actionLabel: String = "Open",
    logoUrl: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProviderOfferLogo(
            logoUrl = logoUrl,
            source = source,
            accent = accent
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source,
                color = CardText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = CardTextMuted,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        ItineraryActionButton(
            label = actionLabel,
            onClick = onClick,
            accent = accent,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ProviderOfferLogo(
    logoUrl: String?,
    source: String,
    accent: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBackground),
        contentAlignment = Alignment.Center
    ) {
        val safeLogoUrl = logoUrl?.takeIf { it.isNotBlank() }
        if (safeLogoUrl != null) {
            val painter = rememberAsyncImagePainter(model = safeLogoUrl)
            if (painter.state is AsyncImagePainter.State.Error) {
                ProviderOfferLogoFallback(source = source, accent = accent)
            } else {
                Image(
                    painter = painter,
                    contentDescription = source,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            ProviderOfferLogoFallback(source = source, accent = accent)
        }
    }
}

@Composable
private fun ProviderOfferLogoFallback(
    source: String,
    accent: Color
) {
    val initial = source.firstOrNull()?.titlecase(Locale.US) ?: "?"
    if (initial == "?") {
        Icon(
            imageVector = Icons.Default.Storefront,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
    } else {
        Text(
            text = initial,
            color = accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
