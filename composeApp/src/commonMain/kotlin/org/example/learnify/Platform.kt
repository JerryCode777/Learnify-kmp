package org.example.learnify

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform