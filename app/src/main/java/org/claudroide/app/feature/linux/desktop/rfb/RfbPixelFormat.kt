package org.claudroide.app.feature.linux.desktop.rfb

import java.io.DataInput
import java.io.DataOutput

/**
 * RFB PIXEL_FORMAT (RFC 6143 §7.4), 16 bytes on the wire.
 * All multi-byte integers are big endian; [bigEndianFlag] describes how the
 * *server* writes pixel values.
 */
data class RfbPixelFormat(
    val bitsPerPixel: Int,      // 8, 16 or 32
    val depth: Int,
    val bigEndianFlag: Boolean,
    val trueColorFlag: Boolean,
    val redMax: Int,
    val greenMax: Int,
    val blueMax: Int,
    val redShift: Int,
    val greenShift: Int,
    val blueShift: Int
) {
    val bytesPerPixel: Int get() = bitsPerPixel / 8

    fun writeTo(out: DataOutput) {
        out.writeByte(bitsPerPixel)
        out.writeByte(depth)
        out.writeByte(if (bigEndianFlag) 1 else 0)
        out.writeByte(if (trueColorFlag) 1 else 0)
        out.writeShort(redMax)
        out.writeShort(greenMax)
        out.writeShort(blueMax)
        out.writeByte(redShift)
        out.writeByte(greenShift)
        out.writeByte(blueShift)
        out.writeByte(0); out.writeByte(0); out.writeByte(0) // padding
    }

    fun toBytes(): ByteArray {
        val buf = java.io.ByteArrayOutputStream(16)
        writeTo(java.io.DataOutputStream(buf))
        return buf.toByteArray()
    }

    companion object {
        fun readFrom(input: DataInput): RfbPixelFormat {
            val bpp = input.readUnsignedByte()
            require(bpp == 8 || bpp == 16 || bpp == 32) { "Invalid bits-per-pixel $bpp (must be 8, 16 or 32)" }
            val depth = input.readUnsignedByte()
            val be = input.readUnsignedByte() != 0
            val tc = input.readUnsignedByte() != 0
            val rMax = input.readUnsignedShort()
            val gMax = input.readUnsignedShort()
            val bMax = input.readUnsignedShort()
            val rShift = input.readUnsignedByte()
            val gShift = input.readUnsignedByte()
            val bShift = input.readUnsignedByte()
            input.skipBytes(3) // padding
            return RfbPixelFormat(bpp, depth, be, tc, rMax, gMax, bMax, rShift, gShift, bShift)
        }

        /** ARGB_8888-style: 32bpp, little endian, shifts 16/8/0 — matches Android Bitmaps directly. */
        val BGRA32 = RfbPixelFormat(
            bitsPerPixel = 32, depth = 24, bigEndianFlag = false, trueColorFlag = true,
            redMax = 255, greenMax = 255, blueMax = 255,
            redShift = 16, greenShift = 8, blueShift = 0
        )
    }
}
