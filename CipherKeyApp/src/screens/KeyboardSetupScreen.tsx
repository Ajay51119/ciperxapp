// src/screens/KeyboardSetupScreen.tsx
import React, { useEffect, useState } from 'react';
import {
  Alert,
  AppState,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { useCipher } from '../context/CipherContext';
import {
  isKeyboardEnabled,
  isKeyboardSelected,
  openKeyboardSettings,
  showKeyboardPicker,
  syncKeyboardStateToNative,
} from '../utils/CipherBridge';

export default function KeyboardSetupScreen() {
  const { cipherMap, activeKey, savedKeys } = useCipher();
  const [kbEnabled, setKbEnabled] = useState(false);
  const [kbSelected, setKbSelected] = useState(false);

  useEffect(() => {
    checkStatus();
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        checkStatus();
      }
    });

    return () => subscription.remove();
  }, []);

  const checkStatus = async () => {
    const enabled = await isKeyboardEnabled();
    const selected = await isKeyboardSelected();
    setKbEnabled(enabled);
    setKbSelected(selected);
  };

  const handleSync = async () => {
    const activeMap = activeKey?.cipher || cipherMap;
    await syncKeyboardStateToNative(activeKey?.name || 'Manual', activeMap, savedKeys);
    Alert.alert('Synced!', 'Active key state has been sent to the keyboard service.');
  };

  const handleOpenSettings = async () => {
    await openKeyboardSettings();
    setTimeout(checkStatus, 2000);
  };

  const handleKeyboardPicker = async () => {
    if (!kbEnabled) {
      Alert.alert(
        'Enable CipherKey first',
        'Turn on CipherKey in Android keyboard settings, then come back and choose it as your active keyboard.',
      );
      await openKeyboardSettings();
      return;
    }

    await showKeyboardPicker();
    setTimeout(checkStatus, 1000);
  };

  const steps = [
    {
      num: '1',
      title: 'Enable CipherKey Keyboard',
      desc: 'Turn CipherKey on in Android keyboard settings. This only needs to be done once unless you turn it off.',
      action: 'Open Keyboard Settings',
      onPress: handleOpenSettings,
      done: kbEnabled,
    },
    {
      num: '2',
      title: 'Choose CipherKey as active keyboard',
      desc: 'Open the Android keyboard picker and select CipherKey. Android keeps it selected until you switch keyboards again.',
      action: 'Choose Active Keyboard',
      onPress: handleKeyboardPicker,
      done: kbSelected,
    },
    {
      num: '3',
      title: 'Sync your cipher',
      desc: 'After setting up your cipher in Key Setup, sync it so the keyboard service uses it.',
      action: 'Sync Cipher to Keyboard',
      onPress: handleSync,
      done: Object.keys(activeKey?.cipher || cipherMap).length >= 5,
    },
    {
      num: '4',
      title: 'Use in any app',
      desc: 'Open WhatsApp, Telegram, etc. CipherKey appears as the keyboard with the Cphi toolbar for encode/decode.',
      action: null,
      onPress: null,
      done: kbEnabled && kbSelected,
    },
  ];

  return (
    <ScrollView style={styles.container}>
      <View style={styles.heroCard}>
        <View style={styles.heroLogo}>
          <Text style={styles.heroLogoText}>Cphi</Text>
        </View>
        <Text style={styles.heroTitle}>CipherKey Keyboard</Text>
        <Text style={styles.heroSub}>
          A custom keyboard that stays available across apps after you enable and choose it.
        </Text>
        <View style={[styles.statusPill, kbEnabled ? styles.statusOk : styles.statusOff]}>
          <Text style={[styles.statusText, kbEnabled ? styles.statusTextOk : styles.statusTextOff]}>
            {kbEnabled ? 'Keyboard enabled' : 'Keyboard not enabled'}
          </Text>
        </View>
        <View style={[styles.statusPill, kbSelected ? styles.statusOk : styles.statusOff, styles.statusGap]}>
          <Text style={[styles.statusText, kbSelected ? styles.statusTextOk : styles.statusTextOff]}>
            {kbSelected ? 'CipherKey is active' : 'CipherKey is not active'}
          </Text>
        </View>
        <TouchableOpacity style={styles.primaryBtn} onPress={handleKeyboardPicker}>
          <Text style={styles.primaryBtnText}>
            {kbSelected ? 'Switch Keyboard' : 'Choose CipherKey Keyboard'}
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>SETUP STEPS</Text>
        {steps.map((step) => (
          <View key={step.num} style={[styles.stepCard, step.done && styles.stepCardDone]}>
            <View style={[styles.stepNum, step.done && styles.stepNumDone]}>
              <Text style={[styles.stepNumText, step.done && styles.stepNumTextDone]}>
                {step.done ? 'OK' : step.num}
              </Text>
            </View>
            <View style={styles.stepContent}>
              <Text style={styles.stepTitle}>{step.title}</Text>
              <Text style={styles.stepDesc}>{step.desc}</Text>
              {step.action && step.onPress && (
                <TouchableOpacity style={styles.stepBtn} onPress={step.onPress}>
                  <Text style={styles.stepBtnText}>{step.action}</Text>
                </TouchableOpacity>
              )}
            </View>
          </View>
        ))}
      </View>

      {Platform.OS === 'ios' && (
        <View style={styles.section}>
          <View style={styles.iosNote}>
            <Text style={styles.iosNoteTitle}>iOS Setup</Text>
            <Text style={styles.iosNoteText}>
              On iOS, go to Settings, General, Keyboard, Keyboards, Add New Keyboard, then choose CipherKey.
              {'\n\n'}
              Then allow Full Access to let the keyboard read your cipher map.
            </Text>
          </View>
        </View>
      )}

      <View style={styles.section}>
        <Text style={styles.label}>HOW THE LOGO WORKS</Text>
        <View style={styles.demoCard}>
          <View style={styles.demoRow}>
            <View style={styles.demoLogo}>
              <Text style={styles.demoLogoText}>Cphi</Text>
            </View>
            <View style={styles.demoInfo}>
              <Text style={styles.demoTitle}>CipherKey</Text>
              <Text style={styles.demoSub}>Tap to encode/decode selected text</Text>
            </View>
            <View style={styles.demoBadge}>
              <Text style={styles.demoBadgeText}>Encode</Text>
            </View>
          </View>
          <Text style={styles.demoHint}>
            This bar appears inside the CipherKey keyboard. Tap the Cphi logo to encode or decode typed or selected text.
          </Text>
        </View>
      </View>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  heroCard: { margin: 16, backgroundColor: '#1e1e1e', borderRadius: 16, padding: 20, alignItems: 'center' },
  heroLogo: { minWidth: 56, height: 56, borderRadius: 14, backgroundColor: '#1DB954', alignItems: 'center', justifyContent: 'center', marginBottom: 12, paddingHorizontal: 8 },
  heroLogoText: { color: '#000', fontWeight: '800', fontSize: 15 },
  heroTitle: { color: '#fff', fontSize: 20, fontWeight: '700', marginBottom: 8 },
  heroSub: { color: '#888', fontSize: 13, textAlign: 'center', lineHeight: 20, marginBottom: 14 },
  statusPill: { paddingHorizontal: 14, paddingVertical: 6, borderRadius: 20 },
  statusGap: { marginTop: 8 },
  statusOk: { backgroundColor: 'rgba(29,185,84,0.15)' },
  statusOff: { backgroundColor: '#2a2a2a' },
  statusText: { fontWeight: '600', fontSize: 13 },
  statusTextOk: { color: '#1DB954' },
  statusTextOff: { color: '#888' },
  primaryBtn: { backgroundColor: '#1DB954', paddingHorizontal: 16, paddingVertical: 10, borderRadius: 8, marginTop: 14 },
  primaryBtnText: { color: '#000', fontWeight: '800', fontSize: 13 },
  section: { paddingHorizontal: 16, paddingBottom: 8 },
  label: { color: '#888', fontSize: 11, fontWeight: '600', letterSpacing: 0.8, marginBottom: 10 },
  stepCard: { flexDirection: 'row', gap: 14, backgroundColor: '#1e1e1e', borderRadius: 12, padding: 14, marginBottom: 10, borderWidth: 1, borderColor: '#333' },
  stepCardDone: { borderColor: 'rgba(29,185,84,0.3)', backgroundColor: 'rgba(29,185,84,0.05)' },
  stepNum: { width: 28, height: 28, borderRadius: 14, backgroundColor: '#2a2a2a', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  stepNumDone: { backgroundColor: '#1DB954' },
  stepNumText: { color: '#888', fontWeight: '700', fontSize: 12 },
  stepNumTextDone: { color: '#000' },
  stepContent: { flex: 1 },
  stepTitle: { color: '#fff', fontWeight: '600', fontSize: 15, marginBottom: 4 },
  stepDesc: { color: '#888', fontSize: 12, lineHeight: 18, marginBottom: 10 },
  stepBtn: { backgroundColor: '#1DB954', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 8, alignSelf: 'flex-start' },
  stepBtnText: { color: '#000', fontWeight: '700', fontSize: 13 },
  iosNote: { backgroundColor: '#1e1e1e', borderRadius: 12, padding: 14, borderWidth: 1, borderColor: '#333' },
  iosNoteTitle: { color: '#fff', fontWeight: '600', fontSize: 15, marginBottom: 8 },
  iosNoteText: { color: '#888', fontSize: 13, lineHeight: 20 },
  demoCard: { backgroundColor: '#1e1e1e', borderRadius: 12, padding: 14 },
  demoRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 12 },
  demoLogo: { minWidth: 36, height: 36, borderRadius: 9, backgroundColor: '#1DB954', alignItems: 'center', justifyContent: 'center', paddingHorizontal: 6 },
  demoLogoText: { color: '#000', fontWeight: '800', fontSize: 11 },
  demoInfo: { flex: 1 },
  demoTitle: { color: '#fff', fontWeight: '600', fontSize: 14 },
  demoSub: { color: '#888', fontSize: 11 },
  demoBadge: { backgroundColor: 'rgba(29,185,84,0.2)', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 8 },
  demoBadgeText: { color: '#1DB954', fontSize: 12, fontWeight: '600' },
  demoHint: { color: '#888', fontSize: 12, lineHeight: 18 },
});
