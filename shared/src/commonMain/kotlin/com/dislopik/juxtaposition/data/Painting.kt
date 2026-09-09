package com.dislopik.juxtaposition.data

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encodes a drawing the way Juxtaposition stores Wii U memos: a bilevel, uncompressed
 * 32-bit TGA, zlib-compressed, then base64.
 *
 * The site's own upload path runs with `autodetectFormat: false`, so a PNG would be
 * rejected outright; TGA-zlib ("tgaz") is the format the server actually inflates and reads.
 */
object Painting {

    /** The canvas the Wii U memo pad uses, and what the site's painting UI draws on. */
    const val WIDTH = 320
    const val HEIGHT = 120

    /**
     * @param argb pixels in row-major order, 0xAARRGGBB, [WIDTH] x [HEIGHT].
     * @return base64 of the zlib-compressed TGA, ready for the `painting` form field.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encode(argb: IntArray, width: Int = WIDTH, height: Int = HEIGHT): String {
        require(argb.size >= width * height) {
            "expected ${width * height} pixels but got ${argb.size}"
        }
        return Base64.encode(zlibCompress(toTga(argb, width, height)))
    }

    /** Uncompressed 32-bit true-colour TGA with a top-left origin. */
    internal fun toTga(argb: IntArray, width: Int, height: Int): ByteArray {
        val out = ByteArray(HEADER_SIZE + width * height * 4)

        out[2] = 2 // uncompressed true-colour
        out[12] = (width and 0xFF).toByte()
        out[13] = ((width ushr 8) and 0xFF).toByte()
        out[14] = (height and 0xFF).toByte()
        out[15] = ((height ushr 8) and 0xFF).toByte()
        out[16] = 32 // bits per pixel
        out[17] = 0x20 // top-left origin

        var o = HEADER_SIZE
        for (i in 0 until width * height) {
            val pixel = argb[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val a = (pixel ushr 24) and 0xFF

            // Anything not drawn on counts as paper. Then threshold to pure black or white:
            // paintings are bilevel, and dithered greys just turn to mud at this size.
            val value = if (a < 128) 255 else if ((r + g + b) / 3 > 127) 255 else 0
            val byte = value.toByte()

            out[o++] = byte // B
            out[o++] = byte // G
            out[o++] = byte // R
            out[o++] = 0xFF.toByte() // A
        }
        return out
    }

    private const val HEADER_SIZE = 18
}
