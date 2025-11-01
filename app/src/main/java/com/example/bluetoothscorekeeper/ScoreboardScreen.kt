package com.example.bluetoothscorekeeper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreboardScreen(
    player1Score: Int,
    player2Score: Int,
    connectionStatus: String,
    isConnected: Boolean,
    onPlayer1Inc: () -> Unit,
    onPlayer2Inc: () -> Unit,
    onResetScores: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // --- Player 1 Score ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Player 1", fontSize = 32.sp, color = Color.Blue)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = player1Score.toString(),
                    fontSize = 120.sp,
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Divider / Status ---
            Text(
                text = "—",
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // --- Player 2 Score ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player2Score.toString(),
                    fontSize = 120.sp,
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Player 2", fontSize = 32.sp, color = Color.Red)
            }

            // --- Connection Status ---
            Text(
                text = connectionStatus,
                fontSize = 18.sp,
                color = if (isConnected) Color.Green else Color.Yellow
            )
        }
    }
}