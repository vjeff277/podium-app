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
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        durationSeconds = 1820,
        publishedAt = Date(),
    ),
    PodcastEpisode(
        id = 2L,
        title = "Interview: Built for an uncertain internet",
        podcastName = "Night Shift Stories",
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        durationSeconds = 2485,
        publishedAt = Date(System.currentTimeMillis() - 86400000),
    ),
    PodcastEpisode(
        id = 3L,
        title = "Production notes",
        podcastName = "Night Shift Stories",
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
        durationSeconds = 1645,
        publishedAt = Date(System.currentTimeMillis() - 172800000),
    ),
    PodcastEpisode(
        id = 4L,
        title = "After Hours",
        podcastName = "Night Shift Stories",
        url = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
        durationSeconds = 2140,
        publishedAt = Date(System.currentTimeMillis() - 259200000),
    )
)
