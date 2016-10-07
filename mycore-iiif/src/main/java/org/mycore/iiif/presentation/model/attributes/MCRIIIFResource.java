package org.mycore.iiif.presentation.model.attributes;

public class MCRIIIFResource extends MCRIIIFLDURI {

    protected MCRIIIFService service;

    private int width;

    private int height;

    public MCRIIIFResource(String uri, MCRDCMIType type, String format) {
        super(uri, type.toString(), format);
    }

    public MCRIIIFResource(String uri, MCRDCMIType type) {
        super(uri, type.toString(), null);
    }

    public void setService(MCRIIIFService service) {
        this.service = service;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

}
