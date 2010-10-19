package org.mycore.iview2.services;

import java.util.Date;

/**
 * Container class handled by hibernate to store and retrieve job information for the next tiling request.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRTileJob {

    private long id;

    private String derivate;

    private String path;

    private char status;

    private Date added;

    private Date start;

    private Date finished;

    private long tiles;

    private long width;

    private long height;

    private long zoomLevel;

    /**
     * @return the date when this job was created
     */
    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }

    /**
     * @return derivate ID
     */
    public String getDerivate() {
        return derivate;
    }

    public void setDerivate(String derivate) {
        this.derivate = derivate;
    }

    /**
     * @return the date when this job was finished
     */
    public Date getFinished() {
        return finished;
    }

    public void setFinished(Date finished) {
        this.finished = finished;
    }

    /**
     * @return height of the image
     */
    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    /**
     * @return internal id
     */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return absolute image path rooted by derivate
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the date when the job was last started
     */
    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * @return {@link MCRJobState#toChar()} of current status
     */
    public char getStatus() {
        return status;
    }

    public void setStatus(MCRJobState status) {
        this.status = status.toChar();
    }

    @SuppressWarnings("unused")
    private void setStatus(char status) {
        this.status = status;
    }

    /**
     * @return number of generated tiles
     */
    public long getTiles() {
        return tiles;
    }

    public void setTiles(long tiles) {
        this.tiles = tiles;
    }

    /**
     * @return image width
     */
    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    /**
     * @return number of zoom levels
     */
    public long getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(long zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

}
