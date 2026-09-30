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
    fun routesWyzeCallbackToAnExternalApp() {
        assertEquals(
            NavigationTarget.EXTERNAL_APP,
            BrowserSecurity.classifyNavigationScheme("wyze")
        )
    }

    @Test
    fun keepsWebLinksInTheBrowserAndBlocksUnsafeSchemes() {
        assertEquals(NavigationTarget.WEB, BrowserSecurity.classifyNavigationScheme("https"))
        assertEquals(NavigationTarget.WEB, BrowserSecurity.classifyNavigationScheme("HTTP"))
        assertEquals(NavigationTarget.EXTERNAL_APP, BrowserSecurity.classifyNavigationScheme("mailto"))
        assertEquals(NavigationTarget.EXTERNAL_APP, BrowserSecurity.classifyNavigationScheme("tel"))

        listOf("javascript", "file", "content", "data", "blob", "about", "intent").forEach {
            assertEquals(NavigationTarget.BLOCKED, BrowserSecurity.classifyNavigationScheme(it))
        }
        assertEquals(NavigationTarget.BLOCKED, BrowserSecurity.classifyNavigationScheme(null))
        assertEquals(NavigationTarget.BLOCKED, BrowserSecurity.classifyNavigationScheme(""))
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
