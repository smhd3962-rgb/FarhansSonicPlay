package com.farhan.sonicplay.playback

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(
    private val onTimerExpired: (finishCurrentSong: Boolean) -> Unit
) {
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var finishSongFirst = false

    fun startTimer(minutes: Int, finishCurrentSong: Boolean = false) {
        cancelTimer()
        if (minutes <= 0) return

        finishSongFirst = finishCurrentSong
        _remainingSeconds.value = minutes * 60
        _isActive.value = true

        timerJob = scope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }
            _isActive.value = false
            withContext(Dispatchers.Main) {
                onTimerExpired(finishSongFirst)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
        _isActive.value = false
    }

    val formattedRemainingTime: String
        get() {
            val totalSec = _remainingSeconds.value
            val m = totalSec / 60
            val s = totalSec % 60
            return String.format("%02d:%02d", m, s)
        }
}
