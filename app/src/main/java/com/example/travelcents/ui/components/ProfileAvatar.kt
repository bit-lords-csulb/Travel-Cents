package com.example.travelcents.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun ProfileAvatar(
    photoUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    borderColor: Color,
    backgroundColor: Color,
    placeholderTint: Color,
    borderWidth: Dp = 1.5.dp,
    isLoading: Boolean = false,
    iconSize: Dp = 22.dp
) {
    val context = LocalContext.current
    val imageRequest = remember(photoUrl) {
        photoUrl.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build()
        }
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val painterState = painter.state

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize + 6.dp),
                    color = placeholderTint,
                    strokeWidth = 2.dp
                )
            }

            painterState is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize + 6.dp),
                    color = placeholderTint,
                    strokeWidth = 2.dp
                )
            }

            imageRequest == null || painterState is AsyncImagePainter.State.Error -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription,
                    tint = placeholderTint,
                    modifier = Modifier.size(iconSize)
                )
            }

            else -> {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

