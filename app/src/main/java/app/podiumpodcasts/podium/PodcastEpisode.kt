package app.podiumpodcasts.podium

import java.util.Date

data class PodcastEpisode(
    val id: Long,
    val title: String,
    val podcastName: String,
    val url: String,
    val durationSeconds: Long,
    val publishedAt: Date,
    val isDownloaded: Boolean = false,
)

fun sampleEpisodes(): List<PodcastEpisode> = listOf(
    PodcastEpisode(
        id = 1L,
        title = "Opening the show",
        podcastName = "Night Shift Stories",
        url = "https://feed.example.com/episode-1.mp3",
        durationSeconds = 1820,
        publishedAt = Date(),
    ),
    PodcastEpisode(
        id = 2L,
        title = "Interview: Built for an uncertain internet",
        podcastName = "Night Shift Stories",
        url = "https://feed.example.com/episode-2.mp3",
        durationSeconds = 2485,
        publishedAt = Date(System.currentTimeMillis() - 86400000),
    ),
    PodcastEpisode(
        id = 3L,
        title = "Production notes",
        podcastName = "Night Shift Stories",
        url = "https://feed.example.com/episode-3.mp3",
        durationSeconds = 1645,
        publishedAt = Date(System.currentTimeMillis() - 172800000),
    )
)
