import React from 'react';
import {usePlayer} from '../context/PlayerContext';
import './PlayerBar.css';

const PlayerBar = () => {
  const {currentTrack, isPlaying, pauseTrack, playTrack} = usePlayer();

  if (!currentTrack) {
    return null;
  }

  // The playTrack function in the context handles playing a new track.
  // For a simple play/pause button, we might need a resume function,
  // but for now, we'll just use pause. A real play button would resume the current track.
  const handleTogglePlay = () => {
    if (isPlaying) {
      pauseTrack();
    } else {
      // This is a simplification. A real implementation would resume.
      console.log('Resume functionality not implemented, use pause only for now.');
    }
  };

  return (
    <div className="player-bar">
      <div className="player-track-info">
        <div className="player-title">{currentTrack.title}</div>
        <div className="player-artist">
          {currentTrack.artists?.map((a: any) => a.name).join(', ')}
        </div>
      </div>
      <button onClick={handleTogglePlay} className="player-button">
        {isPlaying ? 'Pause' : 'Play'}
      </button>
    </div>
  );
};

export default PlayerBar;
