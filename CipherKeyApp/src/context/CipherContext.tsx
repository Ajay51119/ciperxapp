// src/context/CipherContext.tsx
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { CipherMap, KeyFile, LETTERS } from '../utils/cipher';

interface CipherContextType {
  cipherMap: CipherMap;
  setCipherMap: (map: CipherMap) => void;
  savedKeys: KeyFile[];
  addKey: (key: KeyFile) => void;
  removeKey: (name: string) => void;
  activeKey: KeyFile | null;
  setActiveKey: (key: KeyFile | null) => void;
}

const CipherContext = createContext<CipherContextType | null>(null);

const STORAGE_KEY_MAP = '@cipher_map';
const STORAGE_KEY_KEYS = '@saved_keys';
const STORAGE_KEY_ACTIVE = '@active_key';

export function CipherProvider({ children }: { children: ReactNode }) {
  const [cipherMap, setCipherMapState] = useState<CipherMap>({});
  const [savedKeys, setSavedKeys] = useState<KeyFile[]>([]);
  const [activeKey, setActiveKeyState] = useState<KeyFile | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const mapStr = await AsyncStorage.getItem(STORAGE_KEY_MAP);
        if (mapStr) setCipherMapState(JSON.parse(mapStr));

        const keysStr = await AsyncStorage.getItem(STORAGE_KEY_KEYS);
        if (keysStr) setSavedKeys(JSON.parse(keysStr));

        const activeStr = await AsyncStorage.getItem(STORAGE_KEY_ACTIVE);
        if (activeStr) setActiveKeyState(JSON.parse(activeStr));
      } catch {}
    })();
  }, []);

  const setCipherMap = async (map: CipherMap) => {
    setCipherMapState(map);
    await AsyncStorage.setItem(STORAGE_KEY_MAP, JSON.stringify(map));
  };

  const addKey = async (key: KeyFile) => {
    const updated = [...savedKeys.filter(k => k.name !== key.name), key];
    setSavedKeys(updated);
    await AsyncStorage.setItem(STORAGE_KEY_KEYS, JSON.stringify(updated));
  };

  const removeKey = async (name: string) => {
    const updated = savedKeys.filter(k => k.name !== name);
    setSavedKeys(updated);
    await AsyncStorage.setItem(STORAGE_KEY_KEYS, JSON.stringify(updated));
    if (activeKey?.name === name) {
      setActiveKeyState(null);
      await AsyncStorage.removeItem(STORAGE_KEY_ACTIVE);
    }
  };

  const setActiveKey = async (key: KeyFile | null) => {
    setActiveKeyState(key);
    if (key) {
      setCipherMapState(key.cipher);
      await AsyncStorage.setItem(STORAGE_KEY_MAP, JSON.stringify(key.cipher));
      await AsyncStorage.setItem(STORAGE_KEY_ACTIVE, JSON.stringify(key));
    } else {
      await AsyncStorage.removeItem(STORAGE_KEY_ACTIVE);
    }
  };

  return (
    <CipherContext.Provider value={{ cipherMap, setCipherMap, savedKeys, addKey, removeKey, activeKey, setActiveKey }}>
      {children}
    </CipherContext.Provider>
  );
}

export function useCipher() {
  const ctx = useContext(CipherContext);
  if (!ctx) throw new Error('useCipher must be used inside CipherProvider');
  return ctx;
}
