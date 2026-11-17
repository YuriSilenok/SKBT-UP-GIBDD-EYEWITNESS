package com.example.gibddochevidets

// Application logs never contain payloads, coordinates, tokens, URLs or exception details.
object PrivacyLog {
    fun d(tag: String, message: String): Int = 0
    fun e(tag: String, message: String): Int = 0
    fun e(tag: String, message: String, error: Throwable): Int = 0
    fun w(tag: String, message: String): Int = 0
    fun w(tag: String, message: String, error: Throwable): Int = 0
    fun i(tag: String, message: String): Int = 0
}
