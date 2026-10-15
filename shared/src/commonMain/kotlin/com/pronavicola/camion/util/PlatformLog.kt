package com.pronavicola.camion.util

expect object PlatformLog {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object Log {
    fun v(tag: String, message: String) {
        PlatformLog.v(tag, message)
        FileLogger.instance?.log("VERBOSE", tag, message)
    }

    fun d(tag: String, message: String) {
        PlatformLog.d(tag, message)
        FileLogger.instance?.log("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        PlatformLog.i(tag, message)
        FileLogger.instance?.log("INFO", tag, message)
    }

    fun w(tag: String, message: String) {
        PlatformLog.w(tag, message)
        FileLogger.instance?.log("WARN", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        PlatformLog.e(tag, message, throwable)
        FileLogger.instance?.log("ERROR", tag, message, throwable)
    }
}
