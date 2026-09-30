package com.mygdx.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Screens here are plain drawables - {@link Main} owns the batch, camera and frame timing. */
public interface GameScreen {
    void draw(SpriteBatch b, float dt);
    default void enter() {}
    default void exit() {}
    /** Screens that paint their own backdrop opt out of the shared one. */
    default boolean customBackground() { return false; }
}
