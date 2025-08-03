import React from 'react';
import {View, Text, FlatList, StyleSheet} from 'react-native';
import Item from './Item'; // Import the new Item component

const Shelf = ({shelf}: {shelf: any}) => {
  if (!shelf.contents || shelf.contents.length === 0) {
    return null;
  }

  // A placeholder for the press handler
  const handleItemPress = (item: any) => {
    console.log('Pressed item:', item.title);
  };

  return (
    <View style={styles.shelfContainer}>
      <Text style={styles.shelfTitle}>{shelf.title}</Text>
      <FlatList
        horizontal
        data={shelf.contents}
        renderItem={({item}) => (
          <Item item={item} onPress={() => handleItemPress(item)} />
        )}
        keyExtractor={(item, index) => item.videoId || `shelf-item-${index}`}
        showsHorizontalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  shelfContainer: {
    paddingVertical: 10,
  },
  shelfTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginLeft: 10,
    marginBottom: 10,
  },
});

export default Shelf;
