import React from 'react';
import {View, Text, Image, StyleSheet, TouchableOpacity} from 'react-native';
import {usePlayer} from '../context/PlayerContext';

const getThumbnailUrl = (thumbnails: any[]) => {
  if (!thumbnails || thumbnails.length === 0) {
    return null;
  }
  // Return the highest quality thumbnail
  return thumbnails[thumbnails.length - 1].url;
};

const Item = ({item}: {item: any}) => {
  const {playTrack} = usePlayer();
  const thumbnailUrl = getThumbnailUrl(item.thumbnails);

  return (
    <TouchableOpacity
      style={styles.itemContainer}
      onPress={() => playTrack(item)}>
      {thumbnailUrl ? (
        <Image source={{uri: thumbnailUrl}} style={styles.thumbnail} />
      ) : (
        <View style={styles.thumbnailPlaceholder} />
      )}
      <Text style={styles.itemTitle} numberOfLines={2}>
        {item.title || 'Untitled'}
      </Text>
      {item.artists && (
        <Text style={styles.itemSubtitle} numberOfLines={1}>
          {item.artists.map((artist: any) => artist.name).join(', ')}
        </Text>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  itemContainer: {
    width: 140,
    marginHorizontal: 5,
  },
  thumbnail: {
    width: 140,
    height: 140,
    borderRadius: 5,
  },
  thumbnailPlaceholder: {
    width: 140,
    height: 140,
    backgroundColor: '#ccc',
    borderRadius: 5,
  },
  itemTitle: {
    marginTop: 5,
    fontSize: 14,
    fontWeight: 'bold',
  },
  itemSubtitle: {
    fontSize: 12,
    color: '#888',
  },
});

export default Item;
