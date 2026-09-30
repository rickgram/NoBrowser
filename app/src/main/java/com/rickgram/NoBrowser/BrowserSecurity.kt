package com.rickgram.NoBrowser

import java.util.Locale

internal enum class NavigationTarget {
    WEB,
    EXTERNAL_APP,
    BLOCKED
}

internal object BrowserSecurity {
    fun isWebScheme(scheme: String?): Boolean =
        scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)

    fun classifyNavigationScheme(scheme: String?): NavigationTarget {
        if (isWebScheme(scheme)) {
            return NavigationTarget.WEB
        }

        val normalizedScheme = scheme?.lowercase(Locale.ROOT)
        if (normalizedScheme.isNullOrBlank() ||
            !VALID_SCHEME.matches(normalizedScheme) ||
            normalizedScheme in BLOCKED_EXTERNAL_SCHEMES
        ) {
            return NavigationTarget.BLOCKED
        }

        return NavigationTarget.EXTERNAL_APP
    }

    fun sanitizeFileName(fileName: String): String {
        var sanitized = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            .replace(Regex("[<>:\"/\\\\|?*]"), "_")

        while (sanitized.contains("..")) {
            sanitized = sanitized.replace("..", ".")
        }

        sanitized = sanitized.trim().trim('.').take(MAX_FILE_NAME_LENGTH)
        return sanitized.ifBlank { DEFAULT_FILE_NAME }
    }

    private const val DEFAULT_FILE_NAME = "download"
    private const val MAX_FILE_NAME_LENGTH = 127
    private val VALID_SCHEME = Regex("[a-z][a-z0-9+.-]*")
    private val BLOCKED_EXTERNAL_SCHEMES = setOf(
        "about",
        "blob",
        "content",
        "data",
        "file",
        "intent",
        "javascript"
    )
}
