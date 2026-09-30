package com.rickgram.NoBrowser

enum class BrowserKey {
    One,
    Zero,
    Star,
    Pound,
    Up,
    Down,
    Left,
    Right,
    Select,
    Back
}

enum class EditableFieldKind {
    None,
    SingleLine,
    Multiline
}

enum class CursorDirection {
    Up,
    Down,
    Left,
    Right
}

sealed interface CursorAction {
    data object PassThrough : CursorAction
    data object ShowCursor : CursorAction
    data object HideCursor : CursorAction
    data class Move(val direction: CursorDirection) : CursorAction
    data object Click : CursorAction
    data object ZoomIn : CursorAction
    data object ZoomOut : CursorAction
    data object ResetZoom : CursorAction
    data object FocusPreviousField : CursorAction
    data object FocusNextField : CursorAction
    data object ExitField : CursorAction
}

class CursorKeyRouter {
    var cursorEnabled: Boolean = false
        private set

    var fieldKind: EditableFieldKind = EditableFieldKind.None

    fun handle(key: BrowserKey): CursorAction {
        if (fieldKind != EditableFieldKind.None) {
            if (key == BrowserKey.Back) {
                return CursorAction.ExitField
            }
            if (fieldKind == EditableFieldKind.SingleLine) {
                return when (key) {
                    BrowserKey.Up -> CursorAction.FocusPreviousField
                    BrowserKey.Down -> CursorAction.FocusNextField
                    else -> CursorAction.PassThrough
                }
            }
            return CursorAction.PassThrough
        }

        if (key == BrowserKey.One) {
            cursorEnabled = !cursorEnabled
            return if (cursorEnabled) CursorAction.ShowCursor else CursorAction.HideCursor
        }

        if (!cursorEnabled) {
            return CursorAction.PassThrough
        }

        return when (key) {
            BrowserKey.Up -> CursorAction.Move(CursorDirection.Up)
            BrowserKey.Down -> CursorAction.Move(CursorDirection.Down)
            BrowserKey.Left -> CursorAction.Move(CursorDirection.Left)
            BrowserKey.Right -> CursorAction.Move(CursorDirection.Right)
            BrowserKey.Select -> CursorAction.Click
            BrowserKey.Star -> CursorAction.ZoomOut
            BrowserKey.Pound -> CursorAction.ZoomIn
            BrowserKey.Zero -> CursorAction.ResetZoom
            BrowserKey.One,
            BrowserKey.Back -> CursorAction.PassThrough
        }
    }
}

class ConsumedKeyTracker<T> {
    private val consumedKeys = mutableSetOf<T>()

    fun recordKeyDown(key: T, consumed: Boolean) {
        if (consumed) {
            consumedKeys += key
        }
    }

    fun isTracking(key: T): Boolean = key in consumedKeys

    fun consumeKeyUp(key: T): Boolean = consumedKeys.remove(key)
}
