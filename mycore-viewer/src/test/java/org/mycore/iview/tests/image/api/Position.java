package org.mycore.iview.tests.image.api;

public class Position {

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private int x;

    private int y;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
