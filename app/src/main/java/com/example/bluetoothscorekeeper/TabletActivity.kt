package com.example.bluetoothscorekeeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.example.bluetoothscorekeeper.ui.theme.BlueToothScoreKeeperTheme
import java.nio.charset.StandardCharsets

class TabletActivity : ComponentActivity() {

    private val TAG = "TabletScoreboard"
    private val SERVICE_ID = WearableConstants.SCORE_UPDATE_PATH
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT

    private lateinit var connectionsClient: ConnectionsClient
    private var isConnected by mutableStateOf(false)
    private var connectionStatus by mutableStateOf("Starting...")
    private var player1Score by mutableStateOf(0)
    private var player2Score by mutableStateOf(0)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                connectionStatus = "Permissions Granted. Starting Discovery..."
                startDiscovery()
            } else {
                connectionStatus = "Permissions Denied! Check Settings."
                Toast.makeText(this, "Nearby permissions are REQUIRED to connect.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionsClient = Nearby.getConnectionsClient(this)
        requestNearbyPermissions()

        setContent {
            BlueToothScoreKeeperTheme {
                ScoreboardScreen(
                    player1Score = player1Score,
                    player2Score = player2Score,
                    connectionStatus = connectionStatus,
                    isConnected = isConnected
                )
            }
        }
    }

    private fun requestNearbyPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isEmpty()) {
            startDiscovery()
        } else {
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    private fun updateScore(command: String) {
        when (command) {
            WearableConstants.PLAYER_1_INC -> player1Score++
            WearableConstants.PLAYER_2_INC -> player2Score++
            else -> Log.e(TAG, "Unknown score command: $command")
        }
    }

    private fun startDiscovery() {
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started. Looking for Phone...")
            connectionStatus = "Searching for Phone..."
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery failed.", e)
            connectionStatus = "Discovery Failed"
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Found Phone: ${info.endpointName}")
            connectionStatus = "Phone Found. Connecting..."
            connectionsClient.requestConnection("SCORE_CLIENT_TABLET", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.w(TAG, "Lost connection to Phone.")
            connectionStatus = "Disconnected. Restarting discovery..."
            isConnected = false
            startDiscovery()
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            connectionStatus = "Accepting connection..."
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.SUCCESS -> {
                    Log.i(TAG, "Successfully connected to Phone.")
                    isConnected = true
                    connectionStatus = "Connected"
                    connectionsClient.stopDiscovery()
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error.")
                    connectionStatus = "Connection Error"
                    isConnected = false
                    startDiscovery()
                }
                else -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG, "Disconnected from Phone.")
            isConnected = false
            connectionStatus = "Disconnected"
            startDiscovery()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                // Safely unwrap the nullable ByteArray
                payload.asBytes()?.let { payloadBytes ->
                    val command = String(payloadBytes, StandardCharsets.UTF_8)
                    Log.d(TAG, "Payload received: $command")
                    updateScore(command)
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

}
