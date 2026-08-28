package org.sesacteamproject.passmate.ui.play

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

private class ExoVoiceHintPlayerController(
    private val player: ExoPlayer
) : VoiceHintPlayerController {

    private var state = VoiceHintPlaybackState.IDLE

    private val listener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                state = VoiceHintPlaybackState.ENDED
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                state = VoiceHintPlaybackState.PLAYING
            } else if (state == VoiceHintPlaybackState.PLAYING) {
                state = VoiceHintPlaybackState.PAUSED
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            state = VoiceHintPlaybackState.FAILED
        }
    }

    override fun play(url: String) {
        state = VoiceHintPlaybackState.PLAYING
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        if (state == VoiceHintPlaybackState.FAILED || state == VoiceHintPlaybackState.ENDED) {
            player.seekTo(0L)
            player.prepare()
        }
        player.play()
    }

    override fun stop() {
        player.stop()
        state = VoiceHintPlaybackState.IDLE
    }

    override fun playbackState(): VoiceHintPlaybackState {
        return state
    }

    override fun positionMillis(): Long {
        return player.currentPosition.coerceAtLeast(0L)
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }

    init {
        player.addListener(listener)
    }
}

@Composable
actual fun rememberVoiceHintPlayer(): VoiceHintPlayerController? {
    val context = LocalContext.current
    val controller = remember { ExoVoiceHintPlayerController(ExoPlayer.Builder(context).build()) }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
