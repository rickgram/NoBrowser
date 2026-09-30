package com.rickgram.NoBrowser

import org.junit.Assert.assertEquals
import org.junit.Test

class CursorMotionTest {
    @Test
    fun movesWithinTheViewportWithoutScrolling() {
        val movement = CursorMotion.move(
            x = 50f,
            y = 50f,
            direction = CursorDirection.Right,
            viewportWidth = 200f,
            viewportHeight = 300f,
            step = 20f,
            edgeInset = 10f
        )

        assertEquals(70f, movement.x)
        assertEquals(50f, movement.y)
        assertEquals(0, movement.scrollX)
        assertEquals(0, movement.scrollY)
    }

    @Test
    fun holdsAtVerticalEdgesAndRequestsPageScrolling() {
        val down = CursorMotion.move(
            x = 50f,
            y = 290f,
            direction = CursorDirection.Down,
            viewportWidth = 200f,
            viewportHeight = 300f,
            step = 20f,
            edgeInset = 10f
        )
        val up = CursorMotion.move(
            x = 50f,
            y = 10f,
            direction = CursorDirection.Up,
            viewportWidth = 200f,
            viewportHeight = 300f,
            step = 20f,
            edgeInset = 10f
        )

        assertEquals(290f, down.y)
        assertEquals(1, down.scrollY)
        assertEquals(10f, up.y)
        assertEquals(-1, up.scrollY)
    }

    @Test
    fun holdsAtHorizontalEdgesAndRequestsPageScrolling() {
        val right = CursorMotion.move(
            x = 190f,
            y = 50f,
            direction = CursorDirection.Right,
            viewportWidth = 200f,
            viewportHeight = 300f,
            step = 20f,
            edgeInset = 10f
        )
        val left = CursorMotion.move(
            x = 10f,
            y = 50f,
            direction = CursorDirection.Left,
            viewportWidth = 200f,
            viewportHeight = 300f,
            step = 20f,
            edgeInset = 10f
        )

        assertEquals(190f, right.x)
        assertEquals(1, right.scrollX)
        assertEquals(10f, left.x)
        assertEquals(-1, left.scrollX)
    }
}
