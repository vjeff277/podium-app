package app.podiumpodcasts.podium.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import app.podiumpodcasts.podium.PodcastEpisode
import app.podiumpodcasts.podium.PodcastRepository
import app.podiumpodcasts.podium.sampleEpisodes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val QUEUE_PREFS = "podium_queue_prefs"
private const val QUEUE_KEY = "queue_order"

data class PlayerUiState(
    val queue: List<PodcastEpisode> = emptyList(),
    val currentEpisodeId: Long? = null,
    val isQueueVisible: Boolean = false,
    val isPlaying: Boolean = false,
    val lastFeedUrl: String = "",
    val lastFeedError: String? = null,
) {
    val currentEpisode: PodcastEpisode?
        get() = queue.firstOrNull { it.id == currentEpisodeId }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PodcastRepository(application.applicationContext)
    private val prefs = application.getSharedPreferences(QUEUE_PREFS, Context.MODE_PRIVATE)
    private var mediaPlayer: MediaPlayer? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            queue = loadPersistedQueueOrDefaults(),
            currentEpisodeId = loadPersistedQueueOrDefaults().firstOrNull()?.id,
            isQueueVisible = false,
            isPlaying = false,
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        val initialQueue = _uiState.value.queue
        if (initialQueue.isNotEmpty()) {
            val first = initialQueue.first()
            _uiState.value = _uiState.value.copy(currentEpisodeId = first.id)
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
        persistQueue(queue)
        _uiState.value = _uiState.value.copy(queue = queue)
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
            persistQueue(nextQueue)
            _uiState.value = _uiState.value.copy(queue = nextQueue, currentEpisodeId = nextQueue.first().id)
            return
        }

        persistQueue(nextQueue)
        _uiState.value = _uiState.value.copy(
            queue = nextQueue,
            currentEpisodeId = nextCurrent,
            isQueueVisible = true,
        )
    }

    fun loadFeed(feedUrl: String, podcastName: String = "Imported Podcast") {
        if (feedUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(lastFeedError = "Feed URL is required")
            return
        }

        viewModelScope.launch {
            try {
                val episodes = repository.loadEpisodesFromFeed(feedUrl, podcastName)
                if (episodes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(lastFeedError = "No episodes were found in that feed.")
                    return@launch
                }

                val existingQueue = _uiState.value.queue.toMutableList()
                val deduped = episodes.filter { episode -> existingQueue.none { it.url == episode.url } }
                val merged = (existingQueue + deduped).ifEmpty { episodes }
                persistQueue(merged)

                _uiState.value = _uiState.value.copy(
                    queue = merged,
                    currentEpisodeId = merged.firstOrNull()?.id,
                    isQueueVisible = false,
                    lastFeedUrl = feedUrl,
                    lastFeedError = null,
                )

                if (merged.isNotEmpty()) {
                    selectEpisode(merged.first(), autoplay = true)
                }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(lastFeedError = error.message ?: "Unable to load feed")
            }
        }
    }

    fun installSelfSignedCertificate(fileName: String, bytes: ByteArray): Boolean {
        return try {
            val file = repository.installCertificate(fileName, bytes)
            file.exists() && file.length() > 0
        } catch (_: Exception) {
            false
        }
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

    private fun persistQueue(queue: List<PodcastEpisode>) {
        val serialized = queue.joinToString("||") { episode ->
            listOf(
                episode.id,
                episode.title,
                episode.podcastName,
                episode.url,
                episode.durationSeconds,
                episode.publishedAt.time,
            ).joinToString(",")
        }
        prefs.edit().putString(QUEUE_KEY, serialized).apply()
    }

    private fun loadPersistedQueueOrDefaults(): List<PodcastEpisode> {
        val serialized = prefs.getString(QUEUE_KEY, null) ?: return repository.demoQueue()
        if (serialized.isBlank()) return repository.demoQueue()

        return serialized.split("||")
            .filter { it.isNotBlank() }
            .map { entry ->
                val parts = entry.split(",")
                if (parts.size < 6) return@map null
                try {
                    PodcastEpisode(
                        id = parts[0].toLong(),
                        title = parts[1],
                        podcastName = parts[2],
                        url = parts[3],
                        durationSeconds = parts[4].toLong(),
                        publishedAt = java.util.Date(parts[5].toLong()),
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .filterNotNull()
            .ifEmpty { repository.demoQueue() }
    }
}
