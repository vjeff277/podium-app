package app.podiumpodcasts.podium.ui

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import app.podiumpodcasts.podium.PodcastEpisode
import app.podiumpodcasts.podium.PodcastRepository
import app.podiumpodcasts.podium.sampleEpisodes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val queue: List<PodcastEpisode> = emptyList(),
    val currentEpisodeId: Long? = null,
    val isQueueVisible: Boolean = false,
    val isPlaying: Boolean = false,
) {
    val currentEpisode: PodcastEpisode?
        get() = queue.firstOrNull { it.id == currentEpisodeId }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PodcastRepository(application.applicationContext)
    private var mediaPlayer: MediaPlayer? = null

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            queue = repository.demoQueue(),
            currentEpisodeId = repository.demoQueue().firstOrNull()?.id,
            isQueueVisible = false,
            isPlaying = false,
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        val initialQueue = _uiState.value.queue
        if (initialQueue.isNotEmpty()) {
            selectEpisode(initialQueue.first(), autoplay = false)
        }
    }

    fun toggleQueue() {
        _uiState.value = _uiState.value.copy(isQueueVisible = !_uiState.value.isQueueVisible)
    }

    fun moveEpisode(fromIndex: Int, toIndex: Int) {
        val queue = _uiState.value.queue.toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices) return

        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)

        val currentEpisodeId = _uiState.value.currentEpisodeId
        _uiState.value = _uiState.value.copy(queue = queue, currentEpisodeId = currentEpisodeId)
    }

    fun selectEpisode(episode: PodcastEpisode, autoplay: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            currentEpisodeId = episode.id,
            isQueueVisible = false,
        )
        if (autoplay) {
            playEpisode(episode)
        }
    }

    fun removeEpisode(id: Long) {
        val currentQueue = _uiState.value.queue
        val nextQueue = currentQueue.filterNot { it.id == id }
        val current = _uiState.value.currentEpisodeId
        val nextCurrent = when {
            current == id -> nextQueue.firstOrNull()?.id
            current != null -> current
            else -> null
        }

        if (nextQueue.isNotEmpty() && nextCurrent == null) {
            _uiState.value = _uiState.value.copy(queue = nextQueue, currentEpisodeId = nextQueue.first().id)
            return
        }

        _uiState.value = _uiState.value.copy(
            queue = nextQueue,
            currentEpisodeId = nextCurrent,
            isQueueVisible = true,
        )
    }

    fun playPause() {
        val current = _uiState.value.currentEpisode ?: return
        if (_uiState.value.isPlaying) {
            mediaPlayer?.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
            return
        }
        playEpisode(current)
    }

    fun onEpisodeFinished() {
        val queue = _uiState.value.queue
        val currentIndex = queue.indexOfFirst { it.id == _uiState.value.currentEpisodeId }

        if (currentIndex >= 0 && currentIndex < queue.lastIndex) {
            val nextEpisode = queue[currentIndex + 1]
            selectEpisode(nextEpisode, autoplay = true)
        } else {
            stopPlayback()
        }
    }

    private fun playEpisode(episode: PodcastEpisode) {
        stopPlayback()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                onEpisodeFinished()
            }
            setDataSource(episode.url)
            prepareAsync()
            setOnPreparedListener {
                start()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
            setOnErrorListener { _, _, _ ->
                _uiState.value = _uiState.value.copy(isPlaying = false)
                true
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
