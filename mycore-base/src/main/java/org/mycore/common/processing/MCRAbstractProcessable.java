package org.mycore.common.processing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    protected MCRProcessableStatus status;

    protected Throwable error;

    protected Instant createTime;

    protected Instant startTime;

    protected Instant endTime;

    protected List<MCRProcessableStatusListener> statusListener;

    public MCRAbstractProcessable() {
        super();
        this.name = null;
        this.status = MCRProcessableStatus.created;
        this.error = null;

        this.createTime = Instant.now();
        this.startTime = null;
        this.endTime = null;

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
        this.status = MCRProcessableStatus.failed;
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
        if (status.equals(MCRProcessableStatus.successful) || status.equals(MCRProcessableStatus.failed)) {
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
    public void addStatusListener(MCRProcessableStatusListener listener) {
        this.statusListener.add(listener);
    }

    @Override
    public void removeStatusListener(MCRProcessableStatusListener listener) {
        this.statusListener.remove(listener);
    }

    protected void fireStatusChanged(MCRProcessableStatus oldStatus) {
        synchronized (this.statusListener) {
            this.statusListener.forEach(listener -> {
                listener.onStatusChange(this, oldStatus, getStatus());
            });
        }
    }

}
