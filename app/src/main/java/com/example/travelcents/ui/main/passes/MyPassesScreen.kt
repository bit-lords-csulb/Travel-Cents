package com.example.travelcents.ui.main.passes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcents.ui.theme.DeepSea1
import com.example.travelcents.ui.theme.DeepSea2
import com.example.travelcents.ui.theme.DeepSea3
import com.example.travelcents.ui.theme.DeepSea4
import com.example.travelcents.ui.theme.DeepSea5
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

private val CardTopBoarding = Color(0xFF415A77)
private val CardBottomBoarding = Color(0xFF1B263B)
private val CardTopConcert = Color(0xFF4A3060)
private val CardBottomConcert = Color(0xFF1E1030)
private val CardTopSports = Color(0xFF1A3A28)
private val CardBottomSports = Color(0xFF0D1F14)

sealed class PassItem {
    data class BoardingPass(
        val airline: String,
        val from: String,
        val to: String,
        val flight: String,
        val gate: String,
        val seat: String,
        val date: String,
        val time: String,
        val passenger: String,
        val confirmationCode: String,
        val qrSeed: Int
    ) : PassItem()

    data class ConcertTicket(
        val artist: String,
        val venue: String,
        val city: String,
        val date: String,
        val time: String,
        val section: String,
        val row: String,
        val seat: String,
        val holder: String,
        val confirmationCode: String,
        val qrSeed: Int
    ) : PassItem()

    data class SportsTicket(
        val homeTeam: String,
        val awayTeam: String,
        val venue: String,
        val date: String,
        val time: String,
        val section: String,
        val row: String,
        val seat: String,
        val holder: String,
        val confirmationCode: String,
        val qrSeed: Int
    ) : PassItem()
}

private fun buildPasses(name: String): List<PassItem> = listOf(
    PassItem.BoardingPass(
        airline = "Delta Airlines",
        from = "LAX",
        to = "JFK",
        flight = "DL 1234",
        gate = "B12",
        seat = "12A",
        date = "March 15, 2026",
        time = "14:30",
        passenger = name,
        confirmationCode = "ABC123XYZ",
        qrSeed = 7
    ),
    PassItem.ConcertTicket(
        artist = "Taylor Swift",
        venue = "SoFi Stadium",
        city = "Los Angeles, CA",
        date = "April 3, 2026",
        time = "7:00 PM",
        section = "Floor B",
        row = "12",
        seat = "22",
        holder = name,
        confirmationCode = "SWFT2026LA",
        qrSeed = 42
    ),
    PassItem.BoardingPass(
        airline = "United Airlines",
        from = "SFO",
        to = "HND",
        flight = "UA 567",
        gate = "G7",
        seat = "8F",
        date = "May 1, 2026",
        time = "11:45",
        passenger = name,
        confirmationCode = "UA567TOK",
        qrSeed = 13
    ),
    PassItem.SportsTicket(
        homeTeam = "Los Angeles Lakers",
        awayTeam = "Golden State Warriors",
        venue = "Crypto.com Arena",
        date = "April 12, 2026",
        time = "7:30 PM",
        section = "109",
        row = "8",
        seat = "14",
        holder = name,
        confirmationCode = "LAL2026GS",
        qrSeed = 29
    ),
    PassItem.ConcertTicket(
        artist = "Coldplay",
        venue = "Rose Bowl",
        city = "Pasadena, CA",
        date = "June 20, 2026",
        time = "8:00 PM",
        section = "GA2",
        row = "GA",
        seat = "GA",
        holder = name,
        confirmationCode = "CLDY2026PAS",
        qrSeed = 61
    ),
    PassItem.BoardingPass(
        airline = "JetBlue",
        from = "BOS",
        to = "MIA",
        flight = "B6 901",
        gate = "A18",
        seat = "4D",
        date = "July 2, 2026",
        time = "16:25",
        passenger = name,
        confirmationCode = "B6901SUN",
        qrSeed = 34
    )
)

@Composable
fun MyPassesScreen(onBack: () -> Unit) {
    val passengerName = remember {
        val user = FirebaseAuth.getInstance().currentUser
        (user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@"))
            ?.uppercase()
            ?: "TRAVELER"
    }
    val passes = remember(passengerName) { buildPasses(passengerName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSea1)
    ) {
        PassesHeader(onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(passes) { pass ->
                when (pass) {
                    is PassItem.BoardingPass -> BoardingPassCard(pass)
                    is PassItem.ConcertTicket -> ConcertTicketCard(pass)
                    is PassItem.SportsTicket -> SportsTicketCard(pass)
                }
            }
        }
    }
}

@Composable
private fun PassesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = DeepSea5
            )
        }
        Text(
            text = "My Documents",
            color = DeepSea5,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun BoardingPassCard(pass: PassItem.BoardingPass) {
    PassCard(topColor = CardTopBoarding, bottomColor = CardBottomBoarding) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PassLabel("BOARDING PASS")
                Text(pass.airline, color = DeepSea5, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            PassIconBadge(Icons.Outlined.AirplanemodeActive)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AirportCode(pass.from, "Departure")
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalDivider(color = DeepSea4, thickness = 1.dp)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = DeepSea4,
                    modifier = Modifier.size(18.dp)
                )
            }
            AirportCode(pass.to, "Arrival")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Flight", pass.flight)
            DetailBlock("Gate", pass.gate)
            DetailBlock("Seat", pass.seat)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Date", pass.date, bold = false)
            DetailBlock("Time", pass.time, bold = false)
        }
        DetailBlock("Passenger", pass.passenger)
        PassDivider()
        PassQrSection(confirmationCode = pass.confirmationCode, qrSeed = pass.qrSeed)
    }
}

@Composable
private fun ConcertTicketCard(pass: PassItem.ConcertTicket) {
    PassCard(topColor = CardTopConcert, bottomColor = CardBottomConcert) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PassLabel("CONCERT TICKET")
                Text(pass.artist, color = DeepSea5, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            PassIconBadge(Icons.Outlined.MusicNote)
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(pass.venue, color = DeepSea5, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(pass.city, color = DeepSea4, fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Date", pass.date, bold = false)
            DetailBlock("Doors", pass.time, bold = false)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Section", pass.section)
            DetailBlock("Row", pass.row)
            DetailBlock("Seat", pass.seat)
        }
        DetailBlock("Ticket Holder", pass.holder)
        PassDivider()
        PassQrSection(confirmationCode = pass.confirmationCode, qrSeed = pass.qrSeed)
    }
}

@Composable
private fun SportsTicketCard(pass: PassItem.SportsTicket) {
    PassCard(topColor = CardTopSports, bottomColor = CardBottomSports) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PassLabel("GAME TICKET")
                Text(pass.homeTeam, color = DeepSea5, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("vs ${pass.awayTeam}", color = DeepSea4, fontSize = 13.sp)
            }
            PassIconBadge(Icons.Outlined.Stadium)
        }

        Text(pass.venue, color = DeepSea5, fontSize = 14.sp, fontWeight = FontWeight.Medium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Date", pass.date, bold = false)
            DetailBlock("Time", pass.time, bold = false)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DetailBlock("Section", pass.section)
            DetailBlock("Row", pass.row)
            DetailBlock("Seat", pass.seat)
        }
        DetailBlock("Ticket Holder", pass.holder)
        PassDivider()
        PassQrSection(confirmationCode = pass.confirmationCode, qrSeed = pass.qrSeed)
    }
}

@Composable
private fun PassCard(
    topColor: Color,
    bottomColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content
    )
}

@Composable
private fun PassIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(DeepSea5, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepSea2,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AirportCode(code: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(code, color = DeepSea5, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(label, color = DeepSea4, fontSize = 12.sp)
    }
}

@Composable
private fun DetailBlock(label: String, value: String, bold: Boolean = true) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.widthIn(min = 80.dp)
    ) {
        PassLabel(label)
        Text(
            value,
            color = DeepSea5,
            fontSize = if (bold) 15.sp else 13.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PassLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = DeepSea4,
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun PassDivider() {
    HorizontalDivider(color = DeepSea3, thickness = 0.8.dp)
}

@Composable
private fun PassQrSection(confirmationCode: String, qrSeed: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FakeQrCode(seed = qrSeed)
        PassLabel("CONFIRMATION CODE")
        Text(
            text = confirmationCode,
            color = DeepSea5,
            fontSize = 13.sp,
            letterSpacing = 0.8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FakeQrCode(seed: Int) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cells = 21
            val cell = size.minDimension / cells

            fun drawFinder(startX: Int, startY: Int) {
                drawRect(Color.Black, Offset(startX * cell, startY * cell), Size(cell * 7, cell * 7))
                drawRect(Color.White, Offset((startX + 1) * cell, (startY + 1) * cell), Size(cell * 5, cell * 5))
                drawRect(Color.Black, Offset((startX + 2) * cell, (startY + 2) * cell), Size(cell * 3, cell * 3))
            }

            drawFinder(0, 0)
            drawFinder(14, 0)
            drawFinder(0, 14)

            for (x in 0 until cells) {
                for (y in 0 until cells) {
                    val inFinder = (x < 7 && y < 7) || (x >= 14 && y < 7) || (x < 7 && y >= 14)
                    val value = abs((x * 31 + y * 17 + seed * 13) % 9)
                    if (!inFinder && value in listOf(0, 2, 5)) {
                        drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell, cell))
                    }
                }
            }
        }
    }
}
