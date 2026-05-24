// src/utils/CipherBridge.ts
import { NativeModules, Platform } from 'react-native';
import { CipherMap, KeyFile } from './cipher';

const { CipherBridge } = NativeModules;

/**
 * Syncs cipher map to Android SharedPreferences
 * so the IME keyboard service can read it in real time.
 */
export async function syncCipherToKeyboard(map: CipherMap): Promise<void> {
  if (Platform.OS !== 'android') return;
  if (!CipherBridge) {
    console.warn('CipherBridge native module not found');
    return;
  }
  try {
    await CipherBridge.saveCipherMap(JSON.stringify(map));
  } catch (e) {
    console.error('CipherBridge.saveCipherMap error:', e);
  }
}

export async function syncKeyboardStateToNative(
  activeKeyName: string,
  map: CipherMap,
  savedKeys: KeyFile[],
): Promise<void> {
  if (Platform.OS !== 'android' || !CipherBridge) return;
  try {
    await CipherBridge.saveKeyboardState(
      activeKeyName || 'Manual',
      JSON.stringify(map),
      JSON.stringify(savedKeys),
    );
  } catch (e) {
    console.error('CipherBridge.saveKeyboardState error:', e);
  }
}

export async function isKeyboardEnabled(): Promise<boolean> {
  if (Platform.OS !== 'android' || !CipherBridge) return false;
  try {
    return await CipherBridge.isKeyboardEnabled();
  } catch {
    return false;
  }
}

export async function isKeyboardSelected(): Promise<boolean> {
  if (Platform.OS !== 'android' || !CipherBridge) return false;
  try {
    return await CipherBridge.isKeyboardSelected();
  } catch {
    return false;
  }
}

export async function openKeyboardSettings(): Promise<void> {
  if (Platform.OS !== 'android' || !CipherBridge) return;
  try {
    await CipherBridge.openKeyboardSettings();
  } catch {}
}

export async function showKeyboardPicker(): Promise<void> {
  if (Platform.OS !== 'android' || !CipherBridge) return;
  try {
    await CipherBridge.showKeyboardPicker();
  } catch {}
}
