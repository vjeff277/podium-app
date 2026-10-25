package app.podiumpodcasts.podium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.podiumpodcasts.podium.PodcastEpisode

@Composable
fun PlayerScreen(viewModel: PlayerViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Podium", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.playPause() }) {
                    Text(if (uiState.isPlaying) "Pause" else "Play")
                }
                Button(onClick = { viewModel.toggleQueue() }) {
                    Text("Queue")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = uiState.currentEpisode?.title ?: "No episode selected",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(text = uiState.currentEpisode?.podcastName ?: "Select a podcast")
                Text(text = if (uiState.isPlaying) "Playing now" else "Ready to play")

                if (uiState.currentEpisode != null) {
                    Button(
                        onClick = { viewModel.onEpisodeFinished() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Play next episode")
                    }
                }
            }
        }

        if (uiState.isQueueVisible) {
            QueueSheet(
                queue = uiState.queue,
                currentEpisodeId = uiState.currentEpisodeId,
                onEpisodeSelected = { episode -> viewModel.selectEpisode(episode, autoplay = true) },
                onRemove = viewModel::removeEpisode,
                onMoveUp = { index -> viewModel.moveEpisode(index, index - 1) },
                onMoveDown = { index -> viewModel.moveEpisode(index, index + 1) },
                onClose = viewModel::toggleQueue,
            )
        }
    }
}

@Composable
private fun QueueSheet(
    queue: List<PodcastEpisode>,
    currentEpisodeId: Long?,
    onEpisodeSelected: (PodcastEpisode) -> Unit,
    onRemove: (Long) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onClose: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Queue", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Close",
                    modifier = Modifier.clickable { onClose() },
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(queue) { index, episode ->
                    val isCurrent = episode.id == currentEpisodeId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEpisodeSelected(episode) }
                        ) {
                            Text(episode.title)
                            Text(
                                text = if (isCurrent) "Now playing" else "Queued",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "↑",
                                modifier = Modifier.clickable { if (index > 0) onMoveUp(index) },
                                color = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "↓",
                                modifier = Modifier.clickable { if (index < queue.size - 1) onMoveDown(index) },
                                color = if (index < queue.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Remove",
                                modifier = Modifier.clickable { onRemove(episode.id) },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
