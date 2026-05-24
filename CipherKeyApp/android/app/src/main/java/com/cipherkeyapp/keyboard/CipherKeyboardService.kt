package com.cipherkeyapp.keyboard

import android.content.SharedPreferences
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.cipherkeyapp.R
import org.json.JSONArray
import org.json.JSONObject

class CipherKeyboardService : InputMethodService() {

    private data class StoredKey(val name: String, val cipher: Map<String, String>)

    private lateinit var prefs: SharedPreferences
    private var cipherMap: Map<String, String> = emptyMap()
    private var reverseMap: Map<String, String> = emptyMap()
    private var savedKeys: List<StoredKey> = emptyList()
    private var activeKeyName: String = "Manual"
    private var symbolMode = false
    private var shiftMode = false
    private var currentMode = Mode.ENCODE

    private enum class Mode { ENCODE, DECODE }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("CipherKeyApp", MODE_PRIVATE)
        loadCipherState()
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_layout, null)
        loadCipherState()
        setupKeyboard(view)
        return view
    }

    private fun loadCipherState() {
        activeKeyName = prefs.getString("active_key_name", "Manual") ?: "Manual"
        savedKeys = readSavedKeys()

        val mapJson = prefs.getString("cipher_map", null)
        if (mapJson.isNullOrBlank()) {
            cipherMap = emptyMap()
            reverseMap = emptyMap()
            return
        }

        try {
            cipherMap = readCipherObject(JSONObject(mapJson))
            reverseMap = cipherMap.entries.associate { it.value.lowercase() to it.key.uppercase() }
        } catch (e: Exception) {
            cipherMap = emptyMap()
            reverseMap = emptyMap()
        }
    }

    private fun readSavedKeys(): List<StoredKey> {
        val keysJson = prefs.getString("saved_keys", "[]") ?: "[]"
        return try {
            val array = JSONArray(keysJson)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name", "Key ${index + 1}")
                val cipherObject = item.optJSONObject("cipher") ?: return@mapNotNull null
                StoredKey(name, readCipherObject(cipherObject))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readCipherObject(json: JSONObject): Map<String, String> {
        val map = mutableMapOf<String, String>()
        json.keys().forEach { key ->
            val value = json.optString(key, "")
            if (value.isNotBlank()) {
                map[key.uppercase()] = value
            }
        }
        return map
    }

    private fun saveActiveKey(key: StoredKey) {
        activeKeyName = key.name
        cipherMap = key.cipher
        reverseMap = cipherMap.entries.associate { it.value.lowercase() to it.key.uppercase() }
        prefs.edit()
            .putString("active_key_name", key.name)
            .putString("cipher_map", JSONObject(key.cipher).toString())
            .apply()
    }

    private fun encode(text: String): String {
        return text.map { ch ->
            val up = ch.uppercaseChar().toString()
            when {
                cipherMap.containsKey(up) -> cipherMap[up].orEmpty()
                ch == ' ' -> "|"
                ch == '\n' -> "\n"
                else -> ch.toString()
            }
        }.joinToString(".")
    }

    private fun decode(text: String): String {
        return text.split(".").joinToString("") { token ->
            when {
                token == "|" -> " "
                token == "\n" -> "\n"
                reverseMap.containsKey(token.lowercase()) -> reverseMap[token.lowercase()].orEmpty()
                else -> token
            }
        }
    }

    private fun setupKeyboard(view: View) {
        val inputField = view.findViewById<EditText>(R.id.keyboard_input)
        val encodeBtn = view.findViewById<Button>(R.id.btn_encode)
        val decodeBtn = view.findViewById<Button>(R.id.btn_decode)
        val insertBtn = view.findViewById<Button>(R.id.btn_insert)
        val resultView = view.findViewById<TextView>(R.id.result_preview)
        val keyNameView = view.findViewById<TextView>(R.id.key_name)

        fun refreshKeyLabel() {
            keyNameView.text = "Key: $activeKeyName"
        }

        fun transformInput(): String {
            val text = inputField.text.toString()
            return if (currentMode == Mode.ENCODE) encode(text) else decode(text)
        }

        fun showPreview() {
            val text = inputField.text.toString()
            if (text.isBlank()) {
                resultView.visibility = View.GONE
                resultView.text = ""
                return
            }

            val output = transformInput()
            resultView.text = output
            resultView.visibility = View.VISIBLE
        }

        encodeBtn.setOnClickListener {
            currentMode = Mode.ENCODE
            showPreview()
        }

        decodeBtn.setOnClickListener {
            currentMode = Mode.DECODE
            showPreview()
        }

        insertBtn.setOnClickListener {
            val raw = inputField.text.toString()
            val textToInsert = if (resultView.visibility == View.VISIBLE && resultView.text.isNotBlank()) {
                resultView.text.toString()
            } else {
                raw
            }
            commitToTarget(textToInsert)
            inputField.setText("")
            resultView.visibility = View.GONE
        }

        view.findViewById<Button>(R.id.btn_prev_key).setOnClickListener {
            switchKey(-1)
            refreshKeyLabel()
            showPreview()
        }

        view.findViewById<Button>(R.id.btn_next_key).setOnClickListener {
            switchKey(1)
            refreshKeyLabel()
            showPreview()
        }

        setupStaticKeys(view, inputField, resultView)
        renderKeyboardRows(view, inputField, resultView)
        refreshKeyLabel()
    }

    private fun setupStaticKeys(view: View, inputField: EditText, resultView: TextView) {
        view.findViewById<Button>(R.id.key_symbols).setOnClickListener {
            symbolMode = !symbolMode
            renderKeyboardRows(view, inputField, resultView)
        }
        view.findViewById<Button>(R.id.key_comma).setOnClickListener { appendText(inputField, ",") }
        view.findViewById<Button>(R.id.key_period).setOnClickListener { appendText(inputField, ".") }
        view.findViewById<Button>(R.id.key_space).setOnClickListener { appendText(inputField, " ") }
        view.findViewById<Button>(R.id.key_enter).setOnClickListener {
            commitToTarget(inputField.text.toString())
            inputField.setText("")
            resultView.visibility = View.GONE
        }
    }

    private fun renderKeyboardRows(view: View, inputField: EditText, resultView: TextView) {
        val numberRow = view.findViewById<LinearLayout>(R.id.row_numbers)
        val topRow = view.findViewById<LinearLayout>(R.id.row_top)
        val middleRow = view.findViewById<LinearLayout>(R.id.row_middle)
        val bottomRow = view.findViewById<LinearLayout>(R.id.row_bottom)

        numberRow.removeAllViews()
        topRow.removeAllViews()
        middleRow.removeAllViews()
        bottomRow.removeAllViews()

        if (symbolMode) {
            addRow(numberRow, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), inputField)
            addRow(topRow, listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"), inputField)
            addRow(middleRow, listOf("*", "\"", "'", ":", ";", "!", "?", "~", "="), inputField)
            addSpecialBottomRow(bottomRow, inputField, resultView, listOf("<", ">", "_", "[", "]", "{", "}"))
        } else {
            addRow(numberRow, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), inputField)
            addRow(topRow, listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), inputField)
            addRow(middleRow, listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), inputField)
            addLetterBottomRow(bottomRow, inputField, resultView)
        }

        view.findViewById<Button>(R.id.key_symbols).text = if (symbolMode) "ABC" else "123"
    }

    private fun addRow(row: LinearLayout, values: List<String>, inputField: EditText) {
        values.forEach { value ->
            row.addView(makeKey(value, 1f) {
                appendText(inputField, if (shiftMode && value.length == 1 && value[0].isLetter()) value.uppercase() else value)
                if (shiftMode) shiftMode = false
            })
        }
    }

    private fun addLetterBottomRow(row: LinearLayout, inputField: EditText, resultView: TextView) {
        row.addView(makeKey(if (shiftMode) "SHIFT" else "shift", 1.25f) {
            shiftMode = !shiftMode
            renderKeyboardRows((row.parent as View), inputField, resultView)
        })
        listOf("z", "x", "c", "v", "b", "n", "m").forEach { value ->
            row.addView(makeKey(value, 1f) {
                appendText(inputField, if (shiftMode) value.uppercase() else value)
                if (shiftMode) {
                    shiftMode = false
                    renderKeyboardRows((row.parent as View), inputField, resultView)
                }
            })
        }
        row.addView(makeKey("BKSP", 1.45f) { backspace(inputField) })
    }

    private fun addSpecialBottomRow(row: LinearLayout, inputField: EditText, resultView: TextView, values: List<String>) {
        values.forEach { value -> row.addView(makeKey(value, 1f) { appendText(inputField, value) }) }
        row.addView(makeKey("BKSP", 1.45f) { backspace(inputField) })
    }

    private fun makeKey(label: String, weight: Float, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = if (label.length > 1) 10f else 15f
            setTextColor(0xfff6f7fb.toInt())
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setBackgroundColor(0xff272c35.toInt())
            gravity = Gravity.CENTER
            minHeight = 0
            minWidth = 0
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply {
                marginEnd = 4
            }
            setOnClickListener { onClick() }
        }
    }

    private fun appendText(inputField: EditText, text: String) {
        val start = inputField.selectionStart.coerceAtLeast(0)
        val end = inputField.selectionEnd.coerceAtLeast(0)
        val min = minOf(start, end)
        val max = maxOf(start, end)
        inputField.text.replace(min, max, text)
        inputField.setSelection(min + text.length)
    }

    private fun backspace(inputField: EditText) {
        val start = inputField.selectionStart.coerceAtLeast(0)
        val end = inputField.selectionEnd.coerceAtLeast(0)
        if (start != end) {
            inputField.text.delete(minOf(start, end), maxOf(start, end))
        } else if (start > 0) {
            inputField.text.delete(start - 1, start)
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun commitToTarget(text: String) {
        if (text.isBlank()) return
        val ic: InputConnection = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    private fun switchKey(direction: Int) {
        if (savedKeys.isEmpty()) return
        val currentIndex = savedKeys.indexOfFirst { it.name == activeKeyName }.let { if (it < 0) 0 else it }
        val nextIndex = Math.floorMod(currentIndex + direction, savedKeys.size)
        saveActiveKey(savedKeys[nextIndex])
    }
}
