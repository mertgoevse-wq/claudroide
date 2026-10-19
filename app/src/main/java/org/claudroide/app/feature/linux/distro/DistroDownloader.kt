package org.claudroide.app.feature.linux.distro

import java.io.File

interface DistroDownloader {
    /**
     * Downloads the distro image. Callback notifies progress 0.0 to 1.0.
     * Throws exception on failure.
     */
    suspend fun download(image: DistroImage, destination: File, onProgress: (Float) -> Unit): Boolean
    
    /**
     * Reusable block to verify a downloaded image.
     */
    fun verifyChecksum(image: DistroImage, downloadedFile: File): Boolean {
        return ChecksumVerifier.verifyDistroImage(downloadedFile, image)
    }
}
