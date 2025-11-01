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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import com.example.bluetoothscorekeeper.ui.theme.ScoreboardScreen
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    // --- NEARBY CONNECTIONS (FOR TABLET) ---
    private lateinit var connectionsClient: ConnectionsClient
    private val TAG_PHONE = "PhoneMainActivity"
    private val strategy = Strategy.P2P_STAR
    private val serviceId = "com.example.bluetoothscorekeeper.SERVICE_ID"
    private val endpointName = "PHONE_HOST"
    private val connectedClients = mutableMapOf<String, String>()
    private var tabletConnectionStatus by mutableStateOf("Ready")
    private var isTabletConnected by mutableStateOf(false)
    // --- END NEARBY CONNECTIONS ---

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                startAdvertising()
            } else {
                Toast.makeText(this, "Permissions are required for tablet connection.", Toast.LENGTH_LONG).show()
                tabletConnectionStatus = "Permissions Denied"
            }
        }

    // We pass the updateScore lambda to the payload callback.
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { payloadBytes ->
                    val command = String(payloadBytes, StandardCharsets.UTF_8)
                    Log.d(TAG_PHONE, "Received command from tablet: $command")
                    // This will now call the lambda provided by the Composable
                    _updateScore(command)
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
    // A placeholder for our lambda
    private var _updateScore: (String) -> Unit = {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionsClient = Nearby.getConnectionsClient(this)
        requestBluetoothPermissions()

        setContent {
            var player1Score by rememberSaveable { mutableIntStateOf(0) }
            var player2Score by rememberSaveable { mutableIntStateOf(0) }
            var winner by rememberSaveable { mutableStateOf<String?>(null) }

            val updateScoreLambda = { command: String ->
                if (winner != null && command != WearableConstants.RESET_SCORES) {
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

            // 3. HOOK UP THE LAMBDAS
            _updateScore = updateScoreLambda // Hook up the tablet callback

            // Hook up the watch listener
            LaunchedEffect(Unit) {
                ScoreUpdateRepository.scoreCommand.collect { command ->
                    Log.d("WatchListener", "Received command from watch: $command")
                    updateScoreLambda(command)
                }
            }

            ScoreboardScreen(
                player1Score = player1Score,
                player2Score = player2Score,
                connectionStatus = tabletConnectionStatus,
                isConnected = isTabletConnected,
                winner = winner
            )
        }
    }


    override fun onStart() {
        super.onStart()
        if (arePermissionsGranted()) {
            startAdvertising()
        }
    }

    override fun onStop() {
        super.onStop()
        connectionsClient.stopAllEndpoints()
        connectedClients.clear()
        Log.d(TAG_PHONE, "All tablet endpoints stopped.")
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(endpointName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                Log.d(TAG_PHONE, "✅ ADVERTISING SUCCESS: Waiting for tablet.")
                tabletConnectionStatus = "Waiting for tablet..."
            }.addOnFailureListener { e ->
                Log.e(TAG_PHONE, "❌ ADVERTISING FAILED: ${e.message}", e)
                tabletConnectionStatus = "Advertising failed"
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.i(TAG_PHONE, "✅ CONNECTION INITIATED: Accepting request from '${info.endpointName}'.")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                Log.i(TAG_PHONE, "✅ TABLET CONNECTED: Successfully connected to endpoint $endpointId.")
                tabletConnectionStatus = "Tablet Connected"
                isTabletConnected = true
            } else {
                Log.e(TAG_PHONE, "❌ TABLET CONNECTION FAILED: Code ${result.status.statusCode}")
                tabletConnectionStatus = "Tablet connection failed"
                isTabletConnected = false
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG_PHONE, "⚠️ TABLET DISCONNECTED from endpoint $endpointId")
            tabletConnectionStatus = "Tablet Disconnected"
            isTabletConnected = false
            startAdvertising()
        }
    }

    private fun requestBluetoothPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        val missingPermissions = requiredPermissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
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
}
