package com.dislopik.juxtaposition.data

/**
 * zlib (RFC 1950) compression, which Juxtaposition's painting upload expects.
 *
 * The server inflates the blob before handing it to ImageMagick, so the bytes must carry a
 * real zlib header and Adler-32 trailer; a bare deflate stream is rejected.
 */
expect fun zlibCompress(data: ByteArray): ByteArray

/**
 * A zlib stream built entirely from stored (uncompressed) deflate blocks.
 *
 * Every inflater accepts this, so it works as a portable fallback where no compressor is
 * available. It does not shrink the data, which is fine for the small painting canvas.
 */
fun zlibStored(data: ByteArray): ByteArray {
    val out = ArrayList<Byte>(data.size + data.size / MAX_BLOCK * 5 + 16)

    // zlib header: deflate, 32K window, default level, no preset dictionary.
    // 0x78 0x01 is chosen so the two bytes together are a multiple of 31.
    out.add(0x78)
    out.add(0x01)

    if (data.isEmpty()) {
        // A single empty final stored block.
        out.add(0x01)
        out.add(0); out.add(0)
        out.add(0xFF.toByte()); out.add(0xFF.toByte())
    } else {
        var offset = 0
        while (offset < data.size) {
            val len = minOf(MAX_BLOCK, data.size - offset)
            val isFinal = offset + len >= data.size
            out.add(if (isFinal) 1 else 0) // BFINAL, BTYPE=00 (stored)
            out.add((len and 0xFF).toByte())
            out.add(((len ushr 8) and 0xFF).toByte())
            val nlen = len.inv() and 0xFFFF
            out.add((nlen and 0xFF).toByte())
            out.add(((nlen ushr 8) and 0xFF).toByte())
            for (i in 0 until len) out.add(data[offset + i])
            offset += len
        }
    }

    val adler = adler32(data)
    out.add(((adler ushr 24) and 0xFF).toByte())
    out.add(((adler ushr 16) and 0xFF).toByte())
    out.add(((adler ushr 8) and 0xFF).toByte())
    out.add((adler and 0xFF).toByte())

    return out.toByteArray()
}

internal fun adler32(data: ByteArray): Int {
    var a = 1
    var b = 0
    for (byte in data) {
        a = (a + (byte.toInt() and 0xFF)) % ADLER_MOD
        b = (b + a) % ADLER_MOD
    }
    return (b shl 16) or a
}

private const val MAX_BLOCK = 65535
private const val ADLER_MOD = 65521
