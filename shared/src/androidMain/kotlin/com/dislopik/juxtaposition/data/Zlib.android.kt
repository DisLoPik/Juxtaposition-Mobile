package com.dislopik.juxtaposition.data

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater

actual fun zlibCompress(data: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    try {
        deflater.setInput(data)
        deflater.finish()
        val out = ByteArrayOutputStream(data.size / 4 + 64)
        val buffer = ByteArray(8 * 1024)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            if (n > 0) out.write(buffer, 0, n)
        }
        return out.toByteArray()
    } finally {
        deflater.end()
    }
}
