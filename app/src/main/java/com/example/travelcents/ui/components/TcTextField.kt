package com.example.travelcents.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.main.newTrip.TripWizardColors
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun TcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    textFontFamily: FontFamily? = null,
    labelFontFamily: FontFamily? = null,
    placeholderFontFamily: FontFamily? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label.isNotBlank()) {
            {
                Text(
                    text = label,
                    color = TripWizardColors.OnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = labelFontFamily
                )
            }
        } else {
            null
        },
        placeholder = if (placeholder.isNotEmpty()) {
            {
                Text(
                    text = placeholder,
                    color = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontFamily = placeholderFontFamily
                )
            }
        } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        textStyle = TextStyle(
            color = DeepSea5,
            fontSize = 16.sp,
            fontFamily = textFontFamily
        ),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = DeepSea5,
            unfocusedTextColor = DeepSea5,
            disabledTextColor = DeepSea5.copy(alpha = 0.5f),
            cursorColor = TripWizardColors.Blue,
            focusedContainerColor = TripWizardColors.SurfaceBright,
            unfocusedContainerColor = TripWizardColors.SurfaceBright,
            disabledContainerColor = TripWizardColors.ContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = TripWizardColors.Blue,
            unfocusedLabelColor = TripWizardColors.OnSurfaceVariant,
            disabledLabelColor = TripWizardColors.OnSurfaceVariant.copy(alpha = 0.5f),
        )
    )
}

@Composable
fun TcCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    textStyle: TextStyle = TextStyle(color = DeepSea5, fontSize = 12.sp),
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = TripWizardColors.SurfaceBright,
    placeholderColor: Color = TripWizardColors.OnSurfaceVariant,
    cursorColor: Color = TripWizardColors.Blue,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
) {
    Row(
        modifier = modifier
            .background(containerColor, shape)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leadingIcon?.invoke()

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = placeholderColor,
                    fontSize = textStyle.fontSize
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = textStyle,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(cursorColor),
                modifier = Modifier.fillMaxWidth()
            )
        }

        trailingIcon?.invoke()
    }
}

