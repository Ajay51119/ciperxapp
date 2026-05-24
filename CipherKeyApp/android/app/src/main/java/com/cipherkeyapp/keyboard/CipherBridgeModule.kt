// android/app/src/main/java/com/cipherkeyapp/keyboard/CipherBridgeModule.kt
package com.cipherkeyapp.keyboard

import android.content.SharedPreferences
import android.view.inputmethod.InputMethodManager
import android.provider.Settings
import com.facebook.react.bridge.*

class CipherBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "CipherBridge"

    private val prefs: SharedPreferences by lazy {
        reactApplicationContext.getSharedPreferences("CipherKeyApp", android.content.Context.MODE_PRIVATE)
    }

    /**
     * Save cipher map from React Native to SharedPreferences
     * so the keyboard service can read it.
     */
    @ReactMethod
    fun saveCipherMap(mapJson: String, promise: Promise) {
        try {
            prefs.edit().putString("cipher_map", mapJson).apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    /**
     * Save the active key and imported key list for the native keyboard.
     */
    @ReactMethod
    fun saveKeyboardState(activeKeyName: String, mapJson: String, keysJson: String, promise: Promise) {
        try {
            prefs.edit()
                .putString("active_key_name", activeKeyName)
                .putString("cipher_map", mapJson)
                .putString("saved_keys", keysJson)
                .apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }

    /**
     * Check if CipherKey keyboard is enabled in system settings
     */
    @ReactMethod
    fun isKeyboardEnabled(promise: Promise) {
        val enabledInputMethods = Settings.Secure.getString(
            reactApplicationContext.contentResolver,
            Settings.Secure.ENABLED_INPUT_METHODS
        ) ?: ""
        val enabled = enabledInputMethods.contains("com.cipherkeyapp/.keyboard.CipherKeyboardService")
        promise.resolve(enabled)
    }

    /**
     * Check if CipherKey is the currently selected system keyboard.
     */
    @ReactMethod
    fun isKeyboardSelected(promise: Promise) {
        val defaultInputMethod = Settings.Secure.getString(
            reactApplicationContext.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        val selected = defaultInputMethod.contains("com.cipherkeyapp/.keyboard.CipherKeyboardService")
        promise.resolve(selected)
    }

    /**
     * Open keyboard settings so user can enable CipherKey keyboard
     */
    @ReactMethod
    fun openKeyboardSettings(promise: Promise) {
        val intent = android.content.Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        reactApplicationContext.startActivity(intent)
        promise.resolve(true)
    }

    /**
     * Open Android's keyboard switcher so the user can make CipherKey active.
     */
    @ReactMethod
    fun showKeyboardPicker(promise: Promise) {
        try {
            val imm = reactApplicationContext.getSystemService(
                android.content.Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager
            imm.showInputMethodPicker()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERROR", e.message)
        }
    }
}
