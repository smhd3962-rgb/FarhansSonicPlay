package com.farhan.sonicplay.playback

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class HeadsetReceiver : BroadcastReceiver() {

    companion object {
        var onHeadsetDisconnected: (() -> Unit)? = null
        var onHeadsetConnected: (() -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                // Earphones/headphones unplugged -> immediately pause playback to prevent loud speaker accidents
                onHeadsetDisconnected?.invoke()
            }
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                if (state == 0) {
                    onHeadsetDisconnected?.invoke()
                } else if (state == 1) {
                    onHeadsetConnected?.invoke()
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                onHeadsetDisconnected?.invoke()
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                onHeadsetConnected?.invoke()
            }
        }
    }
}
