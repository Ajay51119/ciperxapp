import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/cipher_key_file.dart';
import '../services/native_bridge.dart';

class CipherStore extends ChangeNotifier {
  static const _savedKeysKey = 'cipher.savedKeys';
  static const _activeKeyKey = 'cipher.activeKey';

  List<CipherKeyFile> savedKeys = <CipherKeyFile>[];
  CipherKeyFile? activeKey;
  bool overlayEnabled = false;
  bool accessibilityEnabled = false;
  bool overlayPermissionGranted = false;

  Future<void> bootstrap() async {
    final prefs = await SharedPreferences.getInstance();

    final saved = prefs.getStringList(_savedKeysKey) ?? <String>[];
    savedKeys = saved
        .map((entry) => CipherKeyFile.fromJson(jsonDecode(entry) as Map<String, dynamic>))
        .toList();

    final active = prefs.getString(_activeKeyKey);
    if (active != null) {
      activeKey = CipherKeyFile.fromJson(jsonDecode(active) as Map<String, dynamic>);
    }

    overlayPermissionGranted = await NativeBridge.canDrawOverlays();
    accessibilityEnabled = await NativeBridge.isAccessibilityEnabled();
    overlayEnabled = false;

    notifyListeners();
  }

  Map<String, String> get activeCipher => activeKey?.cipher ?? const <String, String>{};

  Future<void> addOrReplaceKey(CipherKeyFile key) async {
    savedKeys.removeWhere((item) => item.name == key.name);
    savedKeys.insert(0, key);
    await _persistKeys();
    await setActiveKey(key);
  }

  Future<void> setActiveKey(CipherKeyFile? key) async {
    activeKey = key;
    final prefs = await SharedPreferences.getInstance();
    if (key == null) {
      await prefs.remove(_activeKeyKey);
    } else {
      await prefs.setString(_activeKeyKey, jsonEncode(key.toJson()));
    }
    await NativeBridge.syncKeyboardState(
      activeKeyName: key?.name ?? 'Manual',
      cipherMap: activeCipher,
      savedKeys: savedKeys.map((e) => e.toJson()).toList(),
    );
    notifyListeners();
  }

  Future<void> saveGeneratedKey({
    required String name,
    required Map<String, String> cipher,
  }) async {
    await addOrReplaceKey(CipherKeyFile.generate(name: name, cipher: cipher));
  }

  Future<void> toggleOverlay(bool enabled) async {
    overlayPermissionGranted = await NativeBridge.canDrawOverlays();
    if (enabled) {
      if (!overlayPermissionGranted) {
        await NativeBridge.openOverlaySettings();
        notifyListeners();
        return;
      }
      await NativeBridge.syncKeyboardState(
        activeKeyName: activeKey?.name ?? 'Manual',
        cipherMap: activeCipher,
        savedKeys: savedKeys.map((e) => e.toJson()).toList(),
      );
      final capturePermissionGranted = await NativeBridge.requestScreenCapturePermission();
      if (!capturePermissionGranted) {
        notifyListeners();
        return;
      }
      await NativeBridge.startOverlay();
      overlayEnabled = true;
    } else {
      await NativeBridge.stopOverlay();
      overlayEnabled = false;
    }
    notifyListeners();
  }

  Future<void> refreshPermissions() async {
    overlayPermissionGranted = await NativeBridge.canDrawOverlays();
    accessibilityEnabled = await NativeBridge.isAccessibilityEnabled();
    notifyListeners();
  }

  Future<void> _persistKeys() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setStringList(
      _savedKeysKey,
      savedKeys.map((key) => jsonEncode(key.toJson())).toList(),
    );
    notifyListeners();
  }
}
