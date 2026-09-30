package com.rickgram.NoBrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserSecurityTest {
    @Test
    fun allowsOnlyHttpAndHttpsSchemes() {
        assertTrue(BrowserSecurity.isWebScheme("https"))
        assertTrue(BrowserSecurity.isWebScheme("HTTP"))
        assertFalse(BrowserSecurity.isWebScheme("file"))
        assertFalse(BrowserSecurity.isWebScheme("content"))
        assertFalse(BrowserSecurity.isWebScheme("javascript"))
        assertFalse(BrowserSecurity.isWebScheme(null))
    }

    @Test
    fun removesPathTraversalAndUnsafeCharactersFromFileNames() {
        assertEquals("secret_.txt", BrowserSecurity.sanitizeFileName("../../secret?.txt"))
        assertEquals("report_2026_.pdf", BrowserSecurity.sanitizeFileName("report:2026*.pdf"))
    }

    @Test
    fun usesFallbackForEmptyFileName() {
        assertEquals("download", BrowserSecurity.sanitizeFileName("..."))
    }
}
