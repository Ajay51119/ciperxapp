package com.example.cipherkey_flutter.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.cipherkey_flutter.MainActivity
import com.example.cipherkey_flutter.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var rootView: LinearLayout? = null
    private var popupView: View? = null
    private var rootParams: WindowManager.LayoutParams? = null
    private var popupParams: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var inspectArmed = false
    private var lastDetectedText: String = ""
    private var lastDetectedBounds: Rect? = null
    private var longPressRunnable: Runnable? = null
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isNotBlank() && inspectArmed) {
            showDecodedPopup(text)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // Boot as mediaProjection type compatible foreground service
        startForeground(NOTIFICATION_ID, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        addOverlay()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(clipboardListener)
        instance = this

        // Check for any pending text selection triggers
        pendingTextToDecode?.let { text ->
            showDecodedPopup(text)
            pendingTextToDecode = null
        }
    }

    override fun onDestroy() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.removePrimaryClipChangedListener(clipboardListener)
        ScreenCaptureController.release()
        removeBubble()
        removePopup()
        inspectArmed = false
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addOverlay() {
        if (rootView != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Stunning programmatically-drawn circular green/dark gradient search icon logo
        val logo = ImageView(this).apply {
            setImageDrawable(CipherSearchIconDrawable(this@FloatingOverlayService))
            setPadding(dp(10), dp(10), dp(10), dp(10))
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "CipherApex Search Overlay logo"
        }

        container.addView(logo, LinearLayout.LayoutParams(dp(56), dp(56)))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(160)
        }

        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    inspectArmed = false
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    longPressRunnable = Runnable {
                        inspectArmed = true
                    }.also { handler.postDelayed(it, 300) }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.rawX - downX) > dp(8) || kotlin.math.abs(event.rawY - downY) > dp(8)) {
                        moved = true
                        longPressRunnable?.let { handler.removeCallbacks(it) }
                        params.x = startX + (event.rawX - downX).toInt()
                        params.y = startY + (event.rawY - downY).toInt()
                        windowManager.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    if (moved || inspectArmed) {
                        captureAndRecognize(
                            bubbleX = params.x,
                            bubbleY = params.y,
                            bubbleSizePx = dp(56)
                        )
                    }
                    true
                }
                else -> false
            }
        }

        rootView = container
        rootParams = params
        windowManager.addView(container, params)
    }

    private fun removeOverlay() {
        rootView?.let { safeRemove(it) }
        rootView = null
        rootParams = null
    }

    fun onDetectedText(text: String, bounds: Rect?) {
        if (text.isBlank()) return

        lastDetectedText = text
        lastDetectedBounds = bounds?.let { Rect(it) }

        if (inspectArmed) {
            showDecodedPopup(text)
        }
    }

    private fun captureAndRecognize(bubbleX: Int, bubbleY: Int, bubbleSizePx: Int) {
        if (!ScreenCaptureRegistry.isReady()) {
            showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
            return
        }

        if (!ScreenCaptureController.ensureStarted(this)) {
            showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
            return
        }

        // Faintly dim overlay to show screen capture is processing
        rootView?.alpha = 0.25f

        ScreenCaptureController.captureLatestBitmap { bitmap ->
            if (bitmap == null) {
                handler.post {
                    rootView?.alpha = 1.0f
                    showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
                }
                return@captureLatestBitmap
            }

            val metrics = resources.displayMetrics
            
            // Calculate absolute center coords for the search bubble pointer
            val anchorX = bubbleX + bubbleSizePx / 2
            val anchorY = bubbleY + bubbleSizePx / 2

            // Use a wide crop region (420dp width, 240dp height) so full text blocks fit
            val cropWidth = dp(420).coerceAtMost(bitmap.width)
            val cropHeight = dp(240).coerceAtMost(bitmap.height)

            // Clamp crop box inside screen boundaries to handle screen edges and prevent crashes
            val left = (anchorX - cropWidth / 2).coerceIn(0, (bitmap.width - cropWidth).coerceAtLeast(0))
            val top = (anchorY - cropHeight / 2).coerceIn(0, (bitmap.height - cropHeight).coerceAtLeast(0))
            val cropRect = Rect(left, top, left + cropWidth, top + cropHeight)

            try {
                val cropped = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                val image = InputImage.fromBitmap(cropped, 0)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Calculate center coordinate of the search bubble relative to the cropped image
                        val relativeCenterX = (anchorX - left).toFloat()
                        val relativeCenterY = (anchorY - top).toFloat()
                        val extracted = chooseNearestBlock(visionText, relativeCenterX, relativeCenterY)

                        handler.post {
                            rootView?.alpha = 1.0f
                            if (extracted.isNotBlank()) {
                                showDecodedPopup(extracted)
                            } else {
                                showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
                            }
                        }
                    }
                    .addOnFailureListener {
                        handler.post {
                            rootView?.alpha = 1.0f
                            showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
                        }
                    }
            } catch (e: Exception) {
                handler.post {
                    rootView?.alpha = 1.0f
                    showDecodedPopup(lastDetectedText.takeIf { it.isNotBlank() }.orEmpty())
                }
            }
        }
    }

    private fun chooseNearestBlock(text: Text, centerX: Float, centerY: Float): String {
        val blocks = text.textBlocks
        if (blocks.isEmpty()) return ""

        val density = resources.displayMetrics.density

        // 1. Find the primary block closest to the search center anchor based on point-to-rectangle distance
        val sortedBlocks = blocks.mapNotNull { block ->
            val box = block.boundingBox ?: return@mapNotNull null
            
            // Calculate absolute distance from search center to the closest edge of the bounding box
            val dx = when {
                centerX < box.left -> box.left - centerX
                centerX > box.right -> centerX - box.right
                else -> 0f
            }
            val dy = when {
                centerY < box.top -> box.top - centerY
                centerY > box.bottom -> centerY - box.bottom
                else -> 0f
            }
            val dist = dx * dx + dy * dy
            Triple(block, dist, box)
        }.sortedBy { it.second }

        val primaryMatch = sortedBlocks.firstOrNull() ?: return ""
        val primaryBlock = primaryMatch.first
        val primaryBox = primaryMatch.third

        // 2. Identify all related blocks vertically inline (within the same chat bubble column)
        val maxVerticalGap = 20f * density
        val maxHorizontalGap = 12f * density

        val relatedBlocks = blocks.filter { block ->
            val box = block.boundingBox ?: return@filter false
            if (block === primaryBlock) return@filter true

            val verticalGap = when {
                box.bottom < primaryBox.top -> (primaryBox.top - box.bottom).toFloat()
                box.top > primaryBox.bottom -> (box.top - primaryBox.bottom).toFloat()
                else -> 0f
            }

            val horizontalOverlap = (box.left < primaryBox.right && box.right > primaryBox.left)
            val horizontalGap = if (horizontalOverlap) 0f else {
                when {
                    box.right < primaryBox.left -> (primaryBox.left - box.right).toFloat()
                    box.left > primaryBox.right -> (box.left - primaryBox.right).toFloat()
                    else -> 0f
                }
            }

            verticalGap <= maxVerticalGap && horizontalGap <= maxHorizontalGap
        }

        // 3. Extract and sort all individual lines by their vertical top coordinate
        val allLines = relatedBlocks.flatMap { it.lines }
        if (allLines.isEmpty()) return primaryBlock.text.trim()

        val sortedLines = allLines.sortedBy { it.boundingBox?.top ?: 0 }

        // 4. Merge lines while handling tokens dot separator bounds and stripping spaces
        val builder = java.lang.StringBuilder()
        for (line in sortedLines) {
            val lineText = line.text.replace(" ", "").trim()
            if (lineText.isEmpty()) continue
            
            if (builder.isNotEmpty()) {
                val lastChar = builder.last()
                val firstChar = lineText.first()
                if (lastChar != '.' && firstChar != '.') {
                    builder.append(".")
                }
            }
            builder.append(lineText)
        }

        return builder.toString()
    }

    private fun showDecodedPopup(text: String) {
        removePopup()
        val decoded = CipherOverlayCodec.decode(this, text).trim()
        val density = resources.displayMetrics.density

        // Create the card panel
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(0xF20F1115.toInt()) // Deep dark theme background
                setStroke((1.5f * density).toInt(), 0xFF1DB954.toInt()) // Spotify green border
            }
            setPadding((24f * density).toInt(), (20f * density).toInt(), (24f * density).toInt(), (20f * density).toInt())
        }

        // Title Header
        val titleText = TextView(this).apply {
            setText("DECIPHERED RESULT")
            setTextColor(0xFF1DB954.toInt())
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 0, 0, (8f * density).toInt())
            letterSpacing = 0.15f
        }
        panel.addView(titleText)

        // Decoded Message
        val contentText = TextView(this).apply {
            setText(decoded.ifBlank { "No encoded text detected." })
            setTextColor(Color.WHITE)
            textSize = 16f
            setLineSpacing(0f, 1.25f)
            setPadding(0, 0, 0, (20f * density).toInt())
        }
        panel.addView(contentText)

        // Actions row (Horizontal layout)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        // Copy button (Pill shaped, Spotify Green)
        val copyButton = Button(this).apply {
            setText("Copy")
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(0xFF1DB954.toInt())
            }
            setTextColor(Color.BLACK)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (36f * density).toInt()).apply {
                rightMargin = (10f * density).toInt()
            }
            layoutParams = params
            setPadding((16f * density).toInt(), 0, (16f * density).toInt(), 0)
            isAllCaps = false

            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("CipherApex", decoded))
                this.text = "Copied!"
                this.postDelayed({ this.text = "Copy" }, 1200)
            }
        }

        // Close button (Pill shaped, Sleek Dark theme outline)
        val closeButton = Button(this).apply {
            setText("Close")
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(0x33FFFFFF.toInt())
                setStroke((1f * density).toInt(), 0x55FFFFFF.toInt())
            }
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (36f * density).toInt())
            setPadding((16f * density).toInt(), 0, (16f * density).toInt(), 0)
            isAllCaps = false

            setOnClickListener { removePopup() }
        }

        actionsRow.addView(copyButton)
        actionsRow.addView(closeButton)
        panel.addView(actionsRow)

        // Full screen dimmed background to center the overlay popup card
        val overlayContainer = FrameLayout(this).apply {
            setBackgroundColor(0x7F000000.toInt())
            setOnClickListener { removePopup() }
        }

        // Center card inside the container
        val cardParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost((360f * density).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        panel.setOnClickListener { /* no-op configuration */ }
        overlayContainer.addView(panel, cardParams)

        // Make window not touch modal so we can click copy/close buttons
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        popupView = overlayContainer
        popupParams = params
        windowManager.addView(overlayContainer, params)
    }

    private fun removeBubble() {
        removeOverlay()
    }

    private fun removePopup() {
        popupView?.let { safeRemove(it) }
        popupView = null
        popupParams = null
    }

    private fun safeRemove(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {}
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildNotification(): Notification {
        val channelId = "cipher_overlay"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "CipherApex Overlay", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            pendingFlags()
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("CipherApex Active")
            .setContentText("Floating OCR overlay running in background")
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    // Custom programmatically drawn circular visual drawable search icon
    private class CipherSearchIconDrawable(context: Context) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val dp = context.resources.displayMetrics.density

        override fun draw(canvas: Canvas) {
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            val cx = w / 2f
            val cy = h / 2f
            val radius = (w / 2f) - (2f * dp)

            // Background Circle (Dark tone matching Scaffold)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF0F1115.toInt()
            canvas.drawCircle(cx, cy, radius, paint)

            // Outline green gradient stroke
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * dp
            paint.color = 0xFF1DB954.toInt()
            canvas.drawCircle(cx, cy, radius, paint)

            // Magnifying Glass Loop (White)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f * dp
            paint.color = Color.WHITE
            val searchRadius = 6f * dp
            val scx = cx - 2f * dp
            val scy = cy - 2f * dp
            canvas.drawCircle(scx, scy, searchRadius, paint)

            // Handle bar
            val handleStart = (searchRadius / kotlin.math.sqrt(2.0)).toFloat()
            val hx1 = scx + handleStart
            val hy1 = scy + handleStart
            val hx2 = cx + 8f * dp
            val hy2 = cy + 8f * dp
            canvas.drawLine(hx1, hy1, hx2, hy2, paint)

            // Glowing cipher dot indicator (Spotify Green)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF1DB954.toInt()
            canvas.drawCircle(scx, scy, 2f * dp, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    companion object {
        private const val NOTIFICATION_ID = 5011
        private var instance: FloatingOverlayService? = null
        @Volatile private var pendingTextToDecode: String? = null

        fun isRunning(): Boolean = instance != null

        fun onDetectedText(text: String, bounds: Rect?) {
            instance?.onDetectedText(text, bounds)
        }

        fun showDecodedText(text: String) {
            val inst = instance
            if (inst != null) {
                inst.showDecodedPopup(text)
            } else {
                pendingTextToDecode = text
            }
        }
    }
}
