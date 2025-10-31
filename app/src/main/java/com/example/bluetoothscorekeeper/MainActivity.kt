package com.example.bluetoothscorekeeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bluetoothscorekeeper.ui.theme.BlueToothScoreKeeperTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var player1Score by mutableStateOf(0)
    private var player2Score by mutableStateOf(0)

    // Launcher for handling runtime permission requests
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestBluetoothPermissions()

        lifecycleScope.launch {
            ScoreUpdateRepository.scoreCommand.collectLatest { command ->
                updateScore(command)
            }
        }

        setContent {
            BlueToothScoreKeeperTheme {
                ScoreboardScreen(
                    player1Score = player1Score,
                    player2Score = player2Score,
                    connectionStatus = "Ready",
                    isConnected = true
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    private fun updateScore(command: String) {
        when (command) {
            WearableConstants.PLAYER_1_INC -> player1Score++
            WearableConstants.PLAYER_2_INC -> player2Score++
        }
    }
}
