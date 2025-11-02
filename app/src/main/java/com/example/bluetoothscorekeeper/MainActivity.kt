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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bluetoothscorekeeper.ui.theme.ScoreboardScreen
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

// Enum to define the role of the device
enum class DeviceRole {
    HOST, CLIENT
}

class MainActivity : ComponentActivity() {

    // --- NEARBY CONNECTIONS ---
    private lateinit var connectionsClient: ConnectionsClient
    private val TAG_PHONE = "PhoneMainActivity"
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.bluetoothscorekeeper.SERVICE_ID"

    // Use the device's BT name or a generic name. Using a fixed name for simplicity.
    private val endpointName = "Scorekeeper"
    private var connectedEndpointId: String? = null
    private var connectionStatus by mutableStateOf("Ready")
    private var isConnected by mutableStateOf(false)
    private var deviceRole by mutableStateOf<DeviceRole?>(null)
    // --- END NEARBY CONNECTIONS ---

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                // Permissions granted, now we can start the chosen role
                when (deviceRole) {
                    DeviceRole.HOST -> startAdvertising()
                    DeviceRole.CLIENT -> startDiscovery()
                    null -> {}
                }
            } else {
                Toast.makeText(this, "Nearby permissions are required to connect.", Toast.LENGTH_LONG).show()
                connectionStatus = "Permissions Denied"
            }
        }

    // --- PAYLOAD & CONNECTION CALLBACKS ---
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { payloadBytes ->
                    val command = String(payloadBytes, StandardCharsets.UTF_8)
                    Log.d(TAG_PHONE, "Received command: $command")
                    _updateScore(command) // Update the score based on the received command
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
    private var _updateScore: (String) -> Unit = {}


    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Both the host and the client need to accept the connection.
            Log.i(TAG_PHONE, "✅ CONNECTION INITIATED on ${deviceRole?.name}: Accepting request from '${info.endpointName}'.")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                Log.i(TAG_PHONE, "✅ CONNECTION SUCCESS: Successfully connected to endpoint $endpointId.")
                connectionsClient.stopAdvertising() // Host stops advertising
                connectionsClient.stopDiscovery()   // Client stops discovering
                connectionStatus = "Connected to ${if (deviceRole == DeviceRole.HOST) "Tablet" else "Phone"}"
                isConnected = true
                connectedEndpointId = endpointId
            } else {
                Log.e(TAG_PHONE, "❌ CONNECTION FAILED: Code ${result.status.statusCode}")
                connectionStatus = "Connection failed"
                isConnected = false
                connectedEndpointId = null // Ensure we don't think we're connected
                // If the connection fails, restart the process for the device's role.
                when(deviceRole) {
                    DeviceRole.HOST -> startAdvertising()
                    DeviceRole.CLIENT -> startDiscovery()
                    null -> { /* do nothing */ }
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG_PHONE, "⚠️ DISCONNECTED from endpoint $endpointId")
            connectionStatus = "Disconnected"
            isConnected = false
            connectedEndpointId = null
            // Go back to the beginning state for the role.
            when (deviceRole) {
                DeviceRole.HOST -> startAdvertising()
                DeviceRole.CLIENT -> startDiscovery()
                null -> {} // Do nothing
            }
        }
    }


    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG_PHONE, "✅ ENDPOINT FOUND: '${info.endpointName}' ($endpointId). Attempting to connect.")
            // Stop discovering and connect to the host.
            connectionsClient.stopDiscovery()
            connectionsClient.requestConnection(endpointName, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener {
                    Log.i(TAG_PHONE, "✅ Connection request sent successfully.")
                    connectionStatus = "Connecting..."
                }
                .addOnFailureListener { e ->
                    Log.e(TAG_PHONE, "❌ Failed to send connection request.", e)
                    connectionStatus = "Connection failed. Retrying..."
                    startDiscovery() // Try again
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.w(TAG_PHONE, "⚠️ ENDPOINT LOST: $endpointId")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionsClient = Nearby.getConnectionsClient(this)

        setContent {
            var player1Score by rememberSaveable { mutableIntStateOf(0) }
            var player2Score by rememberSaveable { mutableIntStateOf(0) }
            var winner by rememberSaveable { mutableStateOf<String?>(null) }

            // --- LAMBDA FOR UPDATING SCORE ---
            val updateScoreLambda = { command: String ->
                if (winner != null && command != WearableConstants.RESET_SCORES) {
                    // Game is over, only allow reset
                } else {
                    when (command) {
                        WearableConstants.PLAYER_1_INC -> player1Score++
                        WearableConstants.PLAYER_2_INC -> player2Score++
                        WearableConstants.PLAYER_1_DEC -> if (player1Score > 0) player1Score--
                        WearableConstants.PLAYER_2_DEC -> if (player2Score > 0) player2Score--
                        WearableConstants.RESET_SCORES -> {
                            player1Score = 0
                            player2Score = 0
                            winner = null
                        }
                    }

                    // Check for winner
                    if (player1Score >= 15 && (player1Score - player2Score) >= 2) {
                        winner = "TEAM WALL"
                    } else if (player2Score >= 15 && (player2Score - player1Score) >= 2) {
                        winner = "TEAM GLASS"
                    }
                }
            }
            _updateScore = updateScoreLambda

            // --- WATCH & TABLET COMMUNICATION ---
            // This effect runs only on the HOST device
            if (deviceRole == DeviceRole.HOST) {
                LaunchedEffect(Unit) {
                    ScoreUpdateRepository.scoreCommand.collect { command ->
                        Log.d("WatchListener", "Received command from watch: $command")
                        updateScoreLambda(command) // Update host's score
                        sendScoreToTablet(command)   // Forward to client
                    }
                }
            }

            // --- UI SWITCHING LOGIC ---
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (deviceRole) {
                    null -> RoleSelectionScreen(
                        onHostSelected = {
                            deviceRole = DeviceRole.HOST
                            if (arePermissionsGranted()) {
                                startAdvertising()
                            } else {
                                requestNearbyPermissions()
                            }
                        },
                        onClientSelected = {
                            deviceRole = DeviceRole.CLIENT
                            if (arePermissionsGranted()) {
                                startDiscovery()
                            } else {
                                requestNearbyPermissions()
                            }
                        }
                    )
                    DeviceRole.HOST, DeviceRole.CLIENT -> {
                        if (deviceRole == DeviceRole.CLIENT && !isConnected) {
                            // Client-specific "searching" screen
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Searching for Host...", style = MaterialTheme.typography.headlineMedium)
                                Text(connectionStatus, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            // Host and connected Client show the scoreboard
                            ScoreboardScreen(
                                player1Score = player1Score,
                                player2Score = player2Score,
                                connectionStatus = connectionStatus,
                                isConnected = isConnected,
                                winner = winner
                            )
                        }
                    }
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        isConnected = false
        Log.d(TAG_PHONE, "All endpoints stopped.")
        // Reset role on exit, so it asks again on next launch
        deviceRole = null
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(endpointName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                Log.d(TAG_PHONE, "✅ ADVERTISING SUCCESS: Waiting for client.")
                connectionStatus = "Waiting for tablet..."
            }.addOnFailureListener { e ->
                Log.e(TAG_PHONE, "❌ ADVERTISING FAILED: ${e.message}", e)
                connectionStatus = "Advertising failed"
            }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(serviceId, discoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                Log.d(TAG_PHONE, "✅ DISCOVERY STARTED: Searching for host.")
                connectionStatus = "Searching for host..."
            }.addOnFailureListener { e ->
                Log.e(TAG_PHONE, "❌ DISCOVERY FAILED to start: ${e.message}", e)
                connectionStatus = "Discovery failed"
            }
    }

    private fun sendScoreToTablet(command: String) {
        // Only the host can send scores
        if (deviceRole == DeviceRole.HOST) {
            connectedEndpointId?.let {
                val payload = Payload.fromBytes(command.toByteArray(StandardCharsets.UTF_8))
                connectionsClient.sendPayload(it, payload)
                Log.d(TAG_PHONE, "Sent command to client: $command")
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestNearbyPermissions() {
        val missingPermissions = getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}


@Composable
fun RoleSelectionScreen(onHostSelected: () -> Unit, onClientSelected: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Device Role", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onHostSelected, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Host (Phone)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClientSelected, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Client (Tablet)")
        }
    }
}
