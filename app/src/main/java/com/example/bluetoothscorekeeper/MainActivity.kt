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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.example.bluetoothscorekeeper.ui.theme.BlueToothScoreKeeperTheme // Adjust theme name as necessary
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val TAG = "TabletScoreboard" // Log Tag for the Tablet/Scoreboard
    private val SERVICE_ID = WearableConstants.SCORE_UPDATE_PATH // Using a common ID
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT

    private lateinit var connectionsClient: ConnectionsClient
    private var isConnected by mutableStateOf(false)
    private var connectionStatus by mutableStateOf("Starting...")
    private var player1Score by mutableStateOf(0)
    private var player2Score by mutableStateOf(0)

    // Launcher for handling runtime permission requests
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if ALL necessary permissions were granted
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                connectionStatus = "Permissions Granted. Starting Discovery..."
                startDiscovery()
            } else {
                connectionStatus = "Permissions Denied!"
                Toast.makeText(this, "Nearby permissions are REQUIRED to run the scoreboard.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionsClient = Nearby.getConnectionsClient(this)

        // **CRITICAL CHANGE**: Request permissions before launching discovery
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

    override fun onStop() {
        // Only stop discovery/connections if the activity is truly exiting
        // connectionsClient.stopDiscovery()
        // connectionsClient.stopAllEndpoints()
        super.onStop()
    }

    // --- PERMISSION HANDLER ---

    private fun requestNearbyPermissions() {
        // Define all permissions needed for modern Android (API 33+)
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            // For older Android versions (rely on location permission being available)
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Check if permissions are already granted
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isEmpty()) {
            // All required permissions are granted, proceed to discovery
            startDiscovery()
        } else {
            // Request missing permissions
            requestPermissionLauncher.launch(missingPermissions)
        }
    }

    // --- SCORE UPDATE HANDLER ---
    private fun updateScore(command: String) {
        when (command) {
            WearableConstants.PLAYER_1_INC -> player1Score++
            WearableConstants.PLAYER_2_INC -> player2Score++
            else -> Log.e(TAG, "Unknown score command: $command")
        }
    }

    // --- DISCOVERY LOGIC (Listening for the Phone) ---
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
            connectionStatus = "Discovery Failed: Check Permissions"
        }
    }

    // --- DISCOVERY CALLBACKS ---
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Found Phone: ${info.endpointName}")
            connectionStatus = "Phone Found. Requesting connection..."
            connectionsClient.requestConnection("SCORE_CLIENT_TABLET", endpointId, connectionLifecycleCallback)
                .addOnSuccessListener { Log.d(TAG, "Connection request sent.") }
                .addOnFailureListener { Log.e(TAG, "Connection request failed.", it) }
        }
        override fun onEndpointLost(endpointId: String) {
            Log.w(TAG, "Lost connection to Phone endpoint.")
            connectionStatus = "Disconnected. Restarting discovery..."
            isConnected = false
            startDiscovery()
        }
    }

    // --- CONNECTION LIFECYCLE CALLBACKS ---
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto-accept the connection
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            connectionStatus = "Connecting..."
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.SUCCESS -> {
                    Log.i(TAG, "Successfully connected to Phone.")
                    isConnected = true
                    connectionStatus = "Connected (Watch Ready)"
                    connectionsClient.stopDiscovery()
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error.")
                    connectionStatus = "Connection Error"
                    isConnected = false
                    startDiscovery() // Attempt to restart connection
                }
                else -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG, "Disconnected from Phone. Re-enabling discovery.")
            isConnected = false
            connectionStatus = "Disconnected"
            startDiscovery()
        }
    }

    // --- PAYLOAD RECEIVER (Getting data from the Phone) ---
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val commandBytes = payload.asBytes() ?: return
                val command = String(commandBytes, StandardCharsets.UTF_8)

                Log.d(TAG, "Payload received: $command")

                // Update the UI state with the received score command
                updateScore(command)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}