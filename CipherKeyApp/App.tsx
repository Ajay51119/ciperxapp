// App.tsx
import React, { useEffect } from 'react';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { StatusBar, View, Text } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { CipherProvider, useCipher } from './src/context/CipherContext';
import KeySetupScreen from './src/screens/KeySetupScreen';
import EncodeScreen from './src/screens/EncodeScreen';
import KeysScreen from './src/screens/KeysScreen';
import KeyboardSetupScreen from './src/screens/KeyboardSetupScreen';
import { syncKeyboardStateToNative } from './src/utils/CipherBridge';

const Tab = createBottomTabNavigator();

const DarkTheme = {
  ...DefaultTheme,
  colors: { ...DefaultTheme.colors, background: '#121212', card: '#1e1e1e', border: '#333', text: '#fff' },
};

function TabIcon({ label, focused }: { label: string; focused: boolean }) {
  const icons: Record<string, string> = {
    'Key Setup': '⚙',
    'Encode': '🔐',
    'Keys': '🗂',
    'Keyboard': '⌨',
  };
  return (
    <View style={{ alignItems: 'center' }}>
      <Text style={{ fontSize: 18, opacity: focused ? 1 : 0.4 }}>{icons[label]}</Text>
    </View>
  );
}

function AppTabs() {
  const { cipherMap, activeKey, savedKeys } = useCipher();

  // Auto-sync active .cipherkey state to the native keyboard service.
  useEffect(() => {
    const activeMap = activeKey?.cipher || cipherMap;
    if (Object.keys(activeMap).length > 0) {
      syncKeyboardStateToNative(activeKey?.name || 'Manual', activeMap, savedKeys);
    }
  }, [cipherMap, activeKey, savedKeys]);

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused }) => <TabIcon label={route.name} focused={focused} />,
        tabBarActiveTintColor: '#1DB954',
        tabBarInactiveTintColor: '#666',
        tabBarStyle: { backgroundColor: '#1e1e1e', borderTopColor: '#333', height: 60, paddingBottom: 8 },
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        headerStyle: { backgroundColor: '#1e1e1e' },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: '700' },
      })}>
      <Tab.Screen
        name="Key Setup"
        component={KeySetupScreen}
        options={{ title: 'Key Setup', headerTitle: 'CipherKey — Setup' }}
      />
      <Tab.Screen
        name="Encode"
        component={EncodeScreen}
        options={{ title: 'Encode', headerTitle: 'Encode / Decode' }}
      />
      <Tab.Screen
        name="Keys"
        component={KeysScreen}
        options={{ title: 'Keys', headerTitle: 'Manage Keys' }}
      />
      <Tab.Screen
        name="Keyboard"
        component={KeyboardSetupScreen}
        options={{ title: 'Keyboard', headerTitle: 'Keyboard Setup' }}
      />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <CipherProvider>
        <NavigationContainer theme={DarkTheme}>
          <StatusBar barStyle="light-content" backgroundColor="#121212" />
          <AppTabs />
        </NavigationContainer>
      </CipherProvider>
    </SafeAreaProvider>
  );
}
