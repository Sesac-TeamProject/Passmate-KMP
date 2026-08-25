package org.sesacteamproject.passmate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform