import TrackPlayer, {Event} from 'react-native-track-player';

// This is the main function for the playback service
module.exports = async function () {
  TrackPlayer.addEventListener(Event.RemotePlay, () => TrackPlayer.play());
  TrackPlayer.addEventListener(Event.RemotePause, () => TrackPlayer.pause());
  TrackPlayer.addEventListener(Event.RemoteNext, () => TrackPlayer.skipToNext());
  TrackPlayer.addEventListener(Event.RemotePrevious, () =>
    TrackPlayer.skipToPrevious(),
  );

  // We can add more event listeners here, like for when the track ends.
  TrackPlayer.addEventListener(Event.PlaybackTrackChanged, async data => {
    console.log('Track changed to:', data);
  });
};
