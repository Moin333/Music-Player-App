package com.example.musicplayerapp

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.RealmResults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import org.json.JSONObject

class MusicViewModel(private val realm: Realm) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    private val _currentPlayingSongId = MutableStateFlow<String?>(null) // Track the current playing song's ID
    val currentPlayingSongId = _currentPlayingSongId.asStateFlow()

    private val _progressMap = MutableStateFlow<Map<String, Float>>(emptyMap()) // Map to track progress of each song
    val progressMap = _progressMap.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongUrl: String? = null

    init {
        loadSavedSongs()
    }

    /**
     * Loads all saved songs from the Realm database.
     */
    private fun loadSavedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val results: RealmResults<Song> = realm.query<Song>().find()
            _songs.value = results
        }
    }



        /**
     * Searches for songs using the Spotify API and updates the state with results.
     */
    fun searchSongs(query: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://api.spotify.com/v1/search?q=$query&type=track&limit=10"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body!!.string())
                        val tracks = json.getJSONObject("tracks").getJSONArray("items")

                        val songs = mutableListOf<Song>()
                        for (i in 0 until tracks.length()) {
                            val track = tracks.getJSONObject(i)
                            val song = Song().apply {
                                id = track.getString("id")
                                title = track.getString("name")
                                artist = track.getJSONArray("artists")
                                    .getJSONObject(0).getString("name")
                                urL = track.optString("preview_url", "")
                            }
                            if (song.urL.isEmpty()) {
                                println("No preview available for ${song.title}")
                            }
                            songs.add(song)
                        }
                        _songs.value = songs
                    } else {
                        println("Failed to fetch songs: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Saves a song to the Realm database.
     */
    fun saveSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            realm.write {
                try {
                    val existingSong = query<Song>("id == $0", song.id).first().find()
                    if (existingSong == null) {
                        println("Saving new song: ${song.title}")
                        copyToRealm(song)
                        println("Saved new song: ${song.title}")
                    } else {
                        findLatest(existingSong)?.apply {
                            title = song.title
                            artist = song.artist
                            urL = song.urL
                        }
                    }
                } catch (e: Exception) {
                    println("Error saving song: ${e.message}")
                }
            }
        }
    }


    private fun monitorProgress(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            while (_currentPlayingSongId.value == songId) {
                val duration = mediaPlayer?.duration ?: 1  // Prevent divide-by-zero
                val position = mediaPlayer?.currentPosition ?: 0
                _progressMap.value = _progressMap.value.toMutableMap().apply {
                    this[songId] = position / duration.toFloat()
                }
                delay(500)  // Update every 500ms for smoother progress
            }
        }
    }

    fun playSong(song: Song) {
        if (song.urL.isNullOrEmpty()) {
            println("Error: Song URL is empty or null.")
            return
        }

        println("Playing URL: ${song.urL}")
        if (currentSongUrl == song.urL && mediaPlayer?.isPlaying == true) return

        stopSong()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.urL)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    _currentPlayingSongId.value = song.id
                    monitorProgress(song.id)
                }
            } catch (e: IOException) {
                println("Error setting data source: ${e.message}")
                stopSong()
            }
            setOnCompletionListener {
                _currentPlayingSongId.value = null
                _progressMap.value = _progressMap.value.toMutableMap().apply { this[song.id] = 1f }
            }
            setOnErrorListener { _, what, extra ->
                println("MediaPlayer error: $what, extra: $extra")
                stopSong()
                true
            }
        }
        currentSongUrl = song.urL
    }



    fun pauseSong() {
        println("Pausing song from ViewModel: ${mediaPlayer?.isPlaying}")
        mediaPlayer?.pause()
        _currentPlayingSongId.value = null
    }

    fun stopSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentSongUrl = null
        _currentPlayingSongId.value = null
    }



    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}
