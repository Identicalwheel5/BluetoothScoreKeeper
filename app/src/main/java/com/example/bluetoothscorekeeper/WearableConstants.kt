package com.example.bluetoothscorekeeper // IMPORTANT: Use your main app's package name here

object WearableConstants {
    // Data Path (Unique identifier for the score data)
    const val SCORE_UPDATE_PATH = "/score_update"

    // Data Keys (The actual data fields)
    const val COMMAND_KEY = "command"
    const val TIMESTAMP_KEY = "timestamp"

    // Command Values
    const val PLAYER_1_INC = "P1_INC"
    const val PLAYER_2_INC = "P2_INC"
}