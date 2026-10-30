package com.farhan.sonicplay.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.farhan.sonicplay.data.model.AudioEffectsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEffectsController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    private val _effectsState = MutableStateFlow(AudioEffectsState())
    val effectsState: StateFlow<AudioEffectsState> = _effectsState.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        try {
            release()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.bassBoostStrength.toShort())
                }
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_effectsState.value.virtualizerStrength.toShort())
                }
            }

            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = _effectsState.value.isEnabled
                preset = _effectsState.value.reverbPreset
            }

            // Restore band levels
            _effectsState.value.bandLevels.forEach { (band, level) ->
                try {
                    equalizer?.setBandLevel(band.toShort(), level.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _effectsState.value = _effectsState.value.copy(isEnabled = enabled)
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        presetReverb?.enabled = enabled
    }

    fun setBandLevel(band: Int, levelMilliBels: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), levelMilliBels.toShort())
            val updatedBands = _effectsState.value.bandLevels.toMutableMap().apply {
                put(band, levelMilliBels)
            }
            _effectsState.value = _effectsState.value.copy(bandLevels = updatedBands)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            val clamped = strength.coerceIn(0, 1000)
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
            _effectsState.value = _effectsState.value.copy(bassBoostStrength = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            val clamped = strength.coerceIn(0, 1000)
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
            _effectsState.value = _effectsState.value.copy(virtualizerStrength = clamped)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setPreset(presetIndex: Int) {
        try {
            equalizer?.let { eq ->
                if (presetIndex in 0 until eq.numberOfPresets) {
                    eq.usePreset(presetIndex.toShort())
                    val bands = mutableMapOf<Int, Int>()
                    for (i in 0 until eq.numberOfBands) {
                        bands[i.toInt()] = eq.getBandLevel(i).toInt()
                    }
                    _effectsState.value = _effectsState.value.copy(
                        presetIndex = presetIndex,
                        bandLevels = bands
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPlaybackSpeedAndPitch(player: ExoPlayer, speed: Float, pitch: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 2.5f)
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        player.playbackParameters = PlaybackParameters(clampedSpeed, clampedPitch)
        _effectsState.value = _effectsState.value.copy(
            playbackSpeed = clampedSpeed,
            playbackPitch = clampedPitch
        )
    }

    fun getEqualizerBands(): List<Int> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands.toInt()
        return (0 until numBands).map { eq.getCenterFreq(it.toShort()) / 1000 } // in Hz
    }

    fun getBandLevelRange(): Pair<Int, Int> {
        val eq = equalizer ?: return Pair(-1500, 1500)
        val range = eq.bandLevelRange
        return Pair(range[0].toInt(), range[1].toInt())
    }

    fun getPresetNames(): List<String> {
        val eq = equalizer ?: return emptyList()
        return (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            presetReverb = null
        }
    }
}
