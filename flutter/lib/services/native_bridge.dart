import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

class NativeBridge {
  NativeBridge._();

  static const MethodChannel _channel = MethodChannel('cipherkey/native');

  static Future<bool> canDrawOverlays() async {
    if (kIsWeb || !Platform.isAndroid) return false;
    return (await _channel.invokeMethod<bool>('canDrawOverlays')) ?? false;
  }

  static Future<void> openOverlaySettings() async {
    if (kIsWeb || !Platform.isAndroid) return;
    await _channel.invokeMethod('openOverlaySettings');
  }

  static Future<bool> isAccessibilityEnabled() async {
    if (kIsWeb || !Platform.isAndroid) return false;
    return (await _channel.invokeMethod<bool>('isAccessibilityEnabled')) ?? false;
  }

  static Future<void> openAccessibilitySettings() async {
    if (kIsWeb || !Platform.isAndroid) return;
    await _channel.invokeMethod('openAccessibilitySettings');
  }

  static Future<bool> requestScreenCapturePermission() async {
    if (kIsWeb || !Platform.isAndroid) return false;
    return (await _channel.invokeMethod<bool>('requestScreenCapturePermission')) ?? false;
  }

  static Future<void> startOverlay() async {
    if (kIsWeb || !Platform.isAndroid) return;
    await _channel.invokeMethod('startOverlay');
  }

  static Future<void> stopOverlay() async {
    if (kIsWeb || !Platform.isAndroid) return;
    await _channel.invokeMethod('stopOverlay');
  }

  static Future<void> syncKeyboardState({
    required String activeKeyName,
    required Map<String, String> cipherMap,
    required List<Map<String, dynamic>> savedKeys,
  }) async {
    if (kIsWeb || !Platform.isAndroid) return;
    await _channel.invokeMethod('syncKeyboardState', <String, dynamic>{
      'activeKeyName': activeKeyName,
      'cipherMap': cipherMap,
      'savedKeys': savedKeys,
    });
  }
}
