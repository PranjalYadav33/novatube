import React, {useState} from 'react';
import {
  View,
  TextInput,
  Button,
  StyleSheet,
  SafeAreaView,
  FlatList,
  ActivityIndicator,
  Text,
} from 'react-native';
import YtMusic from '../api/YtMusic';
import Shelf from '../components/Shelf';

const SearchScreen = () => {
  const [query, setQuery] = useState('');
  const [shelves, setShelves] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!query) return;
    setLoading(true);
    setShelves([]);
    try {
      const response = await YtMusic.search(query);
      // This is a simplified parser path.
      const parsedShelves =
        response.contents.tabbedSearchResultsRenderer.tabs[0].tabRenderer
          .content.sectionListRenderer.contents;
      setShelves(parsedShelves);
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.input}
          placeholder="Search for a song, artist, or album..."
          value={query}
          onChangeText={setQuery}
          onSubmitEditing={handleSearch}
        />
        <Button
          title={loading ? '...' : 'Search'}
          onPress={handleSearch}
          disabled={loading}
        />
      </View>
      {loading ? (
        <ActivityIndicator size="large" style={styles.centered} />
      ) : (
        <FlatList
          data={shelves}
          renderItem={({item}) => (
            <Shelf shelf={item.musicCardShelfRenderer || item.musicShelfRenderer} />
          )}
          keyExtractor={(item, index) =>
            item.musicCardShelfRenderer?.shelfId ||
            item.musicShelfRenderer?.shelfId ||
            `shelf-${index}`
          }
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  searchContainer: {
    flexDirection: 'row',
    padding: 10,
    alignItems: 'center',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ccc',
    padding: 10,
    borderRadius: 5,
    marginRight: 10,
  },
  centered: {
    flex: 1,
  },
});

export default SearchScreen;
