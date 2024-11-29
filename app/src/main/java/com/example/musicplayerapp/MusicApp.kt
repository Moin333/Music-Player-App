package com.example.musicplayerapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val songs by viewModel.songs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val currentPlayingSongId by viewModel.currentPlayingSongId.collectAsState()
    val progressMap by viewModel.progressMap.collectAsState()


    Column(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)) {

        Spacer(modifier = Modifier.height(30.dp))

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search for songs") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp, bottom = 0.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                val token = SpotifyTokenHelper.getAccessToken()
                if (token != null) {
                    viewModel.searchSongs(searchQuery, token)
                }
            }
        },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        ) {
            Text("Search")
        }

        LazyColumn {
            items(songs) { song ->
                SongItem(
                    song = song,
                    currentPlayingSongId = currentPlayingSongId,
                    progressMap = progressMap,
                    onPlayPauseClick = {
                        if (currentPlayingSongId == song.id) {
                            println("Pausing song: ${song.title}")
                            viewModel.pauseSong()
                            println("Paused song: ${song.title}")
                        }
                        else {
                            println("Playing song: ${song.title}")
                            viewModel.playSong(song)
                            println("Playing song: ${song.title}")
                        }
                    },
                    onSaveClick = {
                        println("Saving song: ${song.title}")
                        viewModel.saveSong(song)
                        println("Saved song: ${song.title}")
                    }
                )
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    currentPlayingSongId: String?,
    progressMap: Map<String, Float>,
    onPlayPauseClick: () -> Unit,
    onSaveClick: () -> Unit
) {

    val isPlaying = currentPlayingSongId == song.id
    val progress = progressMap[song.id] ?: 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
            .clickable { onSaveClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Slider(
                    value = progress,
                    onValueChange = {},
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                )

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White
                    )
                }

            }
        }
    }
}
