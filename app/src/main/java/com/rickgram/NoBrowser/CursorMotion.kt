package com.rickgram.NoBrowser

data class CursorMovement(
    val x: Float,
    val y: Float,
    val scrollX: Int = 0,
    val scrollY: Int = 0
)

object CursorMotion {
    fun move(
        x: Float,
        y: Float,
        direction: CursorDirection,
        viewportWidth: Float,
        viewportHeight: Float,
        step: Float,
        edgeInset: Float
    ): CursorMovement {
        val maxX = (viewportWidth - edgeInset).coerceAtLeast(edgeInset)
        val maxY = (viewportHeight - edgeInset).coerceAtLeast(edgeInset)
        val targetX = x + when (direction) {
            CursorDirection.Left -> -step
            CursorDirection.Right -> step
            CursorDirection.Up,
            CursorDirection.Down -> 0f
        }
        val targetY = y + when (direction) {
            CursorDirection.Up -> -step
            CursorDirection.Down -> step
            CursorDirection.Left,
            CursorDirection.Right -> 0f
        }

        return CursorMovement(
            x = targetX.coerceIn(edgeInset, maxX),
            y = targetY.coerceIn(edgeInset, maxY),
            scrollX = when {
                targetX < edgeInset -> -1
                targetX > maxX -> 1
                else -> 0
            },
            scrollY = when {
                targetY < edgeInset -> -1
                targetY > maxY -> 1
                else -> 0
            }
        )
    }
}
