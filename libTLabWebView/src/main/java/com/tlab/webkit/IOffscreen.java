package com.tlab.webkit;

public interface IOffscreen {
    /**
     * Get content's scroll position x
     *
     * @return Page content's current scroll position x
     */
    int GetScrollX();

    /**
     * Get content's scroll position y
     *
     * @return Page content's current scroll position y
     */
    int GetScrollY();

    /**
     * Set content's scroll position.
     *
     * @param x Scroll position x of the destination
     * @param y Scroll position y of the destination
     */
    void ScrollTo(int x, int y);

    /**
     * Move the scrolled position of WebView
     *
     * @param x The amount of pixels to scroll by horizontally
     * @param y The amount of pixels to scroll by vertically
     */
    void ScrollBy(int x, int y);

    /**
     * Dispatch of a touch event.
     *
     * @param x      Touch position x
     * @param y      Touch position y
     * @param action Touch event type (TOUCH_DOWN: 0, TOUCH_UP: 1, TOUCH_MOVE: 2)
     */
    long TouchEvent(int x, int y, int action, long downTime);

    /**
     * Dispatch of a basic keycode event.
     *
     * @param key 'a', 'b', 'A' ....
     */
    void KeyEvent(char key);

    void KeyEvent(int keyCode);
}
