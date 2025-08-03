import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  Text,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import YtMusic from '../api/YtMusic';

// This screen expects a `playlistId` in its route params
const PlaylistDetailScreen = ({route}: any) => {
  const {playlistId} = route.params;
  const [loading, setLoading] = useState(true);
  const [playlistData, setPlaylistData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchPlaylistData = async () => {
      if (!playlistId) return;
      try {
        const response = await YtMusic.browse(playlistId, undefined, true);
        setPlaylistData(response);
      } catch (e: any) {
        setError(e.message || 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchPlaylistData();
  }, [playlistId]);

  if (loading) {
    return (
      <SafeAreaView style={styles.centered}>
        <ActivityIndicator size="large" />
      </SafeAreaView>
    );
  }

  if (error) {
    return (
      <SafeAreaView style={styles.centered}>
        <Text style={styles.errorText}>Error: {error}</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView>
        <Text style={styles.title}>
          Playlist Details (ID: {playlistId})
        </Text>
        <Text style={styles.dataText}>
          {JSON.stringify(playlistData, null, 2)}
        </Text>
      </ScrollView>
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
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    margin: 10,
  },
  errorText: {
    color: 'red',
  },
  dataText: {
    fontFamily: 'monospace',
    fontSize: 10,
    padding: 10,
  },
});

export default PlaylistDetailScreen;
