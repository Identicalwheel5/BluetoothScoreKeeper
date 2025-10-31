package com.example.bluetoothscorekeeper

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ScoreUpdateRepository {
    private val _scoreCommand = MutableSharedFlow<String>(replay = 1)
    val scoreCommand = _scoreCommand.asSharedFlow()

    suspend fun newCommand(command: String) {
        _scoreCommand.emit(command)
    }
}
