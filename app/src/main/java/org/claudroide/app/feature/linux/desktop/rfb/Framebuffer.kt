package org.claudroide.app.feature.linux.desktop.rfb

/**
 * Client-side framebuffer storing decoded pixels as ARGB ints
 * (0xAARRGGBB, Android Bitmap-compatible). Decodes wire pixels according
 * to the negotiated RfbPixelFormat (RFC 6143 §5, §7.4).
 *
 * Dimensions are mutable: the DesktopSize pseudo-encoding resizes it live.
 */
class Framebuffer(initialWidth: Int, initialHeight: Int) {

    var width: Int = initialWidth
        private set
    var height: Int = initialHeight
        private set

    var pixels: IntArray = IntArray(width * height)
        private set

    var format: RfbPixelFormat = RfbPixelFormat.BGRA32
        set(value) {
            require(value.trueColorFlag) { "Only true-color formats supported (colormap fallback not implemented)" }
            field = value
        }

    /**
     * Resizes the framebuffer, discarding content (RFC 6143 §7.8.2 DesktopSize semantics).
     * Rejects absurd sizes so a malicious server cannot trigger huge allocations:
     * the cap (~32M pixels ≈ 128 MB ARGB) is far above any real desktop geometry.
     */
    fun resize(newWidth: Int, newHeight: Int) {
        require(newWidth > 0 && newHeight > 0) { "Framebuffer dimensions must be positive" }
        require(newWidth.toLong() * newHeight.toLong() <= MAX_PIXELS) {
            "Framebuffer ${newWidth}x$newHeight exceeds the ${MAX_PIXELS}-pixel safety cap"
        }
        width = newWidth
        height = newHeight
        pixels = IntArray(width * height)
    }

    /** Decode [pixelCount] raw wire pixels into framebuffer row-major positions [start]..[start+n). */
    fun decodeRawPixels(data: ByteArray, offset: Int, pixelCount: Int, start: Int) {
        val f = format
        val bpp = f.bytesPerPixel
        require(start >= 0 && start + pixelCount <= pixels.size) { "Update exceeds framebuffer bounds" }
        require(offset + pixelCount.toLong() * bpp <= data.size) { "Pixel payload truncated" }

        var src = offset
        for (i in 0 until pixelCount) {
            pixels[start + i] = toArgb(readPixel(data, src, f), f)
            src += bpp
        }
    }

    /** Blit a w x h raw rectangle at framebuffer position (x, y). */
    fun decodeRawRect(data: ByteArray, offset: Int, x: Int, y: Int, w: Int, h: Int) {
        require(x >= 0 && y >= 0 && x + w <= width && y + h <= height) {
            "Rectangle ($x,$y ${w}x$h) outside ${width}x$height framebuffer"
        }
        val f = format
        val bpp = f.bytesPerPixel
        require(offset.toLong() + w.toLong() * h * bpp <= data.size) { "Pixel payload truncated" }
        var src = offset
        for (row in 0 until h) {
            val dstBase = (y + row) * width + x
            for (col in 0 until w) {
                pixels[dstBase + col] = toArgb(readPixel(data, src, f), f)
                src += bpp
            }
        }
    }

    private fun readPixel(data: ByteArray, src: Int, f: RfbPixelFormat): Long {
        var v = 0L
        if (f.bigEndianFlag) {
            for (b in 0 until f.bytesPerPixel) v = (v shl 8) or (data[src + b].toLong() and 0xFF)
        } else {
            for (b in f.bytesPerPixel - 1 downTo 0) v = (v shl 8) or (data[src + b].toLong() and 0xFF)
        }
        return v
    }

    private fun toArgb(v: Long, f: RfbPixelFormat): Int {
        val r = scale(extract(v, f.redShift, f.redMax), f.redMax)
        val g = scale(extract(v, f.greenShift, f.greenMax), f.greenMax)
        val b = scale(extract(v, f.blueShift, f.blueMax), f.blueMax)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun extract(v: Long, shift: Int, max: Int): Int =
        ((v shr shift) and max.toLong()).toInt()

    private fun scale(value: Int, max: Int): Int {
        if (max == 255 || max <= 0) return value and 0xFF
        return (value * 255 + max / 2) / max
    }

    companion object {
        /** ~32M pixels (≈ 8K display); guards against OOM from hostile ServerInit/DesktopSize. */
        const val MAX_PIXELS: Long = 32L * 1024 * 1024
    }
}
