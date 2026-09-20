package com.elnet.tv

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var playlistUrl: EditText
    private lateinit var searchInput: EditText
    private val allChannels = mutableListOf<Channel>()
    private val preferences by lazy { getSharedPreferences("playlist", Context.MODE_PRIVATE) }
    private data class Channel(val title: String, val url: String, val group: String)

    private val openPlaylist = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { parsePlaylist(it.readText()) }
        } catch (_: Exception) {
            Toast.makeText(this, "Gagal membaca file M3U", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.player_view)
        statusView = findViewById(R.id.status_view)
        channelList = findViewById(R.id.channel_list)
        playlistUrl = findViewById(R.id.playlist_url)
        searchInput = findViewById(R.id.search_input)
        playlistUrl.setText(preferences.getString("url", "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8"))
        findViewById<Button>(R.id.load_url_button).setOnClickListener { loadFromUrl() }
        findViewById<Button>(R.id.open_file_button).setOnClickListener { openPlaylist.launch(arrayOf("text/*", "application/octet-stream")) }
        findViewById<Button>(R.id.clear_button).setOnClickListener {
            allChannels.clear(); channelList.removeAllViews(); player?.stop()
            Toast.makeText(this, "Playlist dihapus", Toast.LENGTH_SHORT).show()
        }
        searchInput.addTextChangedListener(SimpleTextWatcher { buildChannelMenu(it) })
        preferences.getString("content", null)?.takeIf { it.isNotBlank() }?.let { parsePlaylist(it) }
    }

    private fun loadFromUrl() {
        val url = playlistUrl.text.toString().trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Masukkan URL playlist yang valid", Toast.LENGTH_SHORT).show(); return
        }
        statusView.visibility = View.VISIBLE; statusView.text = "Memuat playlist..."
        Thread {
            try {
                val text = java.net.URL(url).openStream().bufferedReader().use { it.readText() }
                runOnUiThread { preferences.edit().putString("url", url).putString("content", text).apply(); parsePlaylist(text) }
            } catch (_: Exception) {
                runOnUiThread { statusView.text = "Gagal memuat playlist"; Toast.makeText(this, "Cek URL atau koneksi internet", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun parsePlaylist(text: String) {
        allChannels.clear(); var title = "Channel"; var group = "Lainnya"
        val lines = text.lines()
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF", true)) {
                title = line.substringAfterLast(",", "Channel").trim().ifBlank { "Channel" }
                group = Regex("group-title=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)?.ifBlank { "Lainnya" } ?: "Lainnya"
                val url = lines.drop(i + 1).firstOrNull { it.trim().isNotEmpty() && !it.trim().startsWith("#") }?.trim()
                if (!url.isNullOrBlank() && (url.startsWith("http://") || url.startsWith("https://"))) allChannels.add(Channel(title, url, group))
            }
        }
        buildChannelMenu("")
        statusView.visibility = if (allChannels.isEmpty()) View.VISIBLE else View.GONE
        if (allChannels.isEmpty()) statusView.text = "Playlist tidak berisi channel yang valid"
    }

    private fun buildChannelMenu(filter: String) {
        channelList.removeAllViews(); val query = filter.trim().lowercase()
        allChannels.filter { query.isBlank() || it.title.lowercase().contains(query) || it.group.lowercase().contains(query) }.forEachIndexed { index, channel ->
            val button = Button(this).apply {
                text = "${index + 1}. ${channel.title}"; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE); textSize = 14f; isFocusable = true
                setOnClickListener { playChannel(allChannels.indexOf(channel)) }
                setOnFocusChangeListener { view, focused -> view.setBackgroundColor(if (focused) Color.rgb(255, 193, 7) else Color.TRANSPARENT) }
            }
            channelList.addView(button, LinearLayout.LayoutParams(-1, 56).apply { bottomMargin = 6 })
        }
        channelList.getChildAt(0)?.requestFocus()
    }

    private fun playChannel(index: Int) {
        if (index !in allChannels.indices) return
        val channel = allChannels[index]; statusView.visibility = View.VISIBLE; statusView.text = "Memuat ${channel.title}..."
        if (player == null) createPlayer() else player?.clearMediaItems()
        player?.setMediaItem(MediaItem.Builder().setUri(channel.url).setMimeType(MimeTypes.APPLICATION_M3U8).build())
        player?.prepare(); player?.playWhenReady = true
    }

    private fun createPlayer() {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) statusView.visibility = View.GONE }
                override fun onPlayerError(error: PlaybackException) { statusView.visibility = View.VISIBLE; statusView.text = "Gagal memutar siaran"; Toast.makeText(this@MainActivity, "Gagal memutar siaran", Toast.LENGTH_SHORT).show() }
            })
        }
    }

    override fun onStop() { playerView.player = null; player?.release(); player = null; super.onStop() }

    private class SimpleTextWatcher(private val changed: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}