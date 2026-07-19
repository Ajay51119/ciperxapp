package com.example.cipherkey_flutter.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CipherAccessibilityService : AccessibilityService() {
    private var lastText = ""
    private var lastShownAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val shouldInspect = event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED

        if (!shouldInspect) return

        val text = extractText(event, event.source) ?: return
        if (text.length < 2) return

        val now = System.currentTimeMillis()
        if (text == lastText && now - lastShownAt < 1200) return
        lastText = text
        lastShownAt = now

        val bounds = Rect()
        event.source?.getBoundsInScreen(bounds)
        FloatingOverlayService.onDetectedText(text, bounds)
    }

    override fun onInterrupt() = Unit

    private fun extractText(event: AccessibilityEvent, node: AccessibilityNodeInfo?): String? {
        if (node == null) return event.text?.joinToString(" ")?.trim()?.takeIf { it.isNotBlank() }

        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val raw = node.text?.toString().orEmpty()
        if (start >= 0 && end > start && raw.isNotBlank()) {
            return raw.substring(start, end).trim().takeIf { it.isNotBlank() }
        }

        return event.text?.joinToString(" ")?.trim()?.takeIf { it.isNotBlank() }
            ?: raw.trim().takeIf { it.isNotBlank() }
    }
}
