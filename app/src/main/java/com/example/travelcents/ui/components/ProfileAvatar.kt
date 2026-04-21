package com.example.travelcents.ui.components

import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

private const val TAG = "ProfileAvatar"

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
    val hasUrl = photoUrl.isNotBlank()

    LaunchedEffect(photoUrl, isLoading) {
        Log.d(
            TAG,
            "render: photoUrl='${photoUrl.take(120)}${if (photoUrl.length > 120) "…" else ""}' " +
                    "hasUrl=$hasUrl isLoading=$isLoading"
        )
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            !hasUrl && isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(iconSize + 6.dp),
                    color = placeholderTint,
                    strokeWidth = 2.dp
                )
            }

            !hasUrl -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription,
                    tint = placeholderTint,
                    modifier = Modifier.size(iconSize)
                )
            }

            else -> {
                val request = remember(photoUrl) {
                    ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .listener(
                            onStart = { Log.d(TAG, "Coil start: $photoUrl") },
                            onSuccess = { _, result ->
                                Log.d(
                                    TAG,
                                    "Coil success: $photoUrl dataSource=${result.dataSource}"
                                )
                            },
                            onError = { _, result ->
                                Log.w(
                                    TAG,
                                    "Coil error: $photoUrl throwable=${result.throwable.javaClass.simpleName}: ${result.throwable.message}"
                                )
                            },
                            onCancel = { Log.d(TAG, "Coil cancel: $photoUrl") }
                        )
                        .build()
                }

                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize + 6.dp),
                            color = placeholderTint,
                            strokeWidth = 2.dp
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = contentDescription,
                            tint = placeholderTint,
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    success = { SubcomposeAsyncImageContent() }
                )
            }
        }
    }
}

@Suppress("unused")
private fun AsyncImagePainter.State.tag(): String = when (this) {
    is AsyncImagePainter.State.Empty -> "Empty"
    is AsyncImagePainter.State.Loading -> "Loading"
    is AsyncImagePainter.State.Success -> "Success"
    is AsyncImagePainter.State.Error -> "Error(${result.throwable.javaClass.simpleName})"
}