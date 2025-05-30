/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.iview2.services;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.mycore.common.MCRClassTools;

/**
 * Container class handled by hibernate to store and retrieve job information for the next tiling request.
 * @author Thomas Scheffler (yagee)
 *
 */
@Entity
@Table(name = "MCRTileJob",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "derivate", "path" }),
    },
    indexes = {
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

    @Column
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
    public MCRTileJob clone()  {
        MCRTileJob clone = MCRClassTools.clone(getClass(), super::clone);

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
        return new MessageFormat("MCRTileJob [derivate:{0}, path:{1}, added:{2}]", Locale.ROOT).format(
            new Object[] { getDerivate(), getPath(), getAdded() });
    }

}
