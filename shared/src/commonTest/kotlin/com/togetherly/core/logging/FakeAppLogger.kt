package com.togetherly.core.logging

class FakeAppLogger : AppLogger {

    data class LogCall(val level: String, val tag: String, val message: String, val throwable: Throwable?)

    val calls: MutableList<LogCall> = mutableListOf()

    override fun debug(tag: String, message: String) {
        calls += LogCall("debug", tag, message, null)
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        calls += LogCall("warn", tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        calls += LogCall("error", tag, message, throwable)
    }
}
