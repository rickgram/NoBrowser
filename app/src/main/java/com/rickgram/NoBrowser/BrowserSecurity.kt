package com.rickgram.NoBrowser

internal object BrowserSecurity {
    fun isWebScheme(scheme: String?): Boolean =
        scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)

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
}
