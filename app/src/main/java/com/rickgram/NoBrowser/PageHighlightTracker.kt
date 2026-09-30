package com.rickgram.NoBrowser

sealed interface PageHighlightAction {
    data object None : PageHighlightAction
    data object Clear : PageHighlightAction
    data class Update(val xFraction: Float, val yFraction: Float) : PageHighlightAction
}

class PageHighlightTracker {
    private var active = false

    fun show(xFraction: Float, yFraction: Float): PageHighlightAction {
        active = true
        return updateAt(xFraction, yFraction)
    }

    fun move(xFraction: Float, yFraction: Float): PageHighlightAction {
        if (!active) {
            return show(xFraction, yFraction)
        }
        return updateAt(xFraction, yFraction)
    }

    fun hide(): PageHighlightAction {
        if (!active) {
            return PageHighlightAction.None
        }
        active = false
        return PageHighlightAction.Clear
    }

    private fun updateAt(xFraction: Float, yFraction: Float) = PageHighlightAction.Update(
        xFraction = xFraction.coerceIn(0f, 1f),
        yFraction = yFraction.coerceIn(0f, 1f)
    )
}
