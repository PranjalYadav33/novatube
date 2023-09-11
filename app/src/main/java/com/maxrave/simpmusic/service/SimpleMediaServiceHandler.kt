package com.maxrave.simpmusic.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.MEDIA_CUSTOM_COMMAND
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.data.repository.MainRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@OptIn(DelicateCoroutinesApi::class)
@UnstableApi
class SimpleMediaServiceHandler @Inject constructor(
    private val player: ExoPlayer,
    private val mediaSession: MediaSession,
    mediaSessionCallback: SimpleMediaSessionCallback,
    private val dataStoreManager: DataStoreManager,
    private val mainRepository: MainRepository,
    @ApplicationContext private val context: Context
) : Player.Listener {

    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var volumeNormalizationJob: Job? = null

    private var sleepTimerJob: Job? = null

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    val simpleMediaState = _simpleMediaState.asStateFlow()

    private val _changeTrack = MutableStateFlow<Boolean>(false)
    val changeTrack = _changeTrack.asStateFlow()

    var nowPlaying = MutableStateFlow(player.currentMediaItem)

    private val _nextTrackAvailable = MutableStateFlow<Boolean>(false)
    val nextTrackAvailable = _nextTrackAvailable.asStateFlow()

    private val _previousTrackAvailable = MutableStateFlow<Boolean>(false)
    val previousTrackAvailable = _previousTrackAvailable.asStateFlow()

    private val _shuffle = MutableStateFlow<Boolean>(false)
    val shuffle = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeat = _repeat.asStateFlow()

    private val _sleepMinutes = MutableStateFlow<Int>(0)
    val sleepMinutes = _sleepMinutes.asSharedFlow()

    private val _sleepDone = MutableStateFlow<Boolean>(false)
    val sleepDone = _sleepDone.asSharedFlow()

    private val _liked = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    private var updateNotificationJob: Job? = null

    private var skipSilent = false

    private var normalizeVolume = false

    private var job: Job? = null

    init {
        player.addListener(this)
        job = Job()
        skipSilent = runBlocking { dataStoreManager.skipSilent.first() == DataStoreManager.TRUE }
        normalizeVolume = runBlocking { dataStoreManager.normalizeVolume.first() == DataStoreManager.TRUE }
        if (runBlocking{ dataStoreManager.saveStateOfPlayback.first() } == DataStoreManager.TRUE ) {
            Log.d("CHECK INIT", "TRUE")
            val shuffleKey = runBlocking { dataStoreManager.shuffleKey.first() }
            val repeatKey = runBlocking { dataStoreManager.repeatKey.first() }
            Log.d("CHECK INIT", "Shuffle: $shuffleKey")
            Log.d("CHECK INIT", "Repeat: $repeatKey")
            player.shuffleModeEnabled = shuffleKey == DataStoreManager.TRUE
            player.repeatMode = when (repeatKey) {
                DataStoreManager.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                DataStoreManager.REPEAT_ALL -> Player.REPEAT_MODE_ALL
                DataStoreManager.REPEAT_MODE_OFF -> Player.REPEAT_MODE_OFF
                else -> {Player.REPEAT_MODE_OFF}
            }
        }
        _shuffle.value = player.shuffleModeEnabled
        _repeat.value = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatState.One
            Player.REPEAT_MODE_ALL -> RepeatState.All
            Player.REPEAT_MODE_OFF -> RepeatState.None
            else -> {RepeatState.None}
        }
        nowPlaying.value = player.currentMediaItem
        mediaSessionCallback.apply {
            toggleLike = ::toggleLike
        }
    }
    private fun toggleLike() {
        updateNotificationJob?.cancel()
        updateNotificationJob = GlobalScope.launch(Dispatchers.Main) {
            mainRepository.updateLikeStatus(player.currentMediaItem?.mediaId ?: "", if (!(_liked.value)) 1 else 0)
        }
        _liked.value = !(_liked.value)
        updateNotification()
    }

    fun like(liked: Boolean) {
        _liked.value = liked
        updateNotification()
    }
    //Set sleep timer
    fun sleepStart(minutes: Int) {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        sleepTimerJob = GlobalScope.launch(Dispatchers.Main) {
            _sleepMinutes.value = minutes
            var count = minutes
            while (count > 0) {
                delay(60 * 1000L)
                count--
                _sleepMinutes.value = count
            }
            player.pause()
            _sleepMinutes.value = 0
            _sleepDone.value = true
        }
    }
    fun sleepStop() {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        _sleepMinutes.value = 0
    }

    private fun updateNextPreviousTrackAvailability() {
        _nextTrackAvailable.value = player.hasNextMediaItem()
        _previousTrackAvailable.value = player.hasPreviousMediaItem()
    }

    fun getMediaItemWithIndex(index: Int): MediaItem {
        return player.getMediaItemAt(index)
    }

    fun removeMediaItem(position: Int) {
        player.removeMediaItem(position)
    }

    fun addMediaItem(mediaItem: MediaItem) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun addMediaItemNotSet(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1){
            player.prepare()
            player.playWhenReady = true
        }
        updateNextPreviousTrackAvailability()
    }

    fun clearMediaItems() {
        player.clearMediaItems()
    }

    fun addMediaItemList(mediaItemList: List<MediaItem>) {
        for (mediaItem in mediaItemList) {
            addMediaItemNotSet(mediaItem)
        }
        Log.d("Media Item List", "addMediaItemList: ${player.mediaItemCount}")
    }

    fun playMediaItemInMediaSource(index: Int){
        player.seekTo(index, 0)
        player.prepare()
        player.playWhenReady = true
    }

    fun moveMediaItem(fromIndex: Int, newIndex: Int) {
        player.moveMediaItem(fromIndex, newIndex)
    }

    suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = true)
                    startProgressUpdate()
                }
            }
            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.Stop -> {stopProgressUpdate()
            player.stop()}
            is PlayerEvent.UpdateProgress -> player.seekTo((player.duration * playerEvent.newProgress/100).toLong())
            PlayerEvent.Shuffle -> {
                if (player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = false
                    _shuffle.value = false
                } else {
                    player.shuffleModeEnabled = true
                    _shuffle.value = true
                }
            }
            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        _repeat.value = RepeatState.One
                    }
                    ExoPlayer.REPEAT_MODE_ONE -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        _repeat.value = RepeatState.All
                    }
                    ExoPlayer.REPEAT_MODE_ALL -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                        _repeat.value = RepeatState.None
                    }
                    else -> {
                        when(_repeat.value) {
                            RepeatState.None -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                                _repeat.value = RepeatState.One
                            }
                            RepeatState.One -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                _repeat.value = RepeatState.All
                            }
                            RepeatState.All -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                                _repeat.value = RepeatState.None
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val shouldBePlaying = !(player.playbackState == Player.STATE_ENDED || !player.playWhenReady)
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            if (shouldBePlaying) {
                sendOpenEqualizerIntent()
            } else {
                sendCloseEqualizerIntent()
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        Log.d("Tracks", "onTracksChanged: ${tracks.groups.size}")
        super.onTracksChanged(tracks)
    }

    override fun onPlayerError(error: PlaybackException) {
        when(error.errorCode) {
            PlaybackException.ERROR_CODE_TIMEOUT -> {
                Log.e("Player Error", "onPlayerError: ${error.message}")
                Toast.makeText(context,
                    context.getString(R.string.time_out_check_internet_connection_or_change_piped_instance_in_settings), Toast.LENGTH_LONG).show()
                player.pause()
            }
            else -> {
                Log.e("Player Error", "onPlayerError: ${error.message}")
                Toast.makeText(context,
                    context.getString(R.string.time_out_check_internet_connection_or_change_piped_instance_in_settings), Toast.LENGTH_LONG).show()
                player.pause()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Log.w("Smooth Switching Transition", "Current Position: ${player.currentPosition}")
        mayBeNormalizeVolume()
        Log.w("REASON", "onMediaItemTransition: $reason")
        Log.d("Media Item Transition", "Media Item: ${mediaItem?.mediaMetadata?.title}")
        nowPlaying.value = mediaItem
        updateNextPreviousTrackAvailability()
        updateNotification()
    }


    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            ExoPlayer.STATE_IDLE -> _simpleMediaState.value = SimpleMediaState.Initial
            ExoPlayer.STATE_ENDED -> _simpleMediaState.value = SimpleMediaState.Ended
            ExoPlayer.STATE_BUFFERING -> _simpleMediaState.value =
                SimpleMediaState.Buffering(player.currentPosition)
            ExoPlayer.STATE_READY -> _simpleMediaState.value =
                SimpleMediaState.Ready(player.duration)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        when (shuffleModeEnabled){
            true -> {
                _shuffle.value = true
            }
            false ->{
                _shuffle.value = false
            }
        }
        updateNextPreviousTrackAvailability()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        when (repeatMode){
            ExoPlayer.REPEAT_MODE_OFF -> _repeat.value = RepeatState.None
            ExoPlayer.REPEAT_MODE_ONE -> _repeat.value = RepeatState.One
            ExoPlayer.REPEAT_MODE_ALL -> _repeat.value = RepeatState.All
        }
        updateNextPreviousTrackAvailability()
        updateNotification()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = isPlaying)
        if (isPlaying) {
            GlobalScope.launch(Dispatchers.Main) {
                startProgressUpdate()
            }
        } else {
            stopProgressUpdate()
        }
        updateNextPreviousTrackAvailability()
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(100)
            _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
        }
    }

    private suspend fun startBufferedUpdate() = job.run {
        while (true){
            delay(500)
            _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
        }
    }
    fun currentIndex(): Int {
        return player.currentMediaItemIndex
    }

    fun mediaListSize(): Int {
        return player.mediaItemCount
    }

    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = false)
    }
    private fun stopBufferedUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)
        _simpleMediaState.value = SimpleMediaState.Loading(player.bufferedPercentage, player.duration)
        if (isLoading) {
            GlobalScope.launch(Dispatchers.Main) {
                startBufferedUpdate()
            }
        } else {
            stopBufferedUpdate()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun mayBeNormalizeVolume() {
        if (!normalizeVolume) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        if (loudnessEnhancer == null) {
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = GlobalScope.launch(Dispatchers.Main) {
                mainRepository.getFormat(songId).cancellable().collectLatest { format ->
                    if (format != null){
                        try {
                            loudnessEnhancer?.setTargetGain(-((format.loudnessDb ?: 0f) * 100).toInt() + 500)
                            Log.w("Loudness", "mayBeNormalizeVolume: ${loudnessEnhancer?.targetGain}")
                            loudnessEnhancer?.enabled = true
                        } catch (_: Exception) { }
                    }
                }
            }
        }
    }
    private fun maybeSkipSilent() {
        player.skipSilenceEnabled = skipSilent
    }
    fun mayBeSaveRecentSong() {
        runBlocking {
            if (dataStoreManager.saveRecentSongAndQueue.first() == DataStoreManager.TRUE) {
                dataStoreManager.saveRecentSong(player.currentMediaItem?.mediaId ?: "", player.currentPosition)
                Log.d("Check saved", player.currentMediaItem?.mediaMetadata?.title.toString())
                val temp: ArrayList<Track> = ArrayList()
                temp.clear()
                Queue.getNowPlaying()?.let { nowPlaying ->
                    if (nowPlaying.videoId != player.currentMediaItem?.mediaId) {
                        temp += nowPlaying
                    }
                }
                temp += Queue.getQueue()
                Log.d("Check queue", Queue.getQueue().toString())
                temp.find { it.videoId == player.currentMediaItem?.mediaId }?.let { track ->
                    temp.remove(track)
                }
                Log.w("Check recover queue", temp.toString())
                mainRepository.recoverQueue(temp)
                dataStoreManager.putString(DataStoreManager.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE, DataStoreManager.FALSE)
            }
        }
    }
    fun mayBeSavePlaybackState() {
        if (runBlocking{ dataStoreManager.saveStateOfPlayback.first() } == DataStoreManager.TRUE ) {
            runBlocking { dataStoreManager.recoverShuffleAndRepeatKey(player.shuffleModeEnabled, player.repeatMode) }
        }
    }
    fun editSkipSilent(skip: Boolean) {
        skipSilent = skip
        maybeSkipSilent()
    }
    fun editNormalizeVolume(normalize: Boolean) {
        normalizeVolume = normalize
    }

    fun seekTo(position: String)  {
        player.seekTo(position.toLong())
        player.playWhenReady = false
        Log.d("Check seek", "seekTo: ${player.duration}")
    }
    fun skipSegment(position: Long) {
        if (position in 0..player.duration) {
            player.seekTo(position)
        }
        else if (position > player.duration) {
            player.seekToNext()
        }
    }
    private fun sendOpenEqualizerIntent() {
        context.sendBroadcast (
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }
    private fun sendCloseEqualizerIntent() {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }
    fun release() {
        sendCloseEqualizerIntent()
        player.removeListener(this)
        job?.cancel()
        sleepTimerJob?.cancel()
        volumeNormalizationJob?.cancel()
        updateNotificationJob?.cancel()
        GlobalScope.cancel()
    }

    private fun updateNotification() {
        updateNotificationJob?.cancel()
        updateNotificationJob = GlobalScope.launch(Dispatchers.Main) {
            val liked = mainRepository.getSongById(player.currentMediaItem?.mediaId ?: "").first()?.liked
            if (liked != null) {
                _liked.value = liked
            }
            mediaSession.setCustomLayout(
                listOf(
                    CommandButton.Builder()
                        .setDisplayName(if (liked == true) context.getString(R.string.liked) else context.getString(R.string.like))
                        .setIconResId(if (liked == true) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24)
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                        .build(),
                    CommandButton.Builder()
                        .setDisplayName(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_one_description)
                                Player.REPEAT_MODE_ALL -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_all_description)
                                else -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_off_description)
                            }
                        )
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                        .setIconResId(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.baseline_repeat_one_24
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.baseline_repeat_24_enable
                            }
                        )
                        .build()
                )
            )
        }
    }
}

sealed class RepeatState {
    object None : RepeatState()
    object All : RepeatState()
    object One : RepeatState()
}

sealed class PlayerEvent {
    object PlayPause : PlayerEvent()
    object Backward : PlayerEvent()
    object Forward : PlayerEvent()
    object Stop : PlayerEvent()
    object Next : PlayerEvent()
    object Previous : PlayerEvent()
    object Shuffle : PlayerEvent()
    object Repeat : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class SimpleMediaState {
    object Initial : SimpleMediaState()
    object Ended : SimpleMediaState()
    data class Ready(val duration: Long) : SimpleMediaState()
    data class Loading(val bufferedPercentage: Int, val duration: Long): SimpleMediaState()
    data class Progress(val progress: Long) : SimpleMediaState()
    data class Buffering(val position: Long) : SimpleMediaState()
    data class Playing(val isPlaying: Boolean) : SimpleMediaState()
}
