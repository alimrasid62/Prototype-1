package com.alimrasid.prototype1.env

import android.util.Log
import java.util.HashSet

class Logger(clazz: Class<*>? = null) {
    companion object {
        private const val DEFAULT_TAG = "tensorflow"
        private const val DEFAULT_MIN_LOG_LEVEL = Log.DEBUG

        // Classes to be ignored when examining the stack trace
        private val IGNORED_CLASS_NAMES: Set<String>

        init {
            IGNORED_CLASS_NAMES = HashSet(3)
            IGNORED_CLASS_NAMES.add("dalvik.system.VMStack")
            IGNORED_CLASS_NAMES.add("java.lang.Thread")
            Logger::class.java.canonicalName?.let { IGNORED_CLASS_NAMES.add(it) }
        }

        private fun getCallerSimpleName(): String {
            // Get the current callstack so we can pull the class of the caller off of it.
            val stackTrace = Thread.currentThread().stackTrace

            for (elem in stackTrace) {
                val className = elem.className
                if (!IGNORED_CLASS_NAMES.contains(className)) {
                    // We're only interested in the simple name of the class, not the complete package.
                    val classParts = className.split("\\.".toRegex()).toTypedArray()
                    return classParts[classParts.size - 1]
                }
            }

            return Logger::class.java.simpleName
        }
    }

    private val tag: String
    private val messagePrefix: String
    private var minLogLevel = DEFAULT_MIN_LOG_LEVEL

    init {
        this.tag = DEFAULT_TAG
        val prefix = clazz?.simpleName ?: getCallerSimpleName()
        this.messagePrefix = if (prefix.length > 0) "$prefix: " else prefix
    }

    fun setMinLogLevel(minLogLevel: Int) {
        this.minLogLevel = minLogLevel
    }

    fun isLoggable(logLevel: Int): Boolean {
        return logLevel >= minLogLevel || Log.isLoggable(tag, logLevel)
    }

    private fun toMessage(format: String, vararg args: Any): String {
        return messagePrefix + if (args.isNotEmpty()) String.format(format, *args) else format
    }

    fun v(format: String, vararg args: Any) {
        if (isLoggable(Log.VERBOSE)) {
            Log.v(tag, toMessage(format, *args))
        }
    }

    fun v(t: Throwable, format: String, vararg args: Any) {
        if (isLoggable(Log.VERBOSE)) {
            Log.v(tag, toMessage(format, *args), t)
        }
    }

    fun d(format: String, vararg args: Any) {
        if (isLoggable(Log.DEBUG)) {
            Log.d(tag, toMessage(format, *args))
        }
    }

    fun d(t: Throwable, format: String, vararg args: Any) {
        if (isLoggable(Log.DEBUG)) {
            Log.d(tag, toMessage(format, *args), t)
        }
    }

    fun i(format: String, vararg args: Any) {
        if (isLoggable(Log.INFO)) {
            Log.i(tag, toMessage(format, *args))
        }
    }

    fun i(t: Throwable, format: String, vararg args: Any) {
        if (isLoggable(Log.INFO)) {
            Log.i(tag, toMessage(format, *args), t)
        }
    }

    fun w(format: String, vararg args: Any) {
        if (isLoggable(Log.WARN)) {
            Log.w(tag, toMessage(format, *args))
        }
    }

    fun w(t: Throwable, format: String, vararg args: Any) {
        if (isLoggable(Log.WARN)) {
            Log.w(tag, toMessage(format, *args), t)
        }
    }

    fun e(format: String, vararg args: Any) {
        if (isLoggable(Log.ERROR)) {
            Log.e(tag, toMessage(format, *args))
        }
    }

    fun e(t: Throwable, format: String, vararg args: Any) {
        if (isLoggable(Log.ERROR)) {
            Log.e(tag, toMessage(format, *args), t)
        }
    }
}
