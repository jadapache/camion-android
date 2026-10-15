package com.pronavicola.camion.util

import java.text.SimpleDateFormat
import java.util.Date

actual object PlatformLog {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    private fun format(level: String, tag: String, message: String): String {
        val now = dateFormat.format(Date())
        return "$now [$level] [$tag] $message"
    }

    actual fun v(tag: String, message: String) {
        println(format("VERBOSE", tag, message))
    }

    actual fun d(tag: String, message: String) {
        println(format("DEBUG", tag, message))
    }

    actual fun i(tag: String, message: String) {
        println(format("INFO ", tag, message))
    }

    actual fun w(tag: String, message: String) {
        System.err.println(format("WARN ", tag, message))
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println(format("ERROR", tag, message))
        throwable?.printStackTrace()
    }
}
