package org.claudroide.app.feature.linux.distro

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URI

class ResilientDistroDownloader(
    private val maxRetries: Int = 3,
    private val connectTimeoutMs: Int = 15000,
    private val readTimeoutMs: Int = 30000
) : DistroDownloader {

    override suspend fun download(
        image: DistroImage,
        destination: File,
        onProgress: (Float) -> Unit
    ): Boolean {
        destination.parentFile?.mkdirs()

        // If target file already exists and matches checksum, skip download
        if (destination.exists() && ChecksumVerifier.verifyDistroImage(destination, image)) {
            onProgress(1.0f)
            return true
        }

        val partFile = File(destination.parentFile, "${destination.name}.part")
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            attempt++
            try {
                downloadWithResume(image.url, partFile, onProgress)

                // Verify checksum after complete download
                if (ChecksumVerifier.verifyDistroImage(partFile, image)) {
                    if (destination.exists()) destination.delete()
                    if (partFile.renameTo(destination)) {
                        success = true
                        onProgress(1.0f)
                    } else {
                        // Copy fallback
                        partFile.copyTo(destination, overwrite = true)
                        partFile.delete()
                        success = true
                        onProgress(1.0f)
                    }
                } else {
                    // Corrupted download -> clean up .part file and retry
                    partFile.delete()
                    if (attempt >= maxRetries) {
                        throw IllegalStateException(
                            "Checksum verification failed for ${image.name} after $maxRetries attempts."
                        )
                    }
                }
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    throw e
                }
                // Exponential backoff before retry (500ms, 1000ms, 2000ms)
                val backoff = (500L * (1 shl (attempt - 1)))
                kotlinx.coroutines.delay(backoff)
            }
        }

        return success
    }

    private fun downloadWithResume(
        urlStr: String,
        partFile: File,
        onProgress: (Float) -> Unit
    ) {
        val existingBytes = if (partFile.exists()) partFile.length() else 0L
        val uri = URI(urlStr)
        val conn = uri.toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = connectTimeoutMs
        conn.readTimeout = readTimeoutMs
        conn.instanceFollowRedirects = true

        if (existingBytes > 0) {
            conn.setRequestProperty("Range", "bytes=$existingBytes-")
        }

        conn.connect()
        val responseCode = conn.responseCode

        val isResumed = responseCode == HttpURLConnection.HTTP_PARTIAL
        val isNew = responseCode == HttpURLConnection.HTTP_OK

        if (!isResumed && !isNew) {
            throw java.io.IOException("HTTP error response: $responseCode from $urlStr")
        }

        val totalLength = if (isResumed) {
            existingBytes + conn.contentLengthLong
        } else {
            conn.contentLengthLong
        }

        conn.inputStream.use { input ->
            if (isResumed) {
                RandomAccessFile(partFile, "rw").use { raf ->
                    raf.seek(existingBytes)
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var currentBytes = existingBytes
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        raf.write(buffer, 0, bytesRead)
                        currentBytes += bytesRead
                        if (totalLength > 0) {
                            onProgress(currentBytes.toFloat() / totalLength.toFloat())
                        }
                    }
                }
            } else {
                FileOutputStream(partFile, false).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var currentBytes = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        currentBytes += bytesRead
                        if (totalLength > 0) {
                            onProgress(currentBytes.toFloat() / totalLength.toFloat())
                        }
                    }
                }
            }
        }
    }
}
