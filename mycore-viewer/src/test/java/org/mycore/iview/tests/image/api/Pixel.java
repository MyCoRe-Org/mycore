package org.mycore.iview.tests.image.api;

import java.awt.Color;

public class Pixel {

    public Pixel(Color color, Position position) {
        this.color = color;
        this.position = position;
    }

    private Color color;

    private Position position;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
