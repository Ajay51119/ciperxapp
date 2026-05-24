# CipherKey — React Native Custom Keyboard App

A custom cipher keyboard app (like Grammarly) that lets users define A-Z cipher mappings,
generate shareable key files, and encode/decode text in **any app** using a custom IME keyboard.

---

## 📁 Project Structure

```
CipherKeyApp/
├── App.tsx                          # Root with navigation + cipher sync
├── package.json
├── src/
│   ├── context/
│   │   └── CipherContext.tsx        # Global cipher state + AsyncStorage
│   ├── screens/
│   │   ├── KeySetupScreen.tsx       # Define A-Z cipher + generate key file
│   │   ├── EncodeScreen.tsx         # Encode / Decode text
│   │   ├── KeysScreen.tsx           # Import/manage .cipherkey files
│   │   └── KeyboardSetupScreen.tsx  # Guide to enable keyboard + sync
│   └── utils/
│       ├── cipher.ts                # Encode/decode logic
│       └── CipherBridge.ts          # RN ↔ Android native module bridge
│
├── android/app/src/main/
│   ├── AndroidManifest.xml          # ← IME service registered here
│   ├── java/com/cipherkeyapp/keyboard/
│   │   ├── CipherKeyboardService.kt # ← THE CUSTOM KEYBOARD (InputMethodService)
│   │   ├── CipherBridgeModule.kt    # Native module: RN → SharedPrefs
│   │   └── CipherBridgePackage.kt   # Package registration
│   └── res/
│       ├── layout/keyboard_layout.xml  # Keyboard UI
│       └── xml/method.xml              # IME metadata
│
└── ios/CipherKeyboardExtension/
    └── KeyboardViewController.swift    # iOS custom keyboard extension
```

---

## 🚀 Setup

### 1. Install dependencies
```bash
npm install
cd ios && pod install && cd ..
```

### 2. Register the native module in MainApplication.kt
```kotlin
// android/app/src/main/java/com/cipherkeyapp/MainApplication.kt
import com.cipherkeyapp.keyboard.CipherBridgePackage

override fun getPackages(): List<ReactPackage> = listOf(
    MainReactPackage(),
    CipherBridgePackage()   // ← Add this line
)
```

### 3. Run the app
```bash
npx react-native run-android
# or
npx react-native run-ios
```

---

## 📱 How to Enable the Custom Keyboard

### Android
1. Open the app → go to **Keyboard** tab
2. Tap **Open Keyboard Settings**
3. Go to: Settings → General Management → Keyboard → On-screen keyboard
4. Enable **CipherKey Keyboard**
5. When typing anywhere, tap the globe 🌐 icon to switch to CipherKey

### iOS
1. Go to Settings → General → Keyboard → Keyboards → Add New Keyboard
2. Select **CipherKey**
3. Tap it → enable **Allow Full Access** (required to read cipher from shared storage)

---

## 🔐 How the Cipher Works

```
User defines: A=12@ B=45# C=78$ ...

"HELLO" → "183@·654#·109$·109$·320%" (encoded)
Encoded  → "HELLO"                     (decoded)

Delimiter: · (middle dot)
Space:     | (pipe)
```

Key files are saved as `.cipherkey` (JSON):
```json
{
  "version": 1,
  "name": "MyKey",
  "cipher": { "A": "12@", "B": "45#", ... },
  "created": "2024-01-01T00:00:00.000Z"
}
```

---

## ⌨️ Keyboard Features

The CipherKey keyboard appears like Grammarly's overlay bar with:

| Button | Action |
|--------|--------|
| **Cφ logo** | Encodes currently selected text / full text |
| **Encode** | Encode typed text in keyboard input |
| **Decode** | Decode encoded text |
| **Insert ↑** | Send result to the active app's text field |
| **Clear** | Clear keyboard input |

---

## 🔄 How cipher syncs to keyboard

```
React Native App (cipherMap)
         │
         ▼  [CipherBridgeModule.saveCipherMap()]
Android SharedPreferences ("CipherKeyApp")
         │
         ▼  [CipherKeyboardService.loadCipher()]
Custom Keyboard IME  ← reads same SharedPrefs
```

On iOS, use an **App Group** shared `UserDefaults` to pass data between the main app and keyboard extension.

---

## 🛠 iOS App Group Setup (required for iOS)

1. In Xcode: Select main target → Signing & Capabilities → + App Group
2. Add `group.com.cipherkeyapp`
3. Do the same for the **CipherKeyboardExtension** target
4. In the main app, write cipher to:
   ```swift
   UserDefaults(suiteName: "group.com.cipherkeyapp")?.set(data, forKey: "cipher_map")
   ```
5. The keyboard extension reads from the same suite (already implemented in `KeyboardViewController.swift`)

---

## 📦 Key Dependencies

| Package | Purpose |
|---------|---------|
| `@react-native-async-storage/async-storage` | Persist cipher + keys |
| `@react-navigation/bottom-tabs` | Tab navigation |
| `react-native-document-picker` | Import .cipherkey files |
| `react-native-fs` | Read/write key files |
| `react-native-share` | Share generated key files |
| `@react-native-clipboard/clipboard` | Copy encoded output |
