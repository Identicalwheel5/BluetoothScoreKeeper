package com.example.bluetoothscorekeeper

import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import java.nio.charset.StandardCharsets

class PhoneListenerService : WearableListenerService() {

    private val TAG = "PhoneService"

    // 1. Nearby Connections setup constants
    private val SERVICE_ID = "com.example.bluetoothscorekeeper" // Must be unique and match the tablet
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT // Direct 1:1 connection

    // 2. Nearby Connections Clients and State
    private lateinit var connectionsClient: ConnectionsClient
    private var connectedEndpointId: String? = null // To track the Tablet connection

    // --- Lifecycle and Initialization ---

    override fun onCreate() {
        super.onCreate()
        connectionsClient = Nearby.getConnectionsClient(this)
        startAdvertising() // Start broadcasting our presence immediately
    }

    override fun onDestroy() {
        // Stop advertising and disconnect everything when the service is destroyed
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        super.onDestroy()
    }

    // --- Wear OS Data Receiver (Watch -> Phone) ---

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearableConstants.SCORE_UPDATE_PATH) {

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val command = dataMap.getString(WearableConstants.COMMAND_KEY)

                Log.i(TAG, "Watch Command Received: $command")
                forwardCommandToTablet(command)
            }
        }
    }

    // --- Nearby Connections Advertizing (Phone -> Tablet) ---

    private fun startAdvertising() {
        connectionsClient.startAdvertising(
            "SCORE_SERVER_PHONE", // Name to show the tablet
            SERVICE_ID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed.", e)
        }
    }

    // --- Nearby Connections Callbacks (Handling the Tablet Connection) ---

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // A tablet found us and wants to connect. Auto-accept for simplicity.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d(TAG, "Connection initiated with: ${info.endpointName}")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.SUCCESS -> {
                    Log.i(TAG, "Connected to Tablet: $endpointId")
                    connectedEndpointId = endpointId // Save ID for sending data later
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection rejected by Tablet.")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with Tablet.")
                }
                else -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG, "Disconnected from Tablet: $endpointId")
            connectedEndpointId = null // Reset the connection ID
            startAdvertising() // Restart advertising to allow the tablet to reconnect
        }
    }

    // We don't expect the tablet to send data back, so we use a minimal PayloadCallback
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // Tablet might send status, but we primarily send data TO the tablet
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // --- Data Forwarder (Phone -> Tablet) ---

    private fun forwardCommandToTablet(command: String?) {
        if (command == null || connectedEndpointId == null) {
            Log.w(TAG, "Cannot send command. Command is null or not connected to tablet.")
            return
        }

        // Convert the string command into a Payload object
        val payload = Payload.fromBytes(command.toByteArray(StandardCharsets.UTF_8))

        // Send the payload to the connected Tablet
        connectionsClient.sendPayload(connectedEndpointId!!, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully forwarded command to Tablet: $command")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send command to Tablet.", e)
            }
    }
}