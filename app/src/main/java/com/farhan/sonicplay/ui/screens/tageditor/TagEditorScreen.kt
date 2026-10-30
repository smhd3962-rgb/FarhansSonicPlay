package com.farhan.sonicplay.ui.screens.tageditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farhan.sonicplay.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorScreen(
    song: Song?,
    onSaveTags: (Long, String, String, String, Int, Int, String) -> Unit,
    onBack: () -> Unit
) {
    if (song == null) return

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var yearText by remember { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var trackText by remember { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var genre by remember { mutableStateOf(song.genre) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Metadata / Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val year = yearText.toIntOrNull() ?: 0
                        val track = trackText.toIntOrNull() ?: 0
                        onSaveTags(song.id, title.trim(), artist.trim(), album.trim(), year, track, genre.trim())
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artist") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text("Album") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it },
                    label = { Text("Year") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = trackText,
                    onValueChange = { trackText = it },
                    label = { Text("Track #") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "File Path:\n${song.dataPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
