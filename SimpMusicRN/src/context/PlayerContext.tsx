import React, {createContext, useContext, useState, ReactNode} from 'react';
import TrackPlayer, {
  State as PlaybackState,
  usePlaybackState,
} from 'react-native-track-player';
import YtMusic from '../api/YtMusic';

interface PlayerContextType {
  currentTrack: any | null;
  isPlaying: boolean;
  playTrack: (track: any) => void;
  pauseTrack: () => void;
}

const PlayerContext = createContext<PlayerContextType | undefined>(undefined);

export const PlayerProvider = ({children}: {children: ReactNode}) => {
  const [currentTrack, setCurrentTrack] = useState<any | null>(null);
  const playbackState = usePlaybackState();
  const isPlaying = playbackState === PlaybackState.Playing;

  const playTrack = async (track: any) => {
    try {
      const playerResponse = await YtMusic.player(track.videoId);
      const audioFormats = playerResponse.streamingData.adaptiveFormats;
      const audioUrl = audioFormats.find((f: any) =>
        f.mimeType.startsWith('audio/mp4'),
      )?.url;

      if (!audioUrl) {
        throw new Error('No audio stream found for this track.');
      }

      await TrackPlayer.reset();
      await TrackPlayer.add({
        id: track.videoId,
        url: audioUrl,
        title: track.title,
        artist: track.artists?.map((a: any) => a.name).join(', '),
        artwork: track.thumbnails?.[0]?.url,
      });

      await TrackPlayer.play();
      setCurrentTrack(track);
    } catch (error) {
      console.error('Failed to play track:', error);
    }
  };

  const pauseTrack = async () => {
    await TrackPlayer.pause();
  };

  const value = {
    currentTrack,
    isPlaying,
    playTrack,
    pauseTrack,
  };

  return (
    <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  const context = useContext(PlayerContext);
  if (context === undefined) {
    throw new Error('usePlayer must be used within a PlayerProvider');
  }
  return context;
};
