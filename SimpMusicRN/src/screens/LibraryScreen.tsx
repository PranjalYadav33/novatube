import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  Button,
  TouchableOpacity,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import YtMusic from '../api/YtMusic';
import {useNavigation} from '@react-navigation/native';
import {StackNavigationProp} from '@react-navigation/stack';
import {RootStackParamList} from '../../App';

const COOKIE_STORAGE_KEY = '@user_cookie';

type LibraryScreenNavigationProp = StackNavigationProp<
  RootStackParamList,
  'PlaylistDetail'
>;

const LibraryScreen = () => {
  const navigation = useNavigation<LibraryScreenNavigationProp>();
  const [loading, setLoading] = useState(true);
  const [playlists, setPlaylists] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const fetchLibraryData = async () => {
    setLoading(true);
    setError(null);
    try {
      const cookie = await AsyncStorage.getItem(COOKIE_STORAGE_KEY);
      if (cookie) {
        setIsLoggedIn(true);
        YtMusic.setCookie(cookie); // Configure the API client
        const response = await YtMusic.browse(
          'FEmusic_liked_playlists',
          undefined,
          true,
        );
        // Simplified parser path
        const parsedPlaylists =
          response.contents.singleColumnBrowseResultsRenderer.tabs[0]
            .tabRenderer.content.sectionListRenderer.contents[0]
            .gridRenderer.items;
        setPlaylists(parsedPlaylists);
      } else {
        setIsLoggedIn(false);
        setError('You are not logged in. Please add your cookie in Settings.');
      }
    } catch (e: any) {
      setError(e.message || 'An unknown error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLibraryData();
  }, []);

  if (loading) {
    return (
      <SafeAreaView style={styles.centered}>
        <ActivityIndicator size="large" />
      </SafeAreaView>
    );
  }

  if (!isLoggedIn || error) {
    return (
      <SafeAreaView style={styles.centered}>
        <Text style={styles.errorText}>{error}</Text>
        <Button title="Retry" onPress={fetchLibraryData} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Your Library</Text>
      <FlatList
        data={playlists}
        renderItem={({item}) => {
          const playlist = item.musicTwoRowItemRenderer;
          const browseId = playlist.navigationEndpoint.browseEndpoint.browseId;
          return (
            <TouchableOpacity
              onPress={() =>
                navigation.navigate('PlaylistDetail', {playlistId: browseId})
              }>
              <Text style={styles.playlistItem}>
                {playlist.title.runs[0].text}
              </Text>
            </TouchableOpacity>
          );
        }}
        keyExtractor={item =>
          item.musicTwoRowItemRenderer.navigationEndpoint.browseEndpoint
            .browseId
        }
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    margin: 10,
  },
  errorText: {
    color: 'red',
    textAlign: 'center',
    marginBottom: 10,
  },
  playlistItem: {
    padding: 15,
    fontSize: 18,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
});

export default LibraryScreen;
