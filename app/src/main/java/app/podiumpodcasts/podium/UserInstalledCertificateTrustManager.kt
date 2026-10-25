package app.podiumpodcasts.podium

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Date

class PodcastRepository(private val context: Context) {
    private val client = PodiumHttpClientFactory.create(context)

    suspend fun loadEpisodesFromFeed(feedUrl: String, podcastName: String): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(feedUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful || response.body == null) return@withContext emptyList()

        val xml = response.body!!.string()
        parseRss(xml, podcastName)
    }

    fun demoQueue(): List<PodcastEpisode> = sampleEpisodes()

    fun installCertificate(fileName: String, certificateBytes: ByteArray): File {
        val certDir = File(context.filesDir, "certs")
        if (!certDir.exists()) certDir.mkdirs()
        val target = File(certDir, fileName.takeIf { it.contains('.') } ?: "$fileName.crt")
        target.writeBytes(certificateBytes)
        return target
    }

    fun availableCertificates(): List<String> = File(context.filesDir, "certs")
        .takeIf { it.exists() }
        ?.listFiles()
        ?.map { it.name }
        ?.filter { it.lowercase().endsWith(".crt") || it.lowercase().endsWith(".pem") || it.lowercase().endsWith(".cer") }
        .orEmpty()

    private fun parseRss(xml: String, podcastName: String): List<PodcastEpisode> {
        val episodes = mutableListOf<PodcastEpisode>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(xml.reader(), null)

        var eventType = parser.eventType
        var currentTitle: String? = null
        var currentUrl: String? = null
        var currentPubDate: String? = null
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "item" -> insideItem = true
                        "title" -> if (insideItem && currentTitle == null) currentTitle = parser.nextText()
                        "enclosure" -> {
                            val url = parser.getAttributeValue(null, "url")
                            if (url != null) currentUrl = url
                        }
                        "pubdate" -> if (insideItem) currentPubDate = parser.nextText()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.lowercase() == "item") {
                        if (!currentTitle.isNullOrBlank() && !currentUrl.isNullOrBlank()) {
                            episodes += PodcastEpisode(
                                id = (episodes.size + 1L),
                                title = currentTitle!!,
                                podcastName = podcastName,
                                url = currentUrl!!,
                                durationSeconds = 1800L,
                                publishedAt = parseDate(currentPubDate) ?: Date(),
                            )
                        }
                        currentTitle = null
                        currentUrl = null
                        currentPubDate = null
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }

        return episodes.ifEmpty { demoQueue() }
    }

    private fun parseDate(raw: String?): Date {
        if (raw.isNullOrBlank()) return Date()
        return try {
            Date(raw)
        } catch (_: Exception) {
            Date()
        }
    }
}
