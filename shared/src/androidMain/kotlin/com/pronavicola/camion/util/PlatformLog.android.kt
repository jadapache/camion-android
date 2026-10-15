package com.pronavicola.camion.util

actual object PlatformLog {
    actual fun v(tag: String, message: String) {
        android.util.Log.v(tag, message)
    }

    actual fun d(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }

    actual fun i(tag: String, message: String) {
        android.util.Log.i(tag, message)
    }

    actual fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable)
        } else {
            android.util.Log.e(tag, message)
        }
    }
}
