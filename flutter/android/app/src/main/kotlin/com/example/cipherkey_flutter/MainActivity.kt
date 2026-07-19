package com.example.cipherkey_flutter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject
import com.example.cipherkey_flutter.overlay.FloatingOverlayService
import com.example.cipherkey_flutter.overlay.ScreenCaptureRegistry

class MainActivity : FlutterActivity() {
    private val channelName = "cipherkey/native"
    private var pendingScreenCaptureResult: MethodChannel.Result? = null

    companion object {
        private const val REQUEST_SCREEN_CAPTURE = 2024
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "canDrawOverlays" -> result.success(Settings.canDrawOverlays(this))
                "openOverlaySettings" -> {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    result.success(true)
                }
                "isAccessibilityEnabled" -> result.success(isAccessibilityEnabled())
                "openAccessibilitySettings" -> {
                    startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    result.success(true)
                }
                "requestScreenCapturePermission" -> {
                    if (ScreenCaptureRegistry.isReady()) {
                        result.success(true)
                    } else if (pendingScreenCaptureResult != null) {
                        result.success(false)
                    } else {
                        pendingScreenCaptureResult = result
                        requestScreenCapturePermission()
                    }
                }
                "startOverlay" -> {
                    if (!Settings.canDrawOverlays(this)) {
                        result.error("MISSING_PERMISSION", "Overlay permission is not enabled.", null)
                        return@setMethodCallHandler
                    }
                    val intent = Intent(this, FloatingOverlayService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    result.success(ScreenCaptureRegistry.isReady())
                }
                "stopOverlay" -> {
                    stopService(Intent(this, FloatingOverlayService::class.java))
                    result.success(true)
                }
                "syncKeyboardState" -> {
                    val activeKeyName = call.argument<String>("activeKeyName") ?: "Manual"
                    val cipherMap = call.argument<Map<String, String>>("cipherMap") ?: emptyMap()
                    val savedKeys = call.argument<List<Map<String, Any>>>("savedKeys") ?: emptyList()
                    persistKeyboardState(activeKeyName, cipherMap, savedKeys)
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val service = "$packageName/.overlay.CipherAccessibilityService"
        return enabled.contains(service)
    }

    private fun requestScreenCapturePermission() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
        } catch (e: Exception) {
            pendingScreenCaptureResult?.error("CAPTURE_ERROR", e.message, null)
            pendingScreenCaptureResult = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            ScreenCaptureRegistry.setPermission(resultCode, data)
            pendingScreenCaptureResult?.success(ScreenCaptureRegistry.isReady())
            pendingScreenCaptureResult = null
        }
    }

    private fun persistKeyboardState(
        activeKeyName: String,
        cipherMap: Map<String, String>,
        savedKeys: List<Map<String, Any>>
    ) {
        val prefs = getSharedPreferences("CipherKeyApp", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("active_key_name", activeKeyName)
            .putString("cipher_map", JSONObject(cipherMap).toString())
            .putString("saved_keys", JSONArray(savedKeys.map { JSONObject(it) }).toString())
            .apply()
    }
}
