// src/screens/KeysScreen.tsx
import React from 'react';
import {
  Alert,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import DocumentPicker from 'react-native-document-picker';
import RNFS from 'react-native-fs';
import ShareFile from 'react-native-share';
import { useCipher } from '../context/CipherContext';
import { keyFileToString, KeyFile, parseKeyFile } from '../utils/cipher';

export default function KeysScreen() {
  const { savedKeys, addKey, removeKey, activeKey, setActiveKey } = useCipher();

  const handleImportKey = async () => {
    try {
      const result = await DocumentPicker.pickSingle({
        type: [DocumentPicker.types.allFiles],
        copyTo: 'cachesDirectory',
      });
      const uri = result.fileCopyUri || result.uri;
      const content = await RNFS.readFile(uri, 'utf8');
      const keyFile = parseKeyFile(content);

      if (!keyFile) {
        Alert.alert('Invalid file', 'This does not appear to be a valid .cipherkey file.');
        return;
      }

      await addKey(keyFile);
      await setActiveKey(keyFile);
      Alert.alert('Key imported', `"${keyFile.name}" has been added and activated.`);
    } catch (e: any) {
      if (!DocumentPicker.isCancel(e)) {
        Alert.alert('Import failed', 'Could not read this .cipherkey file.');
      }
    }
  };

  const handleActivate = async (key: KeyFile) => {
    await setActiveKey(key);
    Alert.alert('Key activated', `"${key.name}" is now active.`);
  };

  const handleDeactivate = async () => {
    await setActiveKey(null);
    Alert.alert('Deactivated', 'No .cipherkey file is active. Manual cipher values remain available.');
  };

  const handleDelete = (key: KeyFile) => {
    Alert.alert('Delete key', `Remove "${key.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => removeKey(key.name) },
    ]);
  };

  const handleShareKey = async (key: KeyFile) => {
    const filename = `${key.name.replace(/\s+/g, '_')}.cipherkey`;
    const path = `${RNFS.DocumentDirectoryPath}/${filename}`;

    try {
      await RNFS.writeFile(path, keyFileToString(key), 'utf8');
      await ShareFile.open({
        title: filename,
        subject: filename,
        filename,
        url: Platform.OS === 'android' ? `file://${path}` : path,
        type: 'application/json',
        failOnCancel: false,
      });
    } catch {
      Alert.alert('Share failed', 'Could not share this key file.');
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.section}>
        <TouchableOpacity style={styles.importBtn} onPress={handleImportKey}>
          <Text style={styles.importIcon}>+</Text>
          <Text style={styles.importText}>Import .cipherkey File</Text>
        </TouchableOpacity>
      </View>

      {activeKey && (
        <View style={styles.section}>
          <Text style={styles.label}>ACTIVE KEY</Text>
          <View style={styles.activeCard}>
            <View style={styles.keyIcon}><Text style={styles.keyIconText}>KEY</Text></View>
            <View style={styles.keyInfo}>
              <Text style={styles.keyName}>{activeKey.name}</Text>
              <Text style={styles.keyMeta}>
                {Object.keys(activeKey.cipher).length} letters mapped - Active
              </Text>
            </View>
            <TouchableOpacity style={styles.deactivateBtn} onPress={handleDeactivate}>
              <Text style={styles.deactivateBtnText}>Deactivate</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      <View style={styles.section}>
        <Text style={styles.label}>SAVED KEYS ({savedKeys.length})</Text>
        {savedKeys.length === 0 ? (
          <View style={styles.emptyBox}>
            <Text style={styles.emptyText}>No keys yet.</Text>
            <Text style={styles.emptySubText}>
              Create one in Key Setup or import a .cipherkey file.
            </Text>
          </View>
        ) : (
          savedKeys.map(key => {
            const isActive = activeKey?.name === key.name;
            return (
              <View key={key.name} style={[styles.keyCard, isActive && styles.keyCardActive]}>
                <View style={[styles.keyIcon, isActive && styles.keyIconActive]}>
                  <Text style={styles.keyIconText}>KEY</Text>
                </View>
                <View style={styles.keyInfo}>
                  <Text style={styles.keyName}>{key.name}</Text>
                  <Text style={styles.keyMeta}>
                    {Object.keys(key.cipher).length} letters - {new Date(key.created).toLocaleDateString()}
                  </Text>
                </View>
                <View style={styles.keyActions}>
                  {isActive ? (
                    <View style={styles.activeBadge}>
                      <Text style={styles.activeBadgeText}>Active</Text>
                    </View>
                  ) : (
                    <TouchableOpacity style={styles.activateBtn} onPress={() => handleActivate(key)}>
                      <Text style={styles.activateBtnText}>Use</Text>
                    </TouchableOpacity>
                  )}
                  <TouchableOpacity style={styles.shareBtn} onPress={() => handleShareKey(key)}>
                    <Text style={styles.shareBtnText}>Share</Text>
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.deleteBtn} onPress={() => handleDelete(key)}>
                    <Text style={styles.deleteBtnText}>X</Text>
                  </TouchableOpacity>
                </View>
              </View>
            );
          })
        )}
      </View>

      <View style={styles.section}>
        <Text style={styles.label}>HOW TO USE</Text>
        <View style={styles.infoBox}>
          <Text style={styles.infoStep}>1. Create your cipher in Key Setup.</Text>
          <Text style={styles.infoStep}>2. Generate and share the .cipherkey file.</Text>
          <Text style={styles.infoStep}>3. The other person imports it here and taps Use.</Text>
          <Text style={styles.infoStep}>4. The active key name appears in the app and native keyboard.</Text>
        </View>
      </View>

      <View style={{ height: 40 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#101114' },
  section: { padding: 16, paddingBottom: 8 },
  label: { color: '#8b93a3', fontSize: 11, fontWeight: '700', letterSpacing: 0.8, marginBottom: 10 },
  importBtn: {
    flexDirection: 'row', alignItems: 'center', gap: 10,
    borderWidth: 1.5, borderColor: '#20d466', borderStyle: 'dashed',
    borderRadius: 12, padding: 16, justifyContent: 'center',
    backgroundColor: 'rgba(32,212,102,0.08)',
  },
  importIcon: { color: '#20d466', fontSize: 22, fontWeight: '300' },
  importText: { color: '#20d466', fontWeight: '700', fontSize: 15 },
  activeCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: 'rgba(32,212,102,0.11)', borderRadius: 12,
    borderWidth: 1, borderColor: 'rgba(32,212,102,0.35)', padding: 14,
  },
  keyCard: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    backgroundColor: '#1a1d23', borderRadius: 12,
    borderWidth: 1, borderColor: '#303642', padding: 14, marginBottom: 8,
  },
  keyCardActive: { borderColor: 'rgba(32,212,102,0.35)' },
  keyIcon: { width: 38, height: 38, borderRadius: 10, backgroundColor: '#272c35', alignItems: 'center', justifyContent: 'center' },
  keyIconActive: { backgroundColor: 'rgba(32,212,102,0.2)' },
  keyIconText: { color: '#20d466', fontSize: 10, fontWeight: '900' },
  keyInfo: { flex: 1 },
  keyName: { color: '#fff', fontWeight: '700', fontSize: 15 },
  keyMeta: { color: '#8b93a3', fontSize: 12, marginTop: 2 },
  keyActions: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  activateBtn: { backgroundColor: '#20d466', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6 },
  activateBtnText: { color: '#06110a', fontWeight: '800', fontSize: 13 },
  deactivateBtn: { borderWidth: 1, borderColor: '#4a5261', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 6 },
  deactivateBtnText: { color: '#c7ccd7', fontSize: 13 },
  activeBadge: { backgroundColor: 'rgba(32,212,102,0.18)', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 5 },
  activeBadgeText: { color: '#20d466', fontSize: 12, fontWeight: '700' },
  shareBtn: { borderWidth: 1, borderColor: '#4a5261', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 5 },
  shareBtnText: { color: '#e5e8ef', fontSize: 12, fontWeight: '700' },
  deleteBtn: { width: 30, height: 30, borderRadius: 8, backgroundColor: '#272c35', alignItems: 'center', justifyContent: 'center' },
  deleteBtnText: { color: '#b9bfca', fontSize: 12, fontWeight: '800' },
  emptyBox: { backgroundColor: '#1a1d23', borderRadius: 12, padding: 24, alignItems: 'center' },
  emptyText: { color: '#c7ccd7', fontWeight: '700', fontSize: 15 },
  emptySubText: { color: '#737986', fontSize: 13, textAlign: 'center', marginTop: 6 },
  infoBox: { backgroundColor: '#1a1d23', borderRadius: 12, padding: 14, gap: 8 },
  infoStep: { color: '#c7ccd7', fontSize: 13, lineHeight: 20 },
});
