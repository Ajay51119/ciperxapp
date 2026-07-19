package com.example.cipherkey_flutter.overlay

import android.content.Context
import org.json.JSONObject

object CipherOverlayCodec {
    private const val PREFS = "CipherKeyApp"
    private const val MAP_KEY = "cipher_map"
    private const val SEP = "."

    fun encode(context: Context, text: String): String {
        if (text.isBlank()) return text
        val map = readCipherMap(context)
        if (map.isEmpty()) return text

        return text.map { ch ->
            val key = ch.uppercaseChar().toString()
            when {
                map.containsKey(key) -> map[key].orEmpty()
                ch == ' ' -> "|"
                ch == '\n' -> "\n"
                else -> ch.toString()
            }
        }.joinToString(SEP)
    }

    fun decode(context: Context, encoded: String): String {
        if (encoded.isBlank()) return encoded
        val map = readCipherMap(context)
        if (map.isEmpty()) return encoded

        val reverse = buildMap {
            map.forEach { (letter, value) -> put(value.lowercase(), letter) }
        }

        return encoded.split(SEP).joinToString("") { token ->
            when {
                token == "|" -> " "
                token == "\n" -> "\n"
                reverse.containsKey(token.lowercase()) -> reverse[token.lowercase()].orEmpty()
                else -> token
            }
        }
    }

    private fun readCipherMap(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(MAP_KEY, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val value = json.optString(key, "")
                    if (value.isNotBlank()) put(key.uppercase(), value)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
