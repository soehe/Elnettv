package com.elnet.tv

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var statusView: TextView
    private lateinit var channelList: LinearLayout
    private var selectedChannel = 0

    private data class Channel(val title: String, val url: String)

    private val channels = listOf(
        Channel("Elnet TV Demo", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Channel("Elnet News", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
        Channel("Elnet Entertainment", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.player_view)
        statusView = findViewById(R.id.status_view)
        channelList = findViewById(R.id.channel_list)
        buildChannelMenu()
    }

    private fun buildChannelMenu() {
        channelList.removeAllViews()
        channels.forEachIndexed { index, channel ->
            val button = Button(this).apply {
                text = "${index + 1}. ${channel.title}"
                isFocusable = true
                isFocusableInTouchMode = true
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(18, 0, 8, 0)
                setOnClickListener { playChannel(index) }
                setOnFocusChangeListener { view, hasFocus ->
                    view.setBackgroundColor(if (hasFocus) Color.rgb(255, 106, 0) else Color.TRANSPARENT)
                }
            }
            channelList.addView(button, LinearLayout.LayoutParams(-1, 56).apply {
                bottomMargin = 6
            })
        }
        channelList.getChildAt(selectedChannel)?.requestFocus()
    }

    private fun playChannel(index: Int) {
        selectedChannel = index
        val channel = channels[index]
        statusView.visibility = View.VISIBLE
        statusView.text = "Soehe TV\nMemuat ${channel.title}..."
        player?.setMediaItem(
            MediaItem.Builder()
                .setUri(channel.url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        ) ?: createPlayer(channel)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun createPlayer(channel: Channel) {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) statusView.visibility = View.GONE
                }
                override fun onPlayerError(error: PlaybackException) {
                    statusView.visibility = View.VISIBLE
                    statusView.text = "Soehe TV\nGagal memutar siaran.\nCek koneksi atau URL channel."
                }
            })
            exoPlayer.setMediaItem(
                MediaItem.Builder().setUri(channel.url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (player == null) playChannel(selectedChannel)
    }

    override fun onStop() {
        playerView.player = null
        player?.release()
        player = null
        super.onStop()
    }
}
