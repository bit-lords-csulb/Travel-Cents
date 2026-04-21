package com.example.travelcents.ui.modules

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.travelcents.data.media.ImageCacheManager
import com.example.travelcents.data.trip.model.EventOption
import com.example.travelcents.data.trip.model.TravelEvent

fun TravelEvent.heroImageModel(context: Context): String {
    val remoteHero = imageUrl.ifBlank { details["imageUrl"] ?: details["image_url"] ?: "" }
    return localImagePath.ifBlank { cachedOrRemoteModelFor(context, itineraryId, remoteHero).orEmpty() }
}

fun TravelEvent.galleryPhotoModels(context: Context): List<String> {
    val remoteHero = imageUrl.ifBlank { details["imageUrl"] ?: details["image_url"] ?: "" }
    val preferredHero = localImagePath.ifBlank {
        cachedOrRemoteModelFor(context, itineraryId, remoteHero).orEmpty()
    }
    return buildList {
        if (preferredHero.isNotBlank()) add(preferredHero)
        photoUrls
            .filter { it.isNotBlank() && it != remoteHero }
            .map { cachedOrRemoteModelFor(context, itineraryId, it).orEmpty() }
            .forEach(::add)
    }.distinct()
}

fun EventOption.heroImageModel(
    context: Context,
    tripId: String
): String {
    return localImagePath.ifBlank { cachedOrRemoteModelFor(context, tripId, imageUrl).orEmpty() }
}

fun EventOption.galleryPhotoModels(
    context: Context,
    tripId: String
): List<String> {
    val preferredHero = heroImageModel(context, tripId)
    return buildList {
        if (preferredHero.isNotBlank()) add(preferredHero)
        photoUrls
            .filter { it.isNotBlank() && it != imageUrl }
            .map { cachedOrRemoteModelFor(context, tripId, it).orEmpty() }
            .forEach(::add)
    }.distinct()
}

private fun cachedOrRemoteModelFor(
    context: Context,
    tripId: String,
    remoteUrl: String
): String? = ImageCacheManager.resolveCachedMediaUrl(context, tripId, remoteUrl)

@Composable
fun PhotoGalleryButton(
    photoCount: Int,
    onClick: () -> Unit,
    containerAlpha: Float = 0.48f,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = containerAlpha),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        IconButton(onClick = onClick) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Open photo gallery",
                    tint = Color.White
                )
                if (photoCount > 1) {
                    Text(
                        text = photoCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TripPhotoGalleryDialog(
    photos: List<String>,
    onDismiss: () -> Unit,
    initialPage: Int = 0
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = photos[page],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close gallery",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

