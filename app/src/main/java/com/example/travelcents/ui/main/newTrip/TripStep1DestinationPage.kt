package com.example.travelcents.ui.main.newTrip

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.example.travelcents.R
import com.example.travelcents.ui.components.TcButton
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea5
import com.example.travelcents.ui.theme.TravelCentsFonts
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale


private data class PopularDestination(val name: String, val tagline: String, val imageRes: Int)

private val popularDestinations = listOf(
    PopularDestination("Paris",    "The city of light and romance.",    R.drawable.dest_paris),
    PopularDestination("Tokyo",    "Futuristic energy meets tradition.", R.drawable.dest_tokyo),
    PopularDestination("Bali",     "Serene island paradise.",            R.drawable.dest_bali),
    PopularDestination("New York", "The city that never sleeps.",        R.drawable.dest_new_york),
    PopularDestination("London",   "A blend of history and modernity.",  R.drawable.dest_london),
    PopularDestination("Dubai",    "Sky-high luxury and desert gold.",   R.drawable.dest_dubai)
)

@Composable
fun TripStep1DestinationPage(
    modifier: Modifier = Modifier,
    viewModel: NewTripViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            getCurrentLocation(context, fusedLocationClient) { city ->
                viewModel.origin = city
            }
        }
    }

    ProvideTextStyle(value = TextStyle(fontFamily = TravelCentsFonts.Body)) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSea1)
        ) {
        WizardHeader(
            currentStep = 1,
            title = "Where to?",
            onBack = onBackClick,
            onClose = onCloseClick
        )

        // Scrollable content
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "DESTINATION SELECTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TripWizardColors.Blue,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose your destination, TravelCents will handle the rest.",
                        fontSize = 14.sp,
                        color = TripWizardColors.OnSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Destination search field with autocomplete
            item(span = { GridItemSpan(2) }) {
                Column {
                    Box {
                        TextField(
                            value = viewModel.destination,
                            onValueChange = {
                                viewModel.updateDestination(it)
                                isExpanded = viewModel.filteredDestinations.isNotEmpty()
                            },
                            placeholder = {
                                Text(
                                    "Search for a destination...",
                                    color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            },
                            label = { Text("Destination", color = TripWizardColors.OnSurfaceVariant, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TripWizardColors.Blue,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = DeepSea5,
                                unfocusedTextColor = DeepSea5,
                                cursorColor = TripWizardColors.Blue,
                                focusedContainerColor = TripWizardColors.SurfaceBright,
                                unfocusedContainerColor = TripWizardColors.SurfaceBright,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = TripWizardColors.Blue,
                                unfocusedLabelColor = TripWizardColors.OnSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(TripWizardColors.SurfaceBright)
                                .border(1.dp, TripWizardColors.Blue.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            viewModel.filteredDestinations.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, color = DeepSea5) },
                                    onClick = {
                                        viewModel.destination = suggestion
                                        isExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = DeepSea5
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = "Enter the city you want to visit.",
                        fontSize = 11.sp,
                        color = TripWizardColors.OnSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Origin field
            item(span = { GridItemSpan(2) }) {
                Column {
                    TextField(
                        value = viewModel.origin,
                        onValueChange = { viewModel.origin = it },
                        placeholder = {
                            Text(
                                "e.g. Los Angeles, CA",
                                color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        },
                        label = { Text("Origin", color = TripWizardColors.OnSurfaceVariant, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TripWizardColors.Blue,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (hasLocationPermission(context)) {
                                    getCurrentLocation(context, fusedLocationClient) { city ->
                                        viewModel.origin = city
                                    }
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }) {
                                Icon(Icons.Default.MyLocation, "GPS", tint = TripWizardColors.Blue)
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = DeepSea5,
                            unfocusedTextColor = DeepSea5,
                            cursorColor = TripWizardColors.Blue,
                            focusedContainerColor = TripWizardColors.SurfaceBright,
                            unfocusedContainerColor = TripWizardColors.SurfaceBright,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = TripWizardColors.Blue,
                            unfocusedLabelColor = TripWizardColors.OnSurfaceVariant
                        )
                    )
                    Text(
                        text = "Enter the city you're departing from.",
                        fontSize = 11.sp,
                        color = TripWizardColors.OnSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Popular destinations header
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Popular Destinations",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSea5
                    )
                    Text(
                        text = "Curated picks for your next adventure",
                        fontSize = 12.sp,
                        color = TripWizardColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Check if selected/typed destination matches a popular card
            val matchedPopular = popularDestinations.find { it.name.contains(viewModel.destination, ignoreCase = true) && viewModel.destination.isNotBlank() }
            val isCustom = viewModel.destination.isNotBlank() && matchedPopular == null

            // Destination cards
            items(popularDestinations) { dest ->
                DestinationCard(
                    destination = dest,
                    isSelected = viewModel.destination.contains(dest.name, ignoreCase = true) && viewModel.destination.isNotBlank(),
                    onClick = { viewModel.destination = dest.name }
                )
            }

            // Fallback card for unknown destination
            if (isCustom) {
                item(span = { GridItemSpan(1) }) {
                    CustomDestinationCard(name = viewModel.destination)
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Sticky continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepSea1)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TcButton(
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.destination.isNotBlank() && viewModel.origin.isNotBlank()
            ) {
                Text(text = "Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}

private fun hasLocationPermission(context: Context): Boolean {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return hasFineLocation || hasCoarseLocation
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onCityFound: (String) -> Unit
) {
    if (!hasLocationPermission(context)) return

    fun resolveCity(lat: Double, lon: Double) {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1) { addresses ->
                val formatted = addresses.firstOrNull()?.let { formatOriginFromAddress(it) }
                if (formatted != null) onCityFound(formatted)
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val formatted = addresses?.firstOrNull()?.let { formatOriginFromAddress(it) }
            if (formatted != null) onCityFound(formatted)
        }
    }

    // Use last known location for instant response, only request fresh fix if nothing cached
    fusedLocationClient.lastLocation.addOnSuccessListener { last ->
        if (last != null) {
            resolveCity(last.latitude, last.longitude)
        } else {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) resolveCity(location.latitude, location.longitude)
            }
        }
    }
}

private fun formatOriginFromAddress(address: Address): String? {
    val cityLike = listOfNotNull(
        address.locality,
        address.subAdminArea,
        address.adminArea,
        address.featureName
    ).firstOrNull { it.isNotBlank() }

    val regionLike = listOfNotNull(
        address.adminArea,
        address.countryName
    ).firstOrNull { it.isNotBlank() && it != cityLike }

    return when {
        cityLike == null && regionLike == null -> null
        cityLike != null && regionLike != null -> "$cityLike, $regionLike"
        else -> cityLike ?: regionLike
    }
}

@Composable
private fun CustomDestinationCard(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh)
            .border(2.dp, TripWizardColors.Blue, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(TripWizardColors.Blue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = TripWizardColors.Blue, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSea5,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Custom destination",
                fontSize = 10.sp,
                color = TripWizardColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun DestinationCard(
    destination: PopularDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(16.dp))
            .background(TripWizardColors.ContainerHigh)
            .then(
                if (isSelected) Modifier.border(2.dp, TripWizardColors.Blue, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(destination.imageRes),
            contentDescription = destination.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000)),
                        startY = 300f,
                        endY = Float.MAX_VALUE
                    )
                )
        )
        // Selected tint
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TripWizardColors.Blue.copy(alpha = 0.15f))
            )
        }
        // Labels
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = destination.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSea5
            )
            Text(
                text = destination.tagline,
                fontSize = 10.sp,
                color = TripWizardColors.OnSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
