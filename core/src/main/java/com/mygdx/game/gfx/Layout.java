package com.mygdx.game.gfx;

/**
 * A top-down stack of boxes: each {@link #block} takes the next slice of height and says where
 * it starts, so panels are placed by what came before them rather than by hand-tuned constants.
 */
public class Layout {

    private float cursor;
    private final float bottom;

    public Layout(float top, float bottom) {
        this.cursor = top;
        this.bottom = bottom;
    }

    /** Reserves {@code h} and leaves {@code gapAfter} below it. Returns the block's bottom edge. */
    public float block(float h, float gapAfter) {
        cursor -= h;
        float edge = cursor;
        cursor -= gapAfter;
        return edge;
    }

    public float block(float h) { return block(h, 0); }

    /** Height still free above the bottom limit. */
    public float remaining() { return cursor - bottom; }
}
