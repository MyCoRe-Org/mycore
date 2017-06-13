package org.mycore.viewer.alto.model;


public class MCRAltoWordChange extends MCRAltoChange {

    public MCRAltoWordChange(String file, String type, int hpos, int vpos, int width, int height, String from, String to) {
        super(file, type);
        this.hpos = hpos;
        this.vpos = vpos;
        this.width = width;
        this.height = height;
        this.from = from;
        this.to = to;
    }

    public MCRAltoWordChange() {
    }

    private int hpos;
    private int vpos;
    private int width;
    private int height;
    private String from;
    private String to;

    public int getHpos() {
        return hpos;
    }

    public int getVpos() {
        return vpos;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
