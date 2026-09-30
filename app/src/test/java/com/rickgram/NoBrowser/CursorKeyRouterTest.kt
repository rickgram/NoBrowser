package com.rickgram.NoBrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorKeyRouterTest {
    @Test
    fun oneTogglesCursorOnlyOutsideEditableFields() {
        val router = CursorKeyRouter()

        assertEquals(CursorAction.ShowCursor, router.handle(BrowserKey.One))
        assertTrue(router.cursorEnabled)

        router.fieldKind = EditableFieldKind.SingleLine

        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.One))
        assertTrue(router.cursorEnabled)

        router.fieldKind = EditableFieldKind.None

        assertEquals(CursorAction.HideCursor, router.handle(BrowserKey.One))
        assertFalse(router.cursorEnabled)
    }

    @Test
    fun cursorModeMapsDirectionalSelectAndZoomKeys() {
        val router = CursorKeyRouter()
        router.handle(BrowserKey.One)

        assertEquals(CursorAction.Move(CursorDirection.Up), router.handle(BrowserKey.Up))
        assertEquals(CursorAction.Move(CursorDirection.Down), router.handle(BrowserKey.Down))
        assertEquals(CursorAction.Move(CursorDirection.Left), router.handle(BrowserKey.Left))
        assertEquals(CursorAction.Move(CursorDirection.Right), router.handle(BrowserKey.Right))
        assertEquals(CursorAction.Click, router.handle(BrowserKey.Select))
        assertEquals(CursorAction.ZoomOut, router.handle(BrowserKey.Star))
        assertEquals(CursorAction.ZoomIn, router.handle(BrowserKey.Pound))
        assertEquals(CursorAction.ResetZoom, router.handle(BrowserKey.Zero))
    }

    @Test
    fun cursorAndZoomKeysPassThroughWhileEditing() {
        val router = CursorKeyRouter()
        router.handle(BrowserKey.One)
        router.fieldKind = EditableFieldKind.SingleLine

        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.One))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Star))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Pound))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Zero))
        assertTrue(router.cursorEnabled)
    }

    @Test
    fun upAndDownNavigateSingleLineFieldsButNotMultilineFields() {
        val router = CursorKeyRouter()
        router.fieldKind = EditableFieldKind.SingleLine

        assertEquals(CursorAction.FocusPreviousField, router.handle(BrowserKey.Up))
        assertEquals(CursorAction.FocusNextField, router.handle(BrowserKey.Down))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Left))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Right))

        router.fieldKind = EditableFieldKind.Multiline

        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Up))
        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Down))
    }

    @Test
    fun backExitsAnEditableFieldBeforeBrowserNavigation() {
        val router = CursorKeyRouter()
        router.fieldKind = EditableFieldKind.SingleLine

        assertEquals(CursorAction.ExitField, router.handle(BrowserKey.Back))

        router.fieldKind = EditableFieldKind.None

        assertEquals(CursorAction.PassThrough, router.handle(BrowserKey.Back))
    }
}
