// src/screens/EncodeScreen.tsx
import React, { useState } from 'react';
import {
  View, Text, TextInput, TouchableOpacity, ScrollView,
  StyleSheet, Alert,
} from 'react-native';
import Clipboard from '@react-native-clipboard/clipboard';
import { useCipher } from '../context/CipherContext';
import { encodeText, decodeText } from '../utils/cipher';

export default function EncodeScreen() {
  const { cipherMap, activeKey } = useCipher();
  const [input, setInput] = useState('');
  const [mode, setMode] = useState<'encode' | 'decode'>('encode');

  const activeMap = activeKey?.cipher || cipherMap;
  const hasMap = Object.keys(activeMap).length > 0;

  const output = (() => {
    if (!input || !hasMap) return '';
    if (mode === 'encode') return encodeText(input, activeMap);
    return decodeText(input, activeMap);
  })();

  const copyOutput = () => {
    if (!output) return;
    Clipboard.setString(output);
    Alert.alert('Copied!', 'Result copied to clipboard.');
  };

  const pasteInput = async () => {
    const text = await Clipboard.getString();
    setInput(text);
  };

  const swapMode = () => {
    setInput(output);
    setMode(m => m === 'encode' ? 'decode' : 'encode');
  };

  return (
    <ScrollView style={styles.container} keyboardShouldPersistTaps="handled">
      <View style={styles.modeToggle}>
        <TouchableOpacity
          style={[styles.modeBtn, mode === 'encode' && styles.modeBtnActive]}
          onPress={() => setMode('encode')}>
          <Text style={[styles.modeBtnText, mode === 'encode' && styles.modeBtnTextActive]}>Encode</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.modeBtn, mode === 'decode' && styles.modeBtnActive]}
          onPress={() => setMode('decode')}>
          <Text style={[styles.modeBtnText, mode === 'decode' && styles.modeBtnTextActive]}>Decode</Text>
        </TouchableOpacity>
      </View>

      {!hasMap && (
        <View style={styles.warningBox}>
          <Text style={styles.warningText}>⚠ No cipher loaded. Go to Key Setup first.</Text>
        </View>
      )}

      {activeKey && (
        <View style={styles.activeKeyBadge}>
          <Text style={styles.activeKeyText}>Using key: {activeKey.name}</Text>
        </View>
      )}

      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.label}>{mode === 'encode' ? 'PLAIN TEXT' : 'ENCODED TEXT'}</Text>
          <TouchableOpacity onPress={pasteInput}>
            <Text style={styles.actionLink}>Paste</Text>
          </TouchableOpacity>
        </View>
        <TextInput
          style={styles.textArea}
          value={input}
          onChangeText={setInput}
          placeholder={mode === 'encode' ? 'Type your message...' : 'Paste encoded text...'}
          placeholderTextColor="#555"
          multiline
          autoCapitalize="none"
          autoCorrect={false}
          textAlignVertical="top"
        />
      </View>

      <View style={styles.swapRow}>
        <View style={styles.divider} />
        <TouchableOpacity style={styles.swapBtn} onPress={swapMode}>
          <Text style={styles.swapIcon}>⇅</Text>
        </TouchableOpacity>
        <View style={styles.divider} />
      </View>

      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.label}>{mode === 'encode' ? 'ENCODED OUTPUT' : 'DECODED OUTPUT'}</Text>
          <TouchableOpacity onPress={copyOutput} disabled={!output}>
            <Text style={[styles.actionLink, !output && { opacity: 0.3 }]}>Copy</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.outputBox}>
          {output ? (
            <Text style={styles.outputText} selectable>{output}</Text>
          ) : (
            <Text style={styles.outputEmpty}>
              {hasMap ? 'Output will appear here...' : 'No cipher loaded'}
            </Text>
          )}
        </View>
      </View>

      {output ? (
        <TouchableOpacity style={styles.copyBtn} onPress={copyOutput}>
          <Text style={styles.copyBtnText}>Copy Result</Text>
        </TouchableOpacity>
      ) : null}

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  modeToggle: { flexDirection: 'row', margin: 16, backgroundColor: '#1e1e1e', borderRadius: 10, padding: 4, gap: 4 },
  modeBtn: { flex: 1, padding: 10, borderRadius: 8, alignItems: 'center' },
  modeBtnActive: { backgroundColor: '#1DB954' },
  modeBtnText: { color: '#888', fontWeight: '600', fontSize: 14 },
  modeBtnTextActive: { color: '#000' },
  warningBox: { marginHorizontal: 16, backgroundColor: '#2a1a00', borderRadius: 10, padding: 12, marginBottom: 8 },
  warningText: { color: '#f90', fontSize: 13 },
  activeKeyBadge: { marginHorizontal: 16, backgroundColor: 'rgba(29,185,84,0.1)', borderRadius: 8, padding: 8, marginBottom: 4 },
  activeKeyText: { color: '#1DB954', fontSize: 12, fontWeight: '600' },
  section: { paddingHorizontal: 16, paddingBottom: 8 },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  label: { color: '#888', fontSize: 11, fontWeight: '600', letterSpacing: 0.8 },
  actionLink: { color: '#1DB954', fontSize: 13, fontWeight: '600' },
  textArea: {
    backgroundColor: '#1e1e1e', borderRadius: 10, borderWidth: 1,
    borderColor: '#333', color: '#fff', padding: 12, fontSize: 15,
    minHeight: 110, textAlignVertical: 'top',
  },
  swapRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, marginVertical: 8 },
  divider: { flex: 1, height: 1, backgroundColor: '#333' },
  swapBtn: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#1e1e1e', borderWidth: 1, borderColor: '#333', alignItems: 'center', justifyContent: 'center', marginHorizontal: 12 },
  swapIcon: { color: '#1DB954', fontSize: 18 },
  outputBox: { backgroundColor: '#1e1e1e', borderRadius: 10, borderWidth: 1, borderColor: '#333', padding: 12, minHeight: 80 },
  outputText: { color: '#fff', fontSize: 14, lineHeight: 22 },
  outputEmpty: { color: '#555', fontSize: 14, fontStyle: 'italic' },
  copyBtn: { marginHorizontal: 16, marginTop: 8, padding: 14, borderRadius: 10, backgroundColor: '#1DB954', alignItems: 'center' },
  copyBtnText: { color: '#000', fontWeight: '700', fontSize: 14 },
});
