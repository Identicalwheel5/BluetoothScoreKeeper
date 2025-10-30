package com.example.wear.presentation // Your actual package name may be different, check the file!

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.example.wear.presentation.WearableConstants // IMPORTANT: Import your constants file!

class MainActivity : ComponentActivity() {

    // 1. Initialize the DataClient for the Wearable Data Layer API
    private val dataClient: DataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Your custom composable for the watch UI
            WearApp(
                onScoreUpdate = { command -> sendScoreUpdate(command) }
            )
        }
    }

    // --- Data Sending Function ---
    private fun sendScoreUpdate(command: String) {
        // 2. Create the DataMap request using the shared path
        val dataMapRequest = PutDataMapRequest.create(WearableConstants.SCORE_UPDATE_PATH).apply {
            // Add the command and a unique timestamp (crucial for sending duplicate taps)
            dataMap.putString(WearableConstants.COMMAND_KEY, command)
            dataMap.putLong(WearableConstants.TIMESTAMP_KEY, System.currentTimeMillis())
        }

        // 3. Convert to PutDataRequest and mark it as URGENT
        val request = dataMapRequest.asPutDataRequest().setUrgent()

        // 4. Send the data item
        val putTask: Task<DataItem> = dataClient.putDataItem(request)

        putTask.addOnSuccessListener {
            Toast.makeText(this, "Score Sent: $command", Toast.LENGTH_SHORT).show()
        }
        putTask.addOnFailureListener {
            Toast.makeText(this, "Send FAILED", Toast.LENGTH_SHORT).show()
        }
    }
}