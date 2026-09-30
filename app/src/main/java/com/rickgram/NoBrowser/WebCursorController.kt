package com.rickgram.NoBrowser

import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlin.math.abs

class WebCursorController(
    private val webView: WebView,
    private val overlay: CursorOverlayView
) {
    private val keyRouter = CursorKeyRouter()
    private val consumedKeys = ConsumedKeyTracker<BrowserKey>()
    private val pageHighlightTracker = PageHighlightTracker()
    private var relativeZoom = 1f

    init {
        webView.addJavascriptInterface(FocusBridge(), FOCUS_BRIDGE_NAME)
    }

    fun onPageStarted() {
        relativeZoom = 1f
        setEditableField(EditableFieldKind.None)
    }

    fun installPageObserver() {
        webView.evaluateJavascript(
            "$FOCUS_OBSERVER_SCRIPT\n$HIGHLIGHT_SETUP_SCRIPT"
        ) {
            if (overlay.visibility == View.VISIBLE) {
                dispatchPageHighlight(pageHighlightTracker.moveAtCursor())
            }
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val key = event.toBrowserKey() ?: return false

        if (event.action == KeyEvent.ACTION_UP) {
            return consumedKeys.consumeKeyUp(key)
        }
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        if (event.repeatCount > 0 && consumedKeys.isTracking(key) && key in NON_REPEATING_KEYS) {
            return true
        }

        val action = keyRouter.handle(key)
        val consumed = action != CursorAction.PassThrough
        consumedKeys.recordKeyDown(key, consumed)

        if (consumed) {
            perform(action)
        }
        return consumed
    }

    fun exitFieldIfEditing(): Boolean {
        if (keyRouter.fieldKind == EditableFieldKind.None) {
            return false
        }
        perform(CursorAction.ExitField)
        return true
    }

    private fun perform(action: CursorAction) {
        when (action) {
            CursorAction.ShowCursor -> updateOverlayVisibility()
            CursorAction.HideCursor -> updateOverlayVisibility()
            is CursorAction.Move -> moveCursor(action.direction)
            CursorAction.Click -> tapAtCursor()
            CursorAction.ZoomIn -> changeZoom(ZOOM_STEP)
            CursorAction.ZoomOut -> changeZoom(1f / ZOOM_STEP)
            CursorAction.ResetZoom -> resetZoom()
            CursorAction.FocusPreviousField -> focusAdjacentField(-1)
            CursorAction.FocusNextField -> focusAdjacentField(1)
            CursorAction.ExitField -> exitField()
            CursorAction.PassThrough -> Unit
        }
    }

    private fun moveCursor(direction: CursorDirection) {
        val movement = overlay.move(direction)
        val scrollScript = if (movement.scrollX != 0 || movement.scrollY != 0) {
            val x = movement.scrollX * SCROLL_DISTANCE_CSS_PIXELS
            val y = movement.scrollY * SCROLL_DISTANCE_CSS_PIXELS
            "window.scrollBy($x, $y);"
        } else {
            ""
        }
        dispatchPageHighlight(pageHighlightTracker.moveAtCursor(), scrollScript)
    }

    private fun tapAtCursor() {
        webView.requestFocus()
        val time = SystemClock.uptimeMillis()
        val x = overlay.cursorX
        val y = overlay.cursorY
        val down = MotionEvent.obtain(
            time,
            time,
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        ).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        val up = MotionEvent.obtain(
            time,
            time + TAP_DURATION_MILLIS,
            MotionEvent.ACTION_UP,
            x,
            y,
            0
        ).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    private fun changeZoom(multiplier: Float) {
        val newZoom = (relativeZoom * multiplier).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (abs(newZoom - relativeZoom) < 0.001f) {
            return
        }
        webView.zoomBy(newZoom / relativeZoom)
        relativeZoom = newZoom
    }

    private fun resetZoom() {
        if (abs(relativeZoom - 1f) < 0.001f) {
            return
        }
        webView.zoomBy(1f / relativeZoom)
        relativeZoom = 1f
    }

    private fun focusAdjacentField(offset: Int) {
        webView.evaluateJavascript(
            """
                (() => {
                  const selector = 'input:not([type="hidden"]):not([disabled]), textarea:not([disabled]), select:not([disabled]), [contenteditable="true"]';
                  const fields = Array.from(document.querySelectorAll(selector)).filter((field) => {
                    const rect = field.getBoundingClientRect();
                    const style = getComputedStyle(field);
                    return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
                  });
                  if (!fields.length) return;
                  const current = fields.indexOf(document.activeElement);
                  const next = Math.max(0, Math.min(fields.length - 1, current + ($offset)));
                  fields[next].focus({ preventScroll: true });
                  fields[next].scrollIntoView({ block: 'center', inline: 'nearest' });
                })();
            """.trimIndent(),
            null
        )
    }

    private fun exitField() {
        webView.evaluateJavascript("document.activeElement?.blur();", null)
        val inputMethodManager = webView.context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(webView.windowToken, 0)
        setEditableField(EditableFieldKind.None)
        webView.requestFocus()
    }

    private fun setEditableField(kind: EditableFieldKind) {
        keyRouter.fieldKind = kind
        updateOverlayVisibility()
    }

    private fun updateOverlayVisibility() {
        val shouldShow =
            keyRouter.cursorEnabled && keyRouter.fieldKind == EditableFieldKind.None

        overlay.visibility = if (shouldShow) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (shouldShow) {
            overlay.post { dispatchPageHighlight(pageHighlightTracker.showAtCursor()) }
        } else {
            dispatchPageHighlight(pageHighlightTracker.hide())
        }
    }

    private fun dispatchPageHighlight(
        action: PageHighlightAction,
        beforeScript: String = ""
    ) {
        val highlightScript = when (action) {
            is PageHighlightAction.Update ->
                "window.__noBrowserHighlightAt?.(${action.xFraction}, ${action.yFraction});"
            PageHighlightAction.Clear -> "window.__noBrowserClearHighlight?.();"
            PageHighlightAction.None -> ""
        }
        val script = beforeScript + highlightScript
        if (script.isNotEmpty()) {
            webView.evaluateJavascript(script, null)
        }
    }

    private fun PageHighlightTracker.showAtCursor(): PageHighlightAction =
        show(overlay.xFraction(), overlay.yFraction())

    private fun PageHighlightTracker.moveAtCursor(): PageHighlightAction =
        move(overlay.xFraction(), overlay.yFraction())

    private fun CursorOverlayView.xFraction(): Float =
        if (width > 0) cursorX / width else 0.5f

    private fun CursorOverlayView.yFraction(): Float =
        if (height > 0) cursorY / height else 0.5f

    private inner class FocusBridge {
        @JavascriptInterface
        fun onFocusChanged(kind: String) {
            webView.post {
                setEditableField(
                    when (kind) {
                        "single" -> EditableFieldKind.SingleLine
                        "multiline" -> EditableFieldKind.Multiline
                        else -> EditableFieldKind.None
                    }
                )
            }
        }
    }

    private fun KeyEvent.toBrowserKey(): BrowserKey? = when (keyCode) {
        KeyEvent.KEYCODE_1,
        KeyEvent.KEYCODE_NUMPAD_1 -> BrowserKey.One
        KeyEvent.KEYCODE_0,
        KeyEvent.KEYCODE_NUMPAD_0 -> BrowserKey.Zero
        KeyEvent.KEYCODE_STAR,
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> BrowserKey.Star
        KeyEvent.KEYCODE_POUND -> BrowserKey.Pound
        KeyEvent.KEYCODE_DPAD_UP -> BrowserKey.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> BrowserKey.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> BrowserKey.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> BrowserKey.Right
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> BrowserKey.Select
        KeyEvent.KEYCODE_BACK -> BrowserKey.Back
        else -> null
    }

    private companion object {
        const val FOCUS_BRIDGE_NAME = "NoBrowserFocusBridge"
        const val SCROLL_DISTANCE_CSS_PIXELS = 20
        const val TAP_DURATION_MILLIS = 50L
        const val ZOOM_STEP = 1.2f
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 3f

        val NON_REPEATING_KEYS = setOf(BrowserKey.One, BrowserKey.Select, BrowserKey.Zero)

        val FOCUS_OBSERVER_SCRIPT =
            """
            (() => {
              const bridge = window.$FOCUS_BRIDGE_NAME;
              if (!bridge) return;
              const fieldKind = () => {
                const active = document.activeElement;
                if (!active || active === document.body || active === document.documentElement) return 'none';
                if (active instanceof HTMLTextAreaElement || active.isContentEditable || active instanceof HTMLSelectElement) return 'multiline';
                if (active instanceof HTMLInputElement) {
                  const nonTextTypes = new Set(['button', 'checkbox', 'color', 'file', 'hidden', 'image', 'radio', 'range', 'reset', 'submit']);
                  return nonTextTypes.has(active.type) ? 'none' : 'single';
                }
                return 'none';
              };
              const report = () => bridge.onFocusChanged(fieldKind());
              if (!window.__noBrowserFocusObserverInstalled) {
                document.addEventListener('focusin', report, true);
                document.addEventListener('focusout', () => setTimeout(report, 0), true);
                window.__noBrowserFocusObserverInstalled = true;
              }
              report();
            })();
            """.trimIndent()

        val HIGHLIGHT_SETUP_SCRIPT =
            """
            (() => {
              const attribute = 'data-nobrowser-pointer-highlight';
              const styleId = 'nobrowser-pointer-highlight-style';
              if (!document.getElementById(styleId)) {
                const style = document.createElement('style');
                style.id = styleId;
                style.textContent = `[${'$'}{attribute}] { outline: 3px solid #ffca28 !important; outline-offset: 2px !important; }`;
                (document.head || document.documentElement).appendChild(style);
              }
              window.__noBrowserClearHighlight = () => {
                document.querySelectorAll(`[${'$'}{attribute}]`).forEach((element) => element.removeAttribute(attribute));
              };
              window.__noBrowserHighlightAt = (xFraction, yFraction) => {
                window.__noBrowserClearHighlight();
                const x = Math.max(0, Math.min(window.innerWidth - 1, window.innerWidth * xFraction));
                const y = Math.max(0, Math.min(window.innerHeight - 1, window.innerHeight * yFraction));
                const element = document.elementFromPoint(x, y);
                const selector = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [role="button"], [role="link"], [onclick], summary';
                const target = element?.closest?.(selector);
                target?.setAttribute(attribute, '');
              };
            })();
            """.trimIndent()
    }
}
