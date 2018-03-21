/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.common.processing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mycore.common.MCRSessionMgr;

/**
 * Can be used as base class for an {@link MCRProcessable}. This class offers some
 * convenient methods but does not handle everything.
 * 
 * <p>
 * If you extend this class make sure to call {@link #setStatus(MCRProcessableStatus)},
 * {@link #setProgress(Integer)} and {@link #setProgressText(String)}. Otherwise the
 * event handlers are not fired.
 * </p>
 * 
 * @author Matthias Eichner
 */
public class MCRAbstractProcessable extends MCRAbstractProgressable implements MCRProcessable {

    protected String name;

    protected String userId;

    protected MCRProcessableStatus status;

    protected Throwable error;

    protected Instant createTime;

    protected Instant startTime;

    protected Instant endTime;

    protected Map<String, Object> properties;

    protected final List<MCRProcessableStatusListener> statusListener;

    public MCRAbstractProcessable() {
        super();
        this.name = null;
        if (MCRSessionMgr.hasCurrentSession()) {
            // do not create a new session! (getCurrentSession() is wrong named!)
            this.userId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        }
        this.status = MCRProcessableStatus.created;
        this.error = null;

        this.createTime = Instant.now();
        this.startTime = null;
        this.endTime = null;

        this.properties = new HashMap<>();

        this.statusListener = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Sets the name for this process.
     * 
     * @param name human readable name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Sets the user identifier responsible for this processable.
     * 
     * @param userId the user id
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getUserId() {
        return this.userId;
    }

    @Override
    public Throwable getError() {
        return this.error;
    }

    /**
     * Sets the internal processable error. This will set the status to failed.
     * 
     * @param error the error
     */
    public void setError(Throwable error) {
        this.error = error;
        setStatus(MCRProcessableStatus.failed);
    }

    /**
     * Sets the new status.
     * 
     * @param status the new status
     */
    public void setStatus(MCRProcessableStatus status) {
        MCRProcessableStatus oldStatus = this.status;
        this.status = status;
        if (status.equals(MCRProcessableStatus.processing)) {
            this.startTime = Instant.now();
        }
        if (status.equals(MCRProcessableStatus.successful) || status.equals(MCRProcessableStatus.failed) ||
                status.equals(MCRProcessableStatus.canceled)) {
            this.endTime = Instant.now();
        }
        fireStatusChanged(oldStatus);
    }

    @Override
    public MCRProcessableStatus getStatus() {
        return this.status;
    }

    @Override
    public Instant getStartTime() {
        return this.startTime;
    }

    @Override
    public Instant getCreateTime() {
        return this.createTime;
    }

    @Override
    public Instant getEndTime() {
        return this.endTime;
    }

    @Override
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public void addStatusListener(MCRProcessableStatusListener listener) {
        this.statusListener.add(listener);
    }

    @Override
    public void removeStatusListener(MCRProcessableStatusListener listener) {
        this.statusListener.remove(listener);
    }

    protected void fireStatusChanged(MCRProcessableStatus oldStatus) {
        synchronized (this.statusListener) {
            this.statusListener.forEach(listener -> listener.onStatusChange(this, oldStatus, getStatus()));
        }
    }

}
