package com.example.bluetoothscorekeeper

import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class PhoneListenerService : WearableListenerService() {

    private val TAG = "PhoneService"

    private val SERVICE_ID = WearableConstants.SCORE_UPDATE_PATH
    private val STRATEGY = Strategy.P2P_POINT_TO_POINT

    private lateinit var connectionsClient: ConnectionsClient
    private var connectedEndpointId: String? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // --- Lifecycle and Initialization ---

    override fun onCreate() {
        super.onCreate()
        connectionsClient = Nearby.getConnectionsClient(this)
        startAdvertising() // CRUCIAL: Start broadcasting immediately
    }

    override fun onDestroy() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        job.cancel()
        super.onDestroy()
    }

    // --- Wear OS Data Receiver (Watch -> Phone) ---

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "Data received from Wear OS.")

        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearableConstants.SCORE_UPDATE_PATH) {

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val command = dataMap.getString(WearableConstants.COMMAND_KEY)

                Log.i(TAG, "Watch Command Received: $command")
                scope.launch {
                    command?.let { ScoreUpdateRepository.newCommand(it) }
                }

                forwardCommandToTablet(command)
            }
        }
    }

    // --- Nearby Connections Advertizing (Phone -> Tablet) ---

    private fun startAdvertising() {
        connectionsClient.startAdvertising(
            "SCORE_SERVER_PHONE",
            SERVICE_ID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully.")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed.", e)
        }
    }

    // --- Nearby Connections Callbacks ---

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d(TAG, "Connection initiated with: ${info.endpointName}")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.SUCCESS -> {
                    Log.i(TAG, "Connected to Tablet: $endpointId")
                    connectedEndpointId = endpointId
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with Tablet.")
                }
                else -> {}
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(TAG, "Disconnected from Tablet: $endpointId")
            connectedEndpointId = null
            startAdvertising()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {}
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    // --- Data Forwarder (Phone -> Tablet) ---

    private fun forwardCommandToTablet(command: String?) {
        if (command == null || connectedEndpointId == null) {
            Log.w(TAG, "Cannot send command. Command is null or not connected to tablet.")
            return
        }

        val payload = Payload.fromBytes(command.toByteArray(StandardCharsets.UTF_8))

        connectionsClient.sendPayload(connectedEndpointId!!, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully forwarded command to Tablet: $command")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send command to Tablet.", e)
            }
    }
}