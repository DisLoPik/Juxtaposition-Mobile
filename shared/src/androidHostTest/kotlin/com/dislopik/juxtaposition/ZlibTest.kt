package com.dislopik.juxtaposition

import com.dislopik.juxtaposition.data.Painting
import com.dislopik.juxtaposition.data.zlibCompress
import com.dislopik.juxtaposition.data.zlibStored
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * The server inflates a painting before reading it, so the encoder output has to be a
 * valid zlib stream. These round-trip both implementations through a real inflater.
 */
class ZlibTest {

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            if (n == 0 && inflater.needsInput()) break
            out.write(buffer, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }

    @Test
    fun platformCompressorRoundTrips() {
        val original = ByteArray(50_000) { (it % 251).toByte() }
        assertContentEquals(original, inflate(zlibCompress(original)))
    }

    @Test
    fun storedFallbackRoundTrips() {
        // Deliberately spans more than one 64KB stored block.
        val original = ByteArray(150_000) { (it % 97).toByte() }
        assertContentEquals(original, inflate(zlibStored(original)))
    }

    @Test
    fun storedFallbackHandlesEmptyInput() {
        assertContentEquals(ByteArray(0), inflate(zlibStored(ByteArray(0))))
    }

    @Test
    fun anEncodedPaintingInflatesBackToItsTga() {
        val pixels = IntArray(Painting.WIDTH * Painting.HEIGHT) { i ->
            if (i % 7 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val expectedTga = Painting.toTga(pixels, Painting.WIDTH, Painting.HEIGHT)

        val base64 = Painting.encode(pixels)
        val raw = java.util.Base64.getDecoder().decode(base64)

        assertContentEquals(expectedTga, inflate(raw))
        // A bilevel drawing should compress hard; if it does not, something is wrong.
        assertTrue(raw.size < expectedTga.size / 4, "painting did not compress (${raw.size} bytes)")
    }
}
