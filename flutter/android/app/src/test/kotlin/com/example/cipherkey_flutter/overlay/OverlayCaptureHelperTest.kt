package com.example.cipherkey_flutter.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayCaptureHelperTest {
    @Test
    fun buildCropRectClampsToScreenBounds() {
        val rect = OverlayCaptureHelper.buildCropRect(
            screenWidth = 1000,
            screenHeight = 800,
            bubbleX = 20,
            bubbleY = 20,
            bubbleSizePx = 56,
            paddingPx = 24
        )

        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
        assertEquals(104, rect.right)
        assertEquals(104, rect.bottom)
    }
}
