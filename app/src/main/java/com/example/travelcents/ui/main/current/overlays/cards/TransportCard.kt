package com.example.travelcents.ui.main.current.overlays.cards

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.data.trip.model.ATTR_LYFT_DEEPLINK
import com.example.travelcents.data.trip.model.ATTR_RIDESHARE_ESTIMATE_USD
import com.example.travelcents.data.trip.model.ATTR_RIDESHARE_MIN
import com.example.travelcents.data.trip.model.ATTR_TRANSIT_MIN
import com.example.travelcents.data.trip.model.ATTR_TRANSPORT_ANCHOR_LABEL
import com.example.travelcents.data.trip.model.ATTR_UBER_DEEPLINK
import com.example.travelcents.data.trip.model.ATTR_WALK_MIN
import com.example.travelcents.data.trip.model.TravelEvent
import com.example.travelcents.data.trip.model.detailValue

@Composable
fun TransportCard(
    event: TravelEvent,
    titleOverride: String? = null,
    eyebrowOverride: String = "Getting there",
    showDirectionsLink: Boolean = true,
    prioritizeRideshare: Boolean = false,
    accentOverride: androidx.compose.ui.graphics.Color = CardCoral
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val anchorLabel = event.detailValue(ATTR_TRANSPORT_ANCHOR_LABEL)?.takeIf { it.isNotBlank() }
        ?: "From your current stop"
    val walkMin = event.detailValue(ATTR_WALK_MIN)?.toIntOrNull()?.takeIf { it > 0 }
    val transitMin = event.detailValue(ATTR_TRANSIT_MIN)?.toIntOrNull()?.takeIf { it > 0 }
    val rideshareMin = event.detailValue(ATTR_RIDESHARE_MIN)?.toIntOrNull()?.takeIf { it > 0 }
    val rideshareEstimate = event.detailValue(ATTR_RIDESHARE_ESTIMATE_USD)
        ?.takeIf { it.isNotBlank() }
    val uberDeeplink = event.detailValue(ATTR_UBER_DEEPLINK)?.takeIf { it.isNotBlank() }
    val lyftDeeplink = event.detailValue(ATTR_LYFT_DEEPLINK)?.takeIf { it.isNotBlank() }
    val hasRideshare = rideshareMin != null ||
        rideshareEstimate != null ||
        uberDeeplink != null ||
        lyftDeeplink != null

    if (walkMin == null && transitMin == null && !hasRideshare) return

    DetailCardFrame(accent = accentOverride) {
        DetailCardHeader(
            eyebrow = eyebrowOverride,
            title = titleOverride ?: anchorLabel
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (prioritizeRideshare && hasRideshare) {
                TransportRideshareRow(
                    rideshareMin = rideshareMin,
                    rideshareEstimate = rideshareEstimate,
                    uberDeeplink = uberDeeplink,
                    lyftDeeplink = lyftDeeplink,
                    accent = accentOverride,
                    onOpenUber = uberDeeplink?.let { { openTransportProvider(context, "com.ubercab", it) } },
                    onOpenLyft = lyftDeeplink?.let { { openTransportProvider(context, "me.lyft.android", it) } }
                )
            }
            walkMin?.let {
                TransportModeRow(
                    label = "Walk",
                    value = "$it min"
                )
            }
            transitMin?.let {
                TransportModeRow(
                    label = "Transit",
                    value = "$it min"
                )
            }
            if (hasRideshare && !prioritizeRideshare) {
                TransportRideshareRow(
                    rideshareMin = rideshareMin,
                    rideshareEstimate = rideshareEstimate,
                    uberDeeplink = uberDeeplink,
                    lyftDeeplink = lyftDeeplink,
                    accent = accentOverride,
                    onOpenUber = uberDeeplink?.let { { openTransportProvider(context, "com.ubercab", it) } },
                    onOpenLyft = lyftDeeplink?.let { { openTransportProvider(context, "me.lyft.android", it) } }
                )
            }
        }
        if (showDirectionsLink) {
            Spacer(modifier = Modifier.height(14.dp))
            DetailLinkRow(
                label = "Directions",
                value = "Open Google Maps",
                onClick = { uriHandler.openUri(googleMapsDirectionsUrl(eventMapsQuery(event))) },
                accent = accentOverride
            )
        }
    }
}

@Composable
private fun TransportModeRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = label.uppercase(),
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = value,
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TransportRideshareRow(
    rideshareMin: Int?,
    rideshareEstimate: String?,
    uberDeeplink: String?,
    lyftDeeplink: String?,
    accent: androidx.compose.ui.graphics.Color,
    onOpenUber: (() -> Unit)?,
    onOpenLyft: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurfaceHigh, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "RIDESHARE",
                color = CardTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = listOfNotNull(
                    rideshareMin?.let { "About $it min" },
                    rideshareEstimate
                ).joinToString(" · ").ifBlank { "Open a rideshare app" },
                color = CardText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uberDeeplink != null && onOpenUber != null) {
                TransportActionChip(
                    label = "Uber",
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenUber
                )
            }
            if (lyftDeeplink != null && onOpenLyft != null) {
                TransportActionChip(
                    label = "Lyft",
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenLyft
                )
            }
        }
    }
}

@Composable
private fun TransportActionChip(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun openTransportProvider(
    context: Context,
    packageName: String,
    uri: String
) {
    val parsedUri = runCatching { Uri.parse(uri) }.getOrNull() ?: return
    val packageManager = context.packageManager
    val fallbackIntent = Intent(Intent.ACTION_VIEW, parsedUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val appIntent = Intent(fallbackIntent).setPackage(packageName)

    try {
        when {
            appIntent.resolveActivity(packageManager) != null -> context.startActivity(appIntent)
            fallbackIntent.resolveActivity(packageManager) != null -> context.startActivity(fallbackIntent)
        }
    } catch (_: ActivityNotFoundException) {
        if (fallbackIntent.resolveActivity(packageManager) != null) {
            runCatching { context.startActivity(fallbackIntent) }
        }
    } catch (_: Exception) {
        if (fallbackIntent.resolveActivity(packageManager) != null) {
            runCatching { context.startActivity(fallbackIntent) }
        }
    }
}
