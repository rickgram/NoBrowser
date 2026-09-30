package com.rickgram.NoBrowser

import org.junit.Assert.assertEquals
import org.junit.Test

class PageHighlightTrackerTest {
    @Test
    fun updatesAtThePointerAndClearsWhenTheCursorHides() {
        val tracker = PageHighlightTracker()

        assertEquals(
            PageHighlightAction.Update(xFraction = 0.25f, yFraction = 0.75f),
            tracker.show(xFraction = 0.25f, yFraction = 0.75f)
        )
        assertEquals(
            PageHighlightAction.Update(xFraction = 0.5f, yFraction = 0.4f),
            tracker.move(xFraction = 0.5f, yFraction = 0.4f)
        )
        assertEquals(PageHighlightAction.Clear, tracker.hide())
        assertEquals(PageHighlightAction.None, tracker.hide())
    }
}
