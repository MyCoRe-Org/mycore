package org.mycore.iview2.services;

import java.text.MessageFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Container class handled by hibernate to store and retrieve job information for the next tiling request.
 * @author Thomas Scheffler (yagee)
 *
 */
@Entity
@Table(name = "MCRTileJob", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "derivate", "path" }),
}, indexes = {
    @Index(columnList = "derivate,status")
})
@NamedQueries({
    @NamedQuery(name = "MCRTileJob.all",
        query = "SELECT job FROM MCRTileJob as job"),
    @NamedQuery(name = "MCRTileJob.countByStateListByDerivate",
        query = "SELECT count(job) FROM MCRTileJob as job WHERE job.derivate= :derivateId AND job.status IN (:states)")
})
public class MCRTileJob implements Cloneable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column(length = 64)
    private String derivate;

    @Column(length = 255)
    private String path;

    @Column
    private char status;

    @Column
    private Date added;

    @Column
    private Date start;

    @Column
    private Date finished;

    @Column
    private long tiles;

    @Column
    private long width;

    @Column
    private long height;

    @Column
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

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRTileJob clone() {
        MCRTileJob clone = new MCRTileJob();
        clone.setAdded(getAdded());
        clone.setDerivate(getDerivate());
        clone.setFinished(getFinished());
        clone.setHeight(getHeight());
        clone.setId(getId());
        clone.setPath(getPath());
        clone.setStart(getStart());
        clone.setStatus(getStatus());
        clone.setTiles(getTiles());
        clone.setWidth(getWidth());
        clone.setZoomLevel(getZoomLevel());
        return clone;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return MessageFormat.format("MCRTileJob [derivate:{0}, path:{1}, added:{2}]", getDerivate(), getPath(),
            getAdded());
    }

}
