import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  Text,
  StyleSheet,
  TextInput,
  Button,
  View,
  Alert,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const COOKIE_STORAGE_KEY = '@user_cookie';

const SettingsScreen = () => {
  const [cookieInput, setCookieInput] = useState('');
  const [storedCookie, setStoredCookie] = useState('');

  useEffect(() => {
    // Load the cookie from storage when the component mounts
    loadCookie();
  }, []);

  const saveCookie = async () => {
    try {
      await AsyncStorage.setItem(COOKIE_STORAGE_KEY, cookieInput);
      setStoredCookie(cookieInput);
      setCookieInput('');
      Alert.alert('Success', 'Cookie saved successfully!');
    } catch (e) {
      Alert.alert('Error', 'Failed to save the cookie.');
    }
  };

  const loadCookie = async () => {
    try {
      const cookie = await AsyncStorage.getItem(COOKIE_STORAGE_KEY);
      if (cookie !== null) {
        setStoredCookie(cookie);
      }
    } catch (e) {
      Alert.alert('Error', 'Failed to load the cookie.');
    }
  };

  const clearCookie = async () => {
    try {
      await AsyncStorage.removeItem(COOKIE_STORAGE_KEY);
      setStoredCookie('');
      Alert.alert('Success', 'Cookie cleared.');
    } catch (e) {
      Alert.alert('Error', 'Failed to clear the cookie.');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Settings & Login</Text>
      <Text style={styles.label}>Enter your YouTube Music cookie:</Text>
      <TextInput
        style={styles.input}
        placeholder="Paste your cookie string here"
        value={cookieInput}
        onChangeText={setCookieInput}
      />
      <Button title="Save Cookie" onPress={saveCookie} />

      <View style={styles.storedCookieContainer}>
        <Text style={styles.label}>Currently Stored Cookie:</Text>
        <Text style={styles.cookieText} numberOfLines={3}>
          {storedCookie ? storedCookie : 'None'}
        </Text>
        <View style={styles.buttonRow}>
          <Button title="Reload" onPress={loadCookie} />
          <Button title="Clear" onPress={clearCookie} color="red" />
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    marginBottom: 10,
    alignSelf: 'flex-start',
  },
  input: {
    width: '100%',
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 10,
    borderRadius: 5,
    marginBottom: 10,
  },
  storedCookieContainer: {
    marginTop: 30,
    width: '100%',
    padding: 10,
    borderWidth: 1,
    borderColor: '#eee',
    borderRadius: 5,
  },
  cookieText: {
    fontFamily: 'monospace',
    fontSize: 12,
    color: '#333',
    marginBottom: 10,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
});

export default SettingsScreen;
