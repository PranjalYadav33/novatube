import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  StyleSheet,
  ActivityIndicator,
  FlatList,
  Text,
} from 'react-native';
import YtMusic from '../api/YtMusic';
import Shelf from '../components/Shelf';
import {parseBrowseResponse} from '../utils/parse'; // This function will need to be created

const HomeScreen = () => {
  const [loading, setLoading] = useState(true);
  const [shelves, setShelves] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchHomeData = async () => {
      try {
        const response = await YtMusic.browse('FEmusic_home');
        // This is a simplified parser path. A real one would be more robust.
        const parsedShelves =
          response.contents.singleColumnBrowseResultsRenderer.tabs[0]
            .tabRenderer.content.sectionListRenderer.contents;
        setShelves(parsedShelves);
      } catch (e: any) {
        setError(e.message || 'An unknown error occurred');
      } finally {
        setLoading(false);
      }
    };

    fetchHomeData();
  }, []);

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
      <FlatList
        data={shelves}
        renderItem={({item}) => <Shelf shelf={item.musicCarouselShelfRenderer} />}
        keyExtractor={(item, index) =>
          item.musicCarouselShelfRenderer?.shelfId || `shelf-${index}`
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
  },
  errorText: {
    color: 'red',
  },
});

export default HomeScreen;
