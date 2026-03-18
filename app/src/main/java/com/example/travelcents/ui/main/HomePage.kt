package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    currencyViewModel: CurrencyViewModel = viewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSea1)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "HOME",
            color = DeepSea4,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        CurrencyConverterCard(viewModel = currencyViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterCard(viewModel: CurrencyViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSea2)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "CURRENCY CONVERTER",
                color = DeepSea4,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Source row: amount input + from-currency dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.amount,
                    onValueChange = viewModel::onAmountChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DeepSea5,
                        unfocusedTextColor = DeepSea5,
                        focusedBorderColor = DeepSea3,
                        unfocusedBorderColor = DeepSea3,
                        cursorColor = DeepSea5,
                        focusedContainerColor = DeepSea1,
                        unfocusedContainerColor = DeepSea1
                    )
                )
                CurrencyDropdown(
                    selected = viewModel.fromCurrency,
                    currencies = viewModel.currencies,
                    recentCurrencies = viewModel.recentCurrencies,
                    onSelect = viewModel::onFromCurrencyChange
                )
            }

            // Swap button centered between the two rows
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = viewModel::swap,
                    modifier = Modifier
                        .size(40.dp)
                        .background(DeepSea3, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap currencies",
                        tint = DeepSea5
                    )
                }
            }

            // Target row: result display + to-currency dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Read-only result box styled to match the amount TextField
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .background(DeepSea1, RoundedCornerShape(4.dp))
                        .border(1.dp, DeepSea3, RoundedCornerShape(4.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when {
                        viewModel.isLoading -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = DeepSea4,
                            strokeWidth = 2.dp
                        )
                        viewModel.error != null -> Text(
                            text = viewModel.error!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp
                        )
                        viewModel.result != null -> Text(
                            text = "%.2f".format(viewModel.result),
                            color = DeepSea5,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        else -> Text("—", color = DeepSea4, fontSize = 16.sp)
                    }
                }
                CurrencyDropdown(
                    selected = viewModel.toCurrency,
                    currencies = viewModel.currencies,
                    recentCurrencies = viewModel.recentCurrencies,
                    onSelect = viewModel::onToCurrencyChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: String,
    currencies: List<String>,
    recentCurrencies: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Search query lives here — reset to empty each time the menu closes
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(expanded) { if (!expanded) searchQuery = "" }

    val filtered = remember(searchQuery, currencies) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    // Only show recents section when not actively searching
    val showRecents = searchQuery.isBlank() && recentCurrencies.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // When expanded, this TextField doubles as the search input
        OutlinedTextField(
            value = if (expanded) searchQuery else selected,
            onValueChange = { searchQuery = it },
            singleLine = true,
            placeholder = {
                if (expanded) Text("Search...", color = DeepSea4, fontSize = 12.sp)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .width(110.dp)
                // PrimaryEditable lets the keyboard appear so the user can type to filter
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DeepSea5,
                unfocusedTextColor = DeepSea5,
                focusedBorderColor = DeepSea3,
                unfocusedBorderColor = DeepSea3,
                cursorColor = DeepSea5,
                focusedContainerColor = DeepSea1,
                unfocusedContainerColor = DeepSea1,
                focusedTrailingIconColor = DeepSea4,
                unfocusedTrailingIconColor = DeepSea4
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Fixed height gives verticalScroll a bounded viewport so the list actually scrolls
            modifier = Modifier.height(300.dp),
            containerColor = DeepSea2
        ) {
            // Recent section — only visible when not filtering
            if (showRecents) {
                Text(
                    text = "RECENT",
                    color = DeepSea4,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                recentCurrencies.forEach { currency ->
                    CurrencyItem(currency = currency, onSelect = { onSelect(it); expanded = false })
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = DeepSea4.copy(alpha = 0.2f)
                )
            }

            // Full (or filtered) list
            filtered.forEach { currency ->
                CurrencyItem(currency = currency, onSelect = { onSelect(it); expanded = false })
            }
        }
    }
}

@Composable
private fun CurrencyItem(currency: String, onSelect: (String) -> Unit) {
    DropdownMenuItem(
        text = { Text(text = currency, color = DeepSea5, fontSize = 12.sp) },
        onClick = { onSelect(currency) },
        // Tighter vertical padding so more items fit on screen
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    )
}
