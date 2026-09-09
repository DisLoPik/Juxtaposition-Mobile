package com.dislopik.juxtaposition.data

/**
 * Kotlin/Native has no bundled deflate, so iOS uses the stored-block stream. It is larger
 * on the wire but decodes identically, which keeps paintings working without cinterop.
 */
actual fun zlibCompress(data: ByteArray): ByteArray = zlibStored(data)
