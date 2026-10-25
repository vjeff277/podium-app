package app.podiumpodcasts.podium

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class UserInstalledCertificateTrustManager(
    private val context: Context,
) : X509TrustManager {

    private val defaultTrustManager: X509TrustManager = buildDefaultTrustManager()

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager.acceptedIssuers
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType)
            return
        } catch (error: Exception) {
            val localCerts = loadUserInstalledCertificates()
            if (localCerts.isNotEmpty() && chain != null) {
                for (serverCert in chain) {
                    if (localCerts.any { it.publicKey == serverCert.publicKey }) {
                        return
                    }
                }
            }
            throw error
        }
    }

    private fun buildDefaultTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        val manager = factory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()

        return manager ?: throw IllegalStateException("No default X509TrustManager available")
    }

    private fun loadUserInstalledCertificates(): List<X509Certificate> {
        val certDir = File(context.filesDir, "certs")
        if (!certDir.exists()) return emptyList()

        val certs = mutableListOf<X509Certificate>()
        val certFactory = CertificateFactory.getInstance("X.509")

        certDir.listFiles()?.forEach { file ->
            if (file.extension.lowercase() in setOf("crt", "pem", "cer")) {
                try {
                    FileInputStream(file).use { input ->
                        certs += certFactory.generateCertificates(input)
                            .filterIsInstance<X509Certificate>()
                    }
                } catch (_: Exception) {
                    // Ignore malformed certs; the system trust manager will decide next.
                }
            }
        }
        return certs
    }
}

object PodiumHttpClientFactory {
    fun create(context: Context): OkHttpClient {
        val trustManager = UserInstalledCertificateTrustManager(context)
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }
}
