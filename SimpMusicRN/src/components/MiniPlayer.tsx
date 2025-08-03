import React from 'react';
import {View, Text, StyleSheet, TouchableOpacity} from 'react-native';
import {usePlayer} from '../context/PlayerContext';

const MiniPlayer = () => {
  const {currentTrack, isPlaying, pauseTrack, playTrack} = usePlayer();

  if (!currentTrack) {
    return null; // Don't render anything if no track is loaded
  }

  const handleTogglePlay = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      // In a real app, you might want to call a resume function
      // For now, we'll just log it.
      console.log('Resuming track (not implemented)');
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.trackInfo}>
        <Text style={styles.title} numberOfLines={1}>
          {currentTrack.title}
        </Text>
        <Text style={styles.artist} numberOfLines={1}>
          {currentTrack.artists?.map((a: any) => a.name).join(', ') || ''}
        </Text>
      </View>
      <TouchableOpacity style={styles.button} onPress={handleTogglePlay}>
        <Text style={styles.buttonText}>{isPlaying ? 'Pause' : 'Play'}</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 50, // This will need to be adjusted based on the tab bar height
    left: 0,
    right: 0,
    height: 60,
    backgroundColor: '#f8f8f8',
    borderTopWidth: 1,
    borderColor: '#e7e7e7',
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    justifyContent: 'space-between',
  },
  trackInfo: {
    flex: 1,
    marginRight: 10,
  },
  title: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  artist: {
    fontSize: 14,
    color: '#888',
  },
  button: {
    padding: 10,
  },
  buttonText: {
    fontSize: 16,
    color: '#007aff',
  },
});

export default MiniPlayer;
