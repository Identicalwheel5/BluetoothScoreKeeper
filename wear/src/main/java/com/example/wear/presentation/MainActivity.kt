package com.example.wear.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
// MAKE SURE YOU HAVE THESE
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private val dataClient: DataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // We now pass the sendScoreUpdate function directly into our new UI
            WearAppWithPages(
                onScoreUpdate = { command: String -> sendScoreUpdate(command) }
            )
        }
    }

    private fun sendScoreUpdate(command: String) {
        val dataMapRequest = PutDataMapRequest.create(WearableConstants.SCORE_UPDATE_PATH).apply {
            dataMap.putString(WearableConstants.COMMAND_KEY, command)
            // It's good practice to add a timestamp to ensure the data item is always fresh
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }

        val request = dataMapRequest.asPutDataRequest().setUrgent()
        val putTask: Task<DataItem> = dataClient.putDataItem(request)

        putTask.addOnSuccessListener {
            // Optional: Show a toast for feedback
            // Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show()
        }
        putTask.addOnFailureListener {
            Toast.makeText(this, "Send FAILED", Toast.LENGTH_SHORT).show()
        }
    }
}

// NEW: This is our new main Composable with swipe functionality
@Composable
fun WearAppWithSwipe(onScoreUpdate: (String) -> Unit) {
    // 1. Create a state for the pager, telling it we have 2 pages
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    // 2. HorizontalPager is the component that allows swiping
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        // The 'page' variable tells us which page we are currently displaying (0 or 1)
        when (page) {
            0 -> ScoreIncrementPage(onScoreUpdate = onScoreUpdate) // Page 1
            1 -> ScoreDecrementPage(onScoreUpdate = onScoreUpdate) // Page 2
        }
    }
}

// This Composable is for the FIRST page (Incrementing Score)
@Composable
fun ScoreIncrementPage(onScoreUpdate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Increment Score", textAlign = TextAlign.Center)
        Button(onClick = { onScoreUpdate(WearableConstants.PLAYER_1_INC) }) {
            Text("Player 1 +")
        }
        Button(onClick = { onScoreUpdate(WearableConstants.PLAYER_2_INC) }) {
            Text("Player 2 +")
        }
    }
}

// This Composable is for the SECOND page (Decrementing Score)
@Composable
fun ScoreDecrementPage(onScoreUpdate: (String) -> Unit) {
    // To make this work, you need to define the constants for decrementing
    // For now, we will assume they are named like this:
    // val PLAYER_1_DEC = "PLAYER_1_DEC"
    // val PLAYER_2_DEC = "PLAYER_2_DEC"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Decrement Score", textAlign = TextAlign.Center)
        Button(onClick = { onScoreUpdate("PLAYER_1_DEC") }) {
            Text("Player 1 -")
        }
        Button(onClick = { onScoreUpdate("PLAYER_2_DEC") }) {
            Text("Player 2 -")
        }
    }
}
