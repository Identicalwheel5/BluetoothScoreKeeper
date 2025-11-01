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
import com.example.bluetoothscorekeeper.ui.theme.BlueToothScoreKeeperTheme
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

class TabletActivity : ComponentActivity() {

    private var player1Score by mutableStateOf(0)
    private var player2Score by mutableStateOf(0)
    private var connectionStatus by mutableStateOf("Ready")
    private var isConnected by mutableStateOf(false)

    // --- START OF CHANGES ---

    // ACTION: Add a dedicated TAG for easy Logcat filtering
    private val TAG_TABLET = "TabletActivity"
    private val clientName = "SCORE_CLIENT_TABLET" // The name it will introduce itself with

    // --- END OF CHANGES ---

    private lateinit var connectionsClient: ConnectionsClient
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.bluetoothscorekeeper.SERVICE_ID"
    private var hostEndpointId: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startDiscovery()
            } else {
                Toast.makeText(this, "All permissions are required for the app to function.", Toast.LENGTH_LONG).show()
                connectionStatus = "Permissions Denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionsClient = Nearby.getConnectionsClient(this)

        setContent {
            BlueToothScoreKeeperTheme {
                // FIX: Add the missing lambda parameters for score updates
                ScoreboardScreen(
                    player1Score = player1Score,
                    player2Score = player2Score,
                    connectionStatus = connectionStatus,
                    isConnected = isConnected,
                    onPlayer1Inc = { updateScore(WearableConstants.PLAYER_1_INC) },
                    onPlayer2Inc = { updateScore(WearableConstants.PLAYER_2_INC) },
                    onResetScores = {
                        player1Score = 0
                        player2Score = 0
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (arePermissionsGranted()) {
            startDiscovery()
        } else {
            requestBluetoothPermissions()
        }
    }

    override fun onStop() {
        hostEndpointId?.let {
            connectionsClient.disconnectFromEndpoint(it)
            Log.d(TAG_TABLET, "Disconnecting from endpoint $it")
        }
        connectionsClient.stopDiscovery()
        Log.d(TAG_TABLET, "Stopping discovery.")
        connectionStatus = "Disconnected"
        isConnected = false
        super.onStop()
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE, // Needed for strategy P2P_STAR
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions)
        } else {
            startDiscovery()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updateScore(command: String) {
        runOnUiThread {
            when (command) {
                WearableConstants.PLAYER_1_INC -> player1Score++
                WearableConstants.PLAYER_2_INC -> player2Score++
            }
        }
    }

    private fun sendScoreUpdate(command: String) {
        if (isConnected && hostEndpointId != null) {
            val payload = Payload.fromBytes(command.toByteArray(StandardCharsets.UTF_8))
            connectionsClient.sendPayload(hostEndpointId!!, payload)
                .addOnSuccessListener {
                    Log.d(TAG_TABLET, "Successfully sent command: $command")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG_TABLET, "Failed to send command: $command", e)
                }
        } else {
            Log.w(TAG_TABLET, "Not connected, cannot send command: $command")
        }
    }


    // --- START OF CHANGES ---

    // ACTION: Modified startDiscovery to include detailed logging
    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            // CHECKPOINT 2 (Success): Tablet successfully starts looking for a host.
            Log.d(TAG_TABLET, "✅ DISCOVERY STARTED: Listening for hosts.")
            connectionStatus = "Searching for phone..."
        }.addOnFailureListener { e ->
            // CHECKPOINT 2 (Failure): If this happens, the process stops here.
            Log.e(TAG_TABLET, "❌ DISCOVERY FAILED: ${e.message}", e)
            connectionStatus = "Discovery failed"
        }
    }

    // ACTION: Added detailed logging to the endpointDiscoveryCallback
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // CHECKPOINT 3: Tablet finds a potential host.
            // The `info.endpointName` should be "PHONE_HOST".
            Log.i(TAG_TABLET, "✅ ENDPOINT FOUND: Discovered '${info.endpointName}' (ID: $endpointId).")
            Log.i(TAG_TABLET, "ACTION: Stopping discovery and requesting connection...")

            // Important: Stop discovery once you find the endpoint to save battery and bandwidth.
            connectionsClient.stopDiscovery()

            // Request the connection
            connectionsClient.requestConnection(clientName, endpointId, connectionLifecycleCallback)
                .addOnFailureListener { e ->
                    Log.e(TAG_TABLET, "❌ FAILED TO REQUEST CONNECTION: ${e.message}", e)
                    // If the request fails, try to start discovering again
                    startDiscovery()
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.w(TAG_TABLET, "⚠️ ENDPOINT LOST: The advertising device $endpointId is no longer available.")
            // If we were connected to this lost endpoint, update the UI
            if (endpointId == hostEndpointId) {
                connectionStatus = "Phone lost. Searching again..."
                isConnected = false
                hostEndpointId = null
                startDiscovery()
            }
        }
    }

    // ACTION: Added detailed logging to the tablet's connectionLifecycleCallback
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // This is not expected to be called on the client (discoverer) side.
            // The host (advertiser) handles this. We can add a log for safety.
            Log.w(TAG_TABLET, "Unexpected onConnectionInitiated on client side. Rejecting connection.")
            connectionsClient.rejectConnection(endpointId)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    // CHECKPOINT 5 (Success): The connection is fully established.
                    Log.i(TAG_TABLET, "✅ CONNECTION ESTABLISHED: Successfully connected to host (ID: $endpointId).")
                    connectionStatus = "Connected"
                    isConnected = true
                    hostEndpointId = endpointId
                }
                else -> {
                    // CHECKPOINT 5 (Failure): The host likely rejected or an error occurred.
                    Log.e(TAG_TABLET, "❌ CONNECTION FAILED: Code ${result.status.statusCode} for endpoint $endpointId.")
                    connectionStatus = "Connection rejected or failed"
                    isConnected = false
                    hostEndpointId = null
                    // IMPORTANT: Restart discovery to try again.
                    startDiscovery()
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG_TABLET, "⚠️ DISCONNECTED from host (ID: $endpointId)")
            connectionStatus = "Disconnected. Searching again..."
            isConnected = false
            hostEndpointId = null
            // Automatically start searching for the host again.
            startDiscovery()
        }
    }

    // ACTION: Added detailed logging to the payload callback
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { payloadBytes ->
                    val command = String(payloadBytes, StandardCharsets.UTF_8)
                    Log.d(TAG_TABLET, "Received command '$command' from host (ID: $endpointId)")
                    updateScore(command)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Can be used to track file transfer progress if needed.
        }
    }

    // --- END OF CHANGES ---
}
