package com.maxrave.kotlinytmusicscraper.extension

import java.security.MessageDigest
import kotlin.random.Random

fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(toByteArray())
    return hash.fold("", { str, it -> str + "%02x".format(it) })
}

fun randomString(length: Int): String {
    val chars = "0123456789abcdef"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}

fun String.verifyYouTubePlaylistId(): String {
    return if (startsWith("VL")) this else "VL$this"
}

fun String.isTwoLetterCode(): Boolean {
    val regex = "^[A-Za-z]{2}$".toRegex()
    return regex.matches(this)
}