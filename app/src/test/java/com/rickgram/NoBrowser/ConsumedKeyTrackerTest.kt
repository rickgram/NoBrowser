package com.rickgram.NoBrowser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsumedKeyTrackerTest {
    @Test
    fun consumesTheMatchingKeyUpEvenWhenStateChangesAfterKeyDown() {
        val tracker = ConsumedKeyTracker<BrowserKey>()

        tracker.recordKeyDown(BrowserKey.One, consumed = true)

        assertTrue(tracker.isTracking(BrowserKey.One))
        assertTrue(tracker.consumeKeyUp(BrowserKey.One))
        assertFalse(tracker.isTracking(BrowserKey.One))
        assertFalse(tracker.consumeKeyUp(BrowserKey.One))
    }

    @Test
    fun doesNotConsumeKeyUpWhenTheKeyDownPassedThrough() {
        val tracker = ConsumedKeyTracker<BrowserKey>()

        tracker.recordKeyDown(BrowserKey.One, consumed = false)

        assertFalse(tracker.consumeKeyUp(BrowserKey.One))
    }
}
