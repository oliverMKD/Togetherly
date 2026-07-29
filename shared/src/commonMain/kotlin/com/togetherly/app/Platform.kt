package com.togetherly.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform