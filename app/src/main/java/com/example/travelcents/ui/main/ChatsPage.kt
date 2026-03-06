package com.example.travelcents.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5

@Composable
fun ChatsPage(modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSea1),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.21f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(color = DeepSea2)
                .padding(top = 40.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "Chats",
                fontSize = 18.sp,
                color = DeepSea5
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Chats",
                    fontSize = 18.sp,
                    color = DeepSea5
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = "", // Connect this to a remember { mutableStateOf("") }
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(48.dp)),
                    placeholder = {
                        Text("Search conversations...", color = DeepSea5.copy(alpha = 0.7f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = DeepSea5)
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeepSea3,
                        unfocusedContainerColor = DeepSea3,
                        disabledContainerColor = DeepSea3,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = DeepSea5
                    )
                )

            }
        }
    }
}
