package com.example.travelcents.ui.main.newTrip

import androidx.compose.ui.graphics.Color
import com.example.travelcents.ui.theme.TravelCentsPaletteState

object TripWizardColors {
    val Blue: Color
        get() = TravelCentsPaletteState.currentPalette.blue

    val PrimaryContainer: Color
        get() = TravelCentsPaletteState.currentPalette.primaryContainer

    val ContainerHigh: Color
        get() = TravelCentsPaletteState.currentPalette.containerHigh

    val ContainerHighest: Color
        get() = TravelCentsPaletteState.currentPalette.containerHighest

    val ContainerLow: Color
        get() = TravelCentsPaletteState.currentPalette.containerLow

    val SurfaceBright: Color
        get() = TravelCentsPaletteState.currentPalette.surfaceBright

    val OnSurfaceVariant: Color
        get() = TravelCentsPaletteState.currentPalette.onSurfaceVariant

    val SecondaryContainer: Color
        get() = TravelCentsPaletteState.currentPalette.secondaryContainer
}
