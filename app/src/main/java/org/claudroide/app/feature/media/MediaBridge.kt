package org.claudroide.app.feature.media

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Request specification for image and media generation.
 */
data class MediaGenerationRequest(
    val prompt: String,
    val negativePrompt: String = "",
    val providerId: String = "omniroute",
    val modelId: String = "segmind/flux-schnell",
    val aspectRatio: String = "1:1",
    val steps: Int = 25,
    val cfgScale: Float = 7.5f,
    val modifiers: List<String> = emptyList(),
)

/**
 * Result of a media generation job.
 */
data class MediaGenerationResult(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val modelUsed: String,
    val providerUsed: String,
    val localFilePath: String?,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val errorMessage: String? = null,
)

/**
 * Provider descriptor for media generation capabilities.
 */
data class MediaProviderInfo(
    val id: String,
    val name: String,
    val description: String,
    val models: List<String>,
    val defaultModel: String,
    val isFree: Boolean = false,
)

/**
 * Multi-provider media generation bridge connecting OmniRoute, Segmind, Fal, and free Pollinations.
 */
class MediaBridge(private val context: Context) {

    private val mediaDir: File by lazy {
        val dir = File(context.filesDir, "generated_media")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val history = mutableListOf<MediaGenerationResult>()

    fun getAvailableProviders(): List<MediaProviderInfo> {
        return listOf(
            MediaProviderInfo(
                id = "omniroute",
                name = "OmniRoute Bridge (Port 20128)",
                description = "Multi-provider gateway routing to Segmind, FLUX, SDXL, and Gemini.",
                models = listOf(
                    "segmind/flux-schnell",
                    "segmind/sdxl1.0-txt2img",
                    "segmind/sd3.5-large-txt2img",
                    "pollinations/flux",
                    "agy/gemini-3.1-flash-image",
                    "unorouter/flux.2-flex"
                ),
                defaultModel = "segmind/flux-schnell"
            ),
            MediaProviderInfo(
                id = "segmind",
                name = "Segmind Direct / Via Route",
                description = "Ultra-fast FLUX.1 and SDXL pipelines with low latency.",
                models = listOf(
                    "segmind/flux-schnell",
                    "segmind/flux-dev",
                    "segmind/sdxl1.0-txt2img",
                    "segmind/sd3.5-large-txt2img"
                ),
                defaultModel = "segmind/flux-schnell"
            ),
            MediaProviderInfo(
                id = "pollinations",
                name = "Pollinations.ai (Keyless Fallback)",
                description = "Direct keyless FLUX and Turbo generation with 100% uptime fallback.",
                models = listOf("flux", "turbo"),
                defaultModel = "flux",
                isFree = true
            )
        )
    }

    suspend fun generateImage(request: MediaGenerationRequest): MediaGenerationResult = withContext(Dispatchers.IO) {
        val fullPrompt = if (request.modifiers.isNotEmpty()) {
            "${request.prompt}, ${request.modifiers.joinToString(", ")}"
        } else {
            request.prompt
        }

        // Try primary provider, with automatic keyless Pollinations fallback
        try {
            if (request.providerId == "pollinations") {
                return@withContext generateViaPollinations(fullPrompt, request)
            } else {
                return@withContext generateViaOmniRoute(fullPrompt, request)
            }
        } catch (e: Exception) {
            // Robust fallback to Pollinations if local route or API key fails
            try {
                val fallback = generateViaPollinations(fullPrompt, request)
                return@withContext fallback
            } catch (fallbackError: Exception) {
                val failed = MediaGenerationResult(
                    prompt = fullPrompt,
                    modelUsed = request.modelId,
                    providerUsed = request.providerId,
                    localFilePath = null,
                    imageUrl = null,
                    status = "FAILED",
                    errorMessage = e.message ?: "Unbekannter Fehler bei Mediengenerierung"
                )
                history.add(0, failed)
                return@withContext failed
            }
        }
    }

    private fun generateViaOmniRoute(fullPrompt: String, request: MediaGenerationRequest): MediaGenerationResult {
        val endpoint = URL("http://127.0.0.1:20128/v1/images/generations")
        val conn = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val (w, h) = dimensionsFromAspectRatio(request.aspectRatio)
        val payload = JSONObject().apply {
            put("model", request.modelId)
            put("prompt", fullPrompt)
            put("size", "${w}x${h}")
            put("n", 1)
        }

        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }

        val code = conn.responseCode
        if (code in 200..299) {
            val responseText = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(responseText)
            val dataArr = json.optJSONArray("data")
            val imgUrl = dataArr?.optJSONObject(0)?.optString("url")
            val b64 = dataArr?.optJSONObject(0)?.optString("b64_json")

            val file = File(mediaDir, "gen_${System.currentTimeMillis()}.png")
            if (!b64.isNullOrEmpty()) {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                FileOutputStream(file).use { it.write(bytes) }
            } else if (!imgUrl.isNullOrEmpty()) {
                downloadFile(imgUrl, file)
            }

            val result = MediaGenerationResult(
                prompt = fullPrompt,
                modelUsed = request.modelId,
                providerUsed = request.providerId,
                localFilePath = file.absolutePath,
                imageUrl = imgUrl,
                status = "SUCCESS"
            )
            history.add(0, result)
            return result
        } else {
            val errText = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            throw IllegalStateException("OmniRoute Media Error ($code): $errText")
        }
    }

    private fun generateViaPollinations(fullPrompt: String, request: MediaGenerationRequest): MediaGenerationResult {
        val (w, h) = dimensionsFromAspectRatio(request.aspectRatio)
        val encodedPrompt = URLEncoder.encode(fullPrompt, "UTF-8")
        val pollUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$w&height=$h&model=${request.modelId.substringAfterLast('/')}&nologo=true"

        val file = File(mediaDir, "poll_${System.currentTimeMillis()}.jpg")
        downloadFile(pollUrl, file)

        val result = MediaGenerationResult(
            prompt = fullPrompt,
            modelUsed = "pollinations/${request.modelId}",
            providerUsed = "Pollinations.ai",
            localFilePath = file.absolutePath,
            imageUrl = pollUrl,
            status = "SUCCESS"
        )
        history.add(0, result)
        return result
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 20000
        conn.readTimeout = 40000
        conn.inputStream.use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun dimensionsFromAspectRatio(ratio: String): Pair<Int, Int> = when (ratio) {
        "16:9" -> Pair(1024, 576)
        "9:16" -> Pair(576, 1024)
        "4:3" -> Pair(1024, 768)
        "21:9" -> Pair(1152, 496)
        else -> Pair(768, 768) // 1:1 square
    }

    fun getHistory(): List<MediaGenerationResult> = history.toList()
}
