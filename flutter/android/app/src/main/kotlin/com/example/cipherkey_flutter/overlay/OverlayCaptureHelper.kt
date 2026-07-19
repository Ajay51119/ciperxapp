package com.example.cipherkey_flutter.overlay

import android.graphics.Rect

object OverlayCaptureHelper {
    fun buildCropRect(
        screenWidth: Int,
        screenHeight: Int,
        bubbleX: Int,
        bubbleY: Int,
        bubbleSizePx: Int,
        paddingPx: Int = 24
    ): Rect {
        val left = (bubbleX - paddingPx).coerceAtLeast(0)
        val top = (bubbleY - paddingPx).coerceAtLeast(0)
        val right = (bubbleX + bubbleSizePx + paddingPx).coerceAtMost(screenWidth)
        val bottom = (bubbleY + bubbleSizePx + paddingPx).coerceAtMost(screenHeight)
        return Rect(left, top, right, bottom)
    }
}
