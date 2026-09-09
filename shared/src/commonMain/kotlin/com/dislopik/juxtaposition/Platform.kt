package com.dislopik.juxtaposition

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform