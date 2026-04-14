package com.example.travelcents.ui.main.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.components.TcCompactTextField
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import java.util.Locale

private val Primary = Color(0xFF64B5F6)
private val SurfaceBright = Color(0xFF243447)

private fun formatConvertedAmount(value: Double): String =
    String.format(Locale.US, "%.2f", value)
        .trimEnd('0')
        .trimEnd('.')

private fun convertedAmountFontSize(text: String) = when {
    text.length >= 14 -> 13.sp
    text.length >= 11 -> 15.sp
    else -> 17.sp
}

// Maps currency codes to their display symbols
internal fun currencySymbol(code: String): String = when (code) {
    "USD" -> "$"; "CAD" -> "CA$"; "AUD" -> "A$"; "NZD" -> "NZ$"
    "SGD" -> "S$"; "HKD" -> "HK$"; "MXN" -> "MX$"
    "EUR" -> "€"
    "GBP" -> "£"
    "JPY" -> "¥"; "CNY" -> "¥"
    "KRW" -> "₩"
    "INR" -> "₹"
    "BRL" -> "R$"
    "TRY" -> "₺"
    "ILS" -> "₪"
    "PHP" -> "₱"
    "THB" -> "฿"
    "PLN" -> "zł"
    "ZAR" -> "R"
    "IDR" -> "Rp"
    "MYR" -> "RM"
    "CHF" -> "Fr"
    "SEK" -> "kr"; "NOK" -> "kr"; "DKK" -> "kr"; "ISK" -> "kr"
    "HUF" -> "Ft"
    "CZK" -> "Kč"
    "RON" -> "lei"
    "BGN" -> "лв"
    else -> ""
}

// Full names used for search filtering inside the dropdown
private val currencyNames = mapOf(
    "AUD" to "Australian Dollar",
    "BGN" to "Bulgarian Lev",
    "BRL" to "Brazilian Real",
    "CAD" to "Canadian Dollar",
    "CHF" to "Swiss Franc",
    "CNY" to "Chinese Yuan",
    "CZK" to "Czech Koruna",
    "DKK" to "Danish Krone",
    "EUR" to "Euro",
    "GBP" to "British Pound",
    "HKD" to "Hong Kong Dollar",
    "HUF" to "Hungarian Forint",
    "IDR" to "Indonesian Rupiah",
    "ILS" to "Israeli Shekel",
    "INR" to "Indian Rupee",
    "ISK" to "Icelandic Króna",
    "JPY" to "Japanese Yen",
    "KRW" to "South Korean Won",
    "MXN" to "Mexican Peso",
    "MYR" to "Malaysian Ringgit",
    "NOK" to "Norwegian Krone",
    "NZD" to "New Zealand Dollar",
    "PHP" to "Philippine Peso",
    "PLN" to "Polish Zloty",
    "RON" to "Romanian Leu",
    "SEK" to "Swedish Krona",
    "SGD" to "Singapore Dollar",
    "THB" to "Thai Baht",
    "TRY" to "Turkish Lira",
    "USD" to "US Dollar",
    "ZAR" to "South African Rand"
)

@Composable
fun CurrencyConverterCard(
    modifier: Modifier = Modifier,
    viewModel: CurrencyViewModel = viewModel()
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Currency",
                    color = DeepSea5,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = viewModel::swap,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FROM row
            CurrencyRow(
                currencyCode = viewModel.fromCurrency,
                currencies = viewModel.currencies,
                labelColor = DeepSea4,
                background = SurfaceBright,
                borderColor = Color.Transparent,
                onSelect = viewModel::onFromCurrencyChange
            ) {
                val sym = currencySymbol(viewModel.fromCurrency)
                CurrencyAmountInput(
                    symbol = sym,
                    symbolColor = DeepSea4,
                    value = viewModel.amount,
                    onValueChange = viewModel::onAmountChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TO row
            CurrencyRow(
                currencyCode = viewModel.toCurrency,
                currencies = viewModel.currencies,
                labelColor = Primary,
                background = Primary.copy(alpha = 0.1f),
                borderColor = Primary.copy(alpha = 0.25f),
                onSelect = viewModel::onToCurrencyChange
            ) {
                val sym = currencySymbol(viewModel.toCurrency)
                when {
                    viewModel.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Primary,
                        strokeWidth = 2.dp
                    )
                    viewModel.error != null -> Text(
                        text = "—",
                        color = Color(0xFFFF6B6B),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    viewModel.result != null -> {
                        val convertedAmount = viewModel.result ?: return@CurrencyRow
                        val amountText = formatConvertedAmount(convertedAmount)

                        CurrencyAmountDisplay(
                            symbol = sym,
                            symbolColor = Primary.copy(alpha = 0.7f),
                            amountText = amountText,
                            amountColor = Primary,
                            amountFontSize = convertedAmountFontSize(amountText)
                        )
                    }
                    else -> Text(
                        text = "—",
                        color = Primary.copy(alpha = 0.5f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrencyAmountInput(
    symbol: String,
    symbolColor: Color,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbol.isNotEmpty()) {
            Text(
                text = symbol,
                color = symbolColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        Box(modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 24.dp, max = 96.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(
                    color = DeepSea5,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                ),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CurrencyAmountDisplay(
    symbol: String,
    symbolColor: Color,
    amountText: String,
    amountColor: Color,
    amountFontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbol.isNotEmpty()) {
            Text(
                text = symbol,
                color = symbolColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(2.dp))
        }

        Text(
            text = amountText,
            color = amountColor,
            fontSize = amountFontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

// Row layout shared by FROM and TO — embeds the dropdown trigger on the left
@Composable
private fun CurrencyRow(
    currencyCode: String,
    currencies: List<String>,
    labelColor: Color,
    background: Color,
    borderColor: Color,
    onSelect: (String) -> Unit,
    valueContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CurrencyDropdown(
            selected = currencyCode,
            currencies = currencies,
            labelColor = labelColor,
            onSelect = onSelect
        )
        Box(
            modifier = Modifier
                .height(14.dp)
                .width(1.dp)
                .background(labelColor.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            valueContent()
        }
    }
}

// Compact dropdown: code + arrow icon as trigger, popup with search + scrollable list
@Composable
private fun CurrencyDropdown(
    selected: String,
    currencies: List<String>,
    labelColor: Color,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filtered = remember(query) {
        if (query.isBlank()) currencies
        else currencies.filter { code ->
            code.contains(query, ignoreCase = true) ||
                currencyNames[code]?.contains(query, ignoreCase = true) == true
        }
    }

    Box {
        // Trigger: code label + small arrow
        Row(
            modifier = Modifier.clickable {
                query = ""
                expanded = true
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = selected,
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                modifier = Modifier.widthIn(min = 28.dp)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Pick currency",
                tint = labelColor,
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DeepSea2,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.width(220.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
                TcCompactTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search…",
                    textStyle = TextStyle(color = DeepSea5, fontSize = 12.sp),
                    containerColor = SurfaceBright,
                    placeholderColor = DeepSea4,
                    cursorColor = Primary,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DeepSea4,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable currency list with non-interactive scroll indicator
                val isScrolling = listState.isScrollInProgress
                val thumbAlpha by animateFloatAsState(
                    targetValue = if (isScrolling) 0.55f else 0f,
                    animationSpec = tween(durationMillis = if (isScrolling) 80 else 600),
                    label = "scrollbar"
                )

                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().height(220.dp).padding(end = 6.dp)
                    ) {
                        items(filtered, key = { it }) { code ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(code)
                                        expanded = false
                                    }
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currencySymbol(code).ifEmpty { " " },
                                    color = Primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.width(22.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = code,
                                    color = DeepSea5,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.width(36.dp)
                                )
                                currencyNames[code]?.let { name ->
                                    Text(
                                        text = name,
                                        color = DeepSea4,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            HorizontalDivider(color = DeepSea4.copy(alpha = 0.1f), thickness = 0.5.dp)
                        }
                    }

                    // Scrollbar thumb — visible only while scrolling
                    val totalItems = filtered.size
                    val visibleCount = 6
                    if (totalItems > visibleCount && thumbAlpha > 0f) {
                        val thumbHeight = (220f * visibleCount / totalItems).coerceAtLeast(20f)
                        val maxOffset = 220f - thumbHeight
                        // derivedStateOf avoids recomposition on every scroll frame
                        val scrollProgress by remember(totalItems) {
                            derivedStateOf {
                                listState.firstVisibleItemIndex.toFloat() /
                                    (totalItems - visibleCount).coerceAtLeast(1)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .width(2.dp)
                                .height(thumbHeight.dp)
                                .offset(y = (maxOffset * scrollProgress).dp)
                                .background(DeepSea4.copy(alpha = thumbAlpha), RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}
