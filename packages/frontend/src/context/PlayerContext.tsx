import React, {createContext, useContext, useState, ReactNode, useRef} from 'react';
import {getPlayer} from '../api/client';

interface PlayerContextType {
  currentTrack: any | null;
  isPlaying: boolean;
  playTrack: (track: any) => void;
  pauseTrack: () => void;
}

const PlayerContext = createContext<PlayerContextType | undefined>(undefined);

export const PlayerProvider = ({children}: {children: ReactNode}) => {
  const [currentTrack, setCurrentTrack] = useState<any | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const audioRef = useRef<HTMLAudioElement>(null);

  const playTrack = async (track: any) => {
    try {
      const response = await getPlayer(track.videoId);
      const audioFormats = response.data.streamingData.adaptiveFormats;
      const audioUrl = audioFormats.find((f: any) =>
        f.mimeType.startsWith('audio/mp4'),
      )?.url;

      if (!audioUrl || !audioRef.current) {
        throw new Error('No audio stream found or audio element not ready.');
      }

      audioRef.current.src = audioUrl;
      audioRef.current.play();
      setCurrentTrack(track);
      setIsPlaying(true);
    } catch (error) {
      console.error('Failed to play track:', error);
    }
  };

  const pauseTrack = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      setIsPlaying(false);
    }
  };

  const value = {
    currentTrack,
    isPlaying,
    playTrack,
    pauseTrack,
  };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <audio
        ref={audioRef}
        onEnded={() => setIsPlaying(false)}
        style={{display: 'none'}}
      />
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  const context = useContext(PlayerContext);
  if (context === undefined) {
    throw new Error('usePlayer must be used within a PlayerProvider');
  }
  return context;
};
