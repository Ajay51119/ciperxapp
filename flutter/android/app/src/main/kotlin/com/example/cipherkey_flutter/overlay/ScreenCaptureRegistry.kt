package com.example.cipherkey_flutter.overlay

import android.app.Activity
import android.content.Intent

object ScreenCaptureRegistry {
    @Volatile private var resultCode: Int = Activity.RESULT_CANCELED
    @Volatile private var data: Intent? = null

    fun setPermission(code: Int, intent: Intent?) {
        resultCode = code
        data = intent?.let { Intent(it) }
    }

    fun isReady(): Boolean {
        return resultCode == Activity.RESULT_OK && data != null
    }

    fun snapshot(): Pair<Int, Intent?>? {
        val intent = data ?: return null
        if (resultCode != Activity.RESULT_OK) return null
        return resultCode to Intent(intent)
    }

    fun clear() {
        resultCode = Activity.RESULT_CANCELED
        data = null
    }
}
