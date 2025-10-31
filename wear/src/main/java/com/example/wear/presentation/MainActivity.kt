package com.example.wear.presentation

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
import com.example.wear.presentation.WearableConstants

class MainActivity : ComponentActivity() {

    private val dataClient: DataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(
                onScoreUpdate = { command: String -> sendScoreUpdate(command) }
            )
        }
    }

    private fun sendScoreUpdate(command: String) {
        val dataMapRequest = PutDataMapRequest.create(WearableConstants.SCORE_UPDATE_PATH).apply {
            dataMap.putString(WearableConstants.COMMAND_KEY, command)
            dataMap.putLong(WearableConstants.TIMESTAMP_KEY, System.currentTimeMillis())
        }

        val request = dataMapRequest.asPutDataRequest().setUrgent()

        val putTask: Task<DataItem> = dataClient.putDataItem(request)

        putTask.addOnSuccessListener {
            Toast.makeText(this, "Score Sent: $command", Toast.LENGTH_SHORT).show()
        }
        putTask.addOnFailureListener {
            Toast.makeText(this, "Send FAILED", Toast.LENGTH_SHORT).show()
        }
    }
}

