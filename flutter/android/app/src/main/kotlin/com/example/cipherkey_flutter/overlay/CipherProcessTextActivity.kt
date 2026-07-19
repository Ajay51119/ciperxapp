package com.example.cipherkey_flutter.overlay

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class CipherProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        if (text.isNotBlank()) {
            if (!FloatingOverlayService.isRunning()) {
                val serviceIntent = Intent(this, FloatingOverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
            FloatingOverlayService.showDecodedText(text)
        }
        finish()
    }
}
