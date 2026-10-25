package app.podiumpodcasts.podium.ui

import androidx.lifecycle.ViewModel
import app.podiumpodcasts.podium.PodcastEpisode
import app.podiumpodcasts.podium.sampleEpisodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val queue: List<PodcastEpisode> = emptyList(),
    val currentEpisodeId: Long? = null,
    val isQueueVisible: Boolean = false,
) {
    val currentEpisode: PodcastEpisode?
        get() = queue.firstOrNull { it.id == currentEpisodeId }
}

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            queue = sampleEpisodes(),
            currentEpisodeId = sampleEpisodes().first().id,
            isQueueVisible = false,
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun toggleQueue() {
        _uiState.value = _uiState.value.copy(
            isQueueVisible = !_uiState.value.isQueueVisible
        )
    }

    fun selectEpisode(episode: PodcastEpisode) {
        _uiState.value = _uiState.value.copy(
            currentEpisodeId = episode.id,
            isQueueVisible = false,
        )
    }

    fun removeEpisode(id: Long) {
        val nextQueue = _uiState.value.queue.filterNot { it.id == id }
        val current = _uiState.value.currentEpisodeId
        val nextCurrent = when {
            current == id -> nextQueue.firstOrNull()?.id
            current != null -> current
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            queue = nextQueue,
            currentEpisodeId = nextCurrent,
            isQueueVisible = true,
        )
    }

    fun onEpisodeFinished() {
        val queue = _uiState.value.queue
        val currentIndex = queue.indexOfFirst { it.id == _uiState.value.currentEpisodeId }

        if (currentIndex < queue.size - 1) {
            val nextEpisode = queue[currentIndex + 1]
            _uiState.value = _uiState.value.copy(
                currentEpisodeId = nextEpisode.id,
                isQueueVisible = false
            )
        }
    }
}
