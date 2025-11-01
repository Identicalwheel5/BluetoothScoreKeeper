package com.example.bluetoothscorekeeper.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreboardScreen(
    player1Score: Int,
    player2Score: Int,
    connectionStatus: String,
    isConnected: Boolean,
    winner: String? // NEW: Accept the winner state
) {
    // NEW: Main UI Logic - decide which screen to show
    if (winner != null) {
        // If there is a winner, show the WinnerScreen
        WinnerScreen(winner = winner)
    } else {
        // If there is no winner, show the regular scoreboard
        ThemedScoreboard(
            player1Score = player1Score,
            player2Score = player2Score,
            connectionStatus = connectionStatus,
            isConnected = isConnected
        )
    }
}

// NEW: A separate composable for the actual scoreboard UI
@Composable
fun ThemedScoreboard(
    player1Score: Int,
    player2Score: Int,
    connectionStatus: String,
    isConnected: Boolean
) {
    BlueToothScoreKeeperTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints {
                val isLandscape = maxWidth > maxHeight

                if (isLandscape) {
                    LandscapeLayout(
                        player1Score = player1Score,
                        player2Score = player2Score,
                        connectionStatus = connectionStatus,
                        isConnected = isConnected
                    )
                } else {
                    PortraitLayout(
                        player1Score = player1Score,
                        player2Score = player2Score,
                        connectionStatus = connectionStatus,
                        isConnected = isConnected
                    )
                }
            }
        }
    }
}

// NEW: The screen to display when a team has won
@Composable
fun WinnerScreen(winner: String) {
    // Determine the background color based on the winner
    val backgroundColor = if (winner == "TEAM WALL") Color.Red else Color.Blue

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = winner,
                fontSize = 80.sp, // Larger text for the winner's name
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "is the Winner!",
                fontSize = 60.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


// --- Portrait Layout (Unchanged) ---
@Composable
fun PortraitLayout(player1Score: Int, player2Score: Int, connectionStatus: String, isConnected: Boolean) {
    // ... (this function remains exactly the same)
    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            TeamScore(
                teamName = "TEAM WALL",
                score = player1Score,
                nameAboveScore = true
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ConnectionStatusText(connectionStatus, isConnected)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Blue), // TEAM GLASS'S BLUE BACKGROUND
            contentAlignment = Alignment.Center
        ) {
            TeamScore(
                teamName = "TEAM GLASS",
                score = player2Score,
                nameAboveScore = false
            )
        }
    }
}

// --- Landscape Layout (Unchanged) ---
@Composable
fun LandscapeLayout(player1Score: Int, player2Score: Int, connectionStatus: String, isConnected: Boolean) {
    // ... (this function remains exactly the same)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Color.Blue), // TEAM GLASS'S BLUE BACKGROUND
                contentAlignment = Alignment.Center
            ) {
                TeamScore(
                    teamName = "TEAM GLASS",
                    score = player2Score,
                    nameAboveScore = true
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Color.Red), // TEAM WALL'S RED BACKGROUND
                contentAlignment = Alignment.Center
            ) {
                TeamScore(
                    teamName = "TEAM WALL",
                    score = player1Score,
                    nameAboveScore = true
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            ConnectionStatusText(connectionStatus, isConnected)
        }
    }
}

// --- Reusable Composables (Unchanged) ---
@Composable
fun TeamScore(teamName: String, score: Int, nameAboveScore: Boolean) {
    // ... (this function remains exactly the same)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (nameAboveScore) {
            Text(
                text = teamName,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = score.toString(),
            fontSize = 250.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        if (!nameAboveScore) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = teamName,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ConnectionStatusText(connectionStatus: String, isConnected: Boolean) {
    // ... (this function remains exactly the same)
    Text(
        text = if (isConnected) "CONNECTED" else connectionStatus,
        fontSize = 24.sp,
        color = if (isConnected) Color.Green.copy(alpha = 0.8f) else Color.Yellow,
        modifier = Modifier.padding(16.dp)
    )
}


// --- PREVIEW FUNCTIONS ---

// ... (PortraitPreview is unchanged)

// ... (LandscapePreview is unchanged)

// NEW: Add a preview for the Winner Screen
@Preview(showBackground = true, name = "Winner Screen Preview (Wall)")
@Composable
fun WinnerScreenWallPreview() {
    WinnerScreen(winner = "TEAM WALL")
}

@Preview(showBackground = true, name = "Winner Screen Preview (Glass)")
@Composable
fun WinnerScreenGlassPreview() {
    WinnerScreen(winner = "TEAM GLASS")
}
