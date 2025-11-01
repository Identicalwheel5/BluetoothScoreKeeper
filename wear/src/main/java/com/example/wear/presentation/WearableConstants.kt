package com.example.wear.presentation

object WearableConstants {
    // Data Path (Unique identifier for the score data)
    const val SCORE_UPDATE_PATH = "/score_update"

    // Data Keys (The actual data fields)
    const val COMMAND_KEY = "command"
    const val TIMESTAMP_KEY = "timestamp"

    // Command Values
    const val PLAYER_1_INC = "PLAYER_1_INC"
    const val PLAYER_2_INC = "PLAYER_2_INC"
    const val PLAYER_1_DEC = "PLAYER_1_DEC"
    const val PLAYER_2_DEC = "PLAYER_2_DEC"

    // NEW: Add this for the reset button
    const val RESET_SCORES = "RESET_SCORES"
}