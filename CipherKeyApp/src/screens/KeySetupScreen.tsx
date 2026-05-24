// src/screens/KeySetupScreen.tsx
import React, { useState, useEffect } from 'react';
import {
  View, Text, TextInput, ScrollView, TouchableOpacity,
  StyleSheet, Alert, Platform,
} from 'react-native';
import { useCipher } from '../context/CipherContext';
import { LETTERS, randomCipher, generateKeyFile, keyFileToString } from '../utils/cipher';
import RNFS from 'react-native-fs';
import ShareFile from 'react-native-share';

export default function KeySetupScreen() {
  const { cipherMap, setCipherMap, addKey, setActiveKey } = useCipher();
  const [localMap, setLocalMap] = useState<{ [k: string]: string }>({ ...cipherMap });
  const [keyName, setKeyName] = useState('MyKey');

  useEffect(() => {
    setLocalMap({ ...cipherMap });
  }, [cipherMap]);

  const updateLetter = (letter: string, value: string) => {
    setLocalMap(prev => ({ ...prev, [letter]: value }));
  };

  const handleRandomFill = () => {
    const rnd = randomCipher();
    setLocalMap(rnd);
  };

  const handleClearAll = () => {
    Alert.alert('Clear All', 'Are you sure you want to clear all cipher values?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Clear', style: 'destructive', onPress: () => setLocalMap({}) },
    ]);
  };

  const handleSave = async () => {
    const filled = Object.values(localMap).filter(v => v.trim()).length;
    if (filled < 5) {
      Alert.alert('Not enough', 'Please fill at least 5 letters before saving.');
      return;
    }
    await setCipherMap(localMap);
    Alert.alert('Saved!', 'Your cipher has been saved.');
  };

  const handleGenerateKey = async () => {
    const filled = Object.entries(localMap).filter(([, v]) => v.trim());
    if (filled.length < 5) {
      Alert.alert('Not enough', 'Fill at least 5 letters first.');
      return;
    }
    if (!keyName.trim()) {
      Alert.alert('Name required', 'Give your key a name.');
      return;
    }
    await setCipherMap(localMap);
    const keyFile = generateKeyFile(keyName.trim(), localMap);
    await addKey(keyFile);
    await setActiveKey(keyFile);

    const content = keyFileToString(keyFile);
    const filename = `${keyName.replace(/\s+/g, '_')}.cipherkey`;

    try {
      const path = `${RNFS.DocumentDirectoryPath}/${filename}`;
      await RNFS.writeFile(path, content, 'utf8');
      await ShareFile.open({
        title: filename,
        subject: filename,
        filename,
        url: Platform.OS === 'android' ? `file://${path}` : path,
        type: 'application/json',
        failOnCancel: false,
      });
    } catch (e) {
      Alert.alert('Key saved', `${filename} was saved and set as the active key.`);
    }
  };

  return (
    <ScrollView style={styles.container} keyboardShouldPersistTaps="handled">
      <View style={styles.header}>
        <View style={styles.logo}><Text style={styles.logoText}>Cφ</Text></View>
        <View>
          <Text style={styles.title}>CipherKey</Text>
          <Text style={styles.subtitle}>Define your A–Z cipher</Text>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>KEY NAME</Text>
        <TextInput
          style={styles.nameInput}
          value={keyName}
          onChangeText={setKeyName}
          placeholder="e.g. MySecretKey"
          placeholderTextColor="#666"
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>CIPHER MAPPING (A–Z)</Text>
        <View style={styles.grid}>
          {LETTERS.map(letter => (
            <View key={letter} style={styles.row}>
              <View style={styles.letterBadge}>
                <Text style={styles.letterText}>{letter}</Text>
              </View>
              <Text style={styles.eq}>=</Text>
              <TextInput
                style={styles.cipherInput}
                value={localMap[letter] || ''}
                onChangeText={v => updateLetter(letter, v)}
                placeholder="e.g. 12e"
                placeholderTextColor="#555"
                autoCapitalize="none"
                autoCorrect={false}
                maxLength={12}
              />
            </View>
          ))}
        </View>
      </View>

      <View style={styles.buttonRow}>
        <TouchableOpacity style={styles.btnOutline} onPress={handleRandomFill}>
          <Text style={styles.btnOutlineText}>Random Fill</Text>
        </TouchableOpacity>
        <TouchableOpacity style={styles.btnOutline} onPress={handleClearAll}>
          <Text style={styles.btnOutlineText}>Clear All</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.btnSave} onPress={handleSave}>
        <Text style={styles.btnSaveText}>Save Cipher</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.btnGenerate} onPress={handleGenerateKey}>
        <Text style={styles.btnGenerateText}>Generate & Share Key File</Text>
      </TouchableOpacity>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  header: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, backgroundColor: '#1e1e1e' },
  logo: { width: 40, height: 40, borderRadius: 10, backgroundColor: '#1DB954', alignItems: 'center', justifyContent: 'center' },
  logoText: { color: '#000', fontWeight: '800', fontSize: 15 },
  title: { color: '#fff', fontWeight: '700', fontSize: 18 },
  subtitle: { color: '#888', fontSize: 12 },
  section: { padding: 16 },
  label: { color: '#888', fontSize: 11, fontWeight: '600', letterSpacing: 0.8, marginBottom: 10 },
  nameInput: { backgroundColor: '#1e1e1e', borderRadius: 10, borderWidth: 1, borderColor: '#333', color: '#fff', padding: 12, fontSize: 15 },
  grid: { gap: 8 },
  row: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#1e1e1e', borderRadius: 10, padding: 10, gap: 8 },
  letterBadge: { width: 32, height: 32, borderRadius: 8, backgroundColor: '#1DB954', alignItems: 'center', justifyContent: 'center' },
  letterText: { color: '#000', fontWeight: '800', fontSize: 14 },
  eq: { color: '#555', fontSize: 16 },
  cipherInput: { flex: 1, backgroundColor: '#2a2a2a', borderRadius: 8, borderWidth: 1, borderColor: '#333', color: '#fff', paddingHorizontal: 10, paddingVertical: 6, fontSize: 13 },
  buttonRow: { flexDirection: 'row', gap: 10, paddingHorizontal: 16, marginBottom: 10 },
  btnOutline: { flex: 1, padding: 13, borderRadius: 10, borderWidth: 1, borderColor: '#333', alignItems: 'center' },
  btnOutlineText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  btnSave: { marginHorizontal: 16, padding: 14, borderRadius: 10, backgroundColor: '#2a2a2a', borderWidth: 1, borderColor: '#1DB954', alignItems: 'center', marginBottom: 10 },
  btnSaveText: { color: '#1DB954', fontSize: 14, fontWeight: '700' },
  btnGenerate: { marginHorizontal: 16, padding: 14, borderRadius: 10, backgroundColor: '#1DB954', alignItems: 'center' },
  btnGenerateText: { color: '#000', fontSize: 14, fontWeight: '700' },
});
