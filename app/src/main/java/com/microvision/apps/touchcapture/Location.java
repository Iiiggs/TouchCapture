package com.microvision.apps.touchcapture;


import android.graphics.Point;


public class Location {
    int x;
    int y;

    public Point getSpritePosition() {
        return new Point(x * Constants.SPRITE_WIDTH, y * Constants.SPRITE_HEIGHT);
    }

    public Location(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("tiles: %d, %d, spritePosition:%d, %d", x, y, getSpritePosition().x, getSpritePosition().y);
    }

    // todo: return screen coordinates as well as tile index
}


