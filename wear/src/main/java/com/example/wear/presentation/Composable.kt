package com.example.wear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

@Composable
fun WearApp(onScoreUpdate: (command: String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Button for Player 1
        Button(
            onClick = { onScoreUpdate(WearableConstants.PLAYER_1_INC) },
            // Make the button stretch for easier tapping
            modifier = Modifier.weight(1f).fillMaxSize()
        ) {
            Text("Player 1 +1", modifier = Modifier.padding(10.dp))
        }

        // Spacer or divider could go here if needed

        // Button for Player 2
        Button(
            onClick = { onScoreUpdate(WearableConstants.PLAYER_2_INC) },
            modifier = Modifier.weight(1f).fillMaxSize()
        ) {
            Text("Player 2 +1", modifier = Modifier.padding(10.dp))
        }
    }
}