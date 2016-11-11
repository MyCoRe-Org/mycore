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
 * @param T the task
 * 
 * @author Matthias Eichner
 */
public abstract class MCRAbstractProcessable<T> extends MCRAbstractProgressable implements MCRProcessable {

    protected String name;

    protected MCRProcessableStatus status;

    protected Throwable error;

    protected Instant createTime;

    protected Instant startTime;

    protected Instant endTime;

    protected List<MCRProcessableStatusListener> statusListener;

    protected T task;

    public MCRAbstractProcessable(T task) {
        super();
        this.task = task;
        this.name = null;
        this.status = MCRProcessableStatus.created;
        this.error = null;

        this.createTime = Instant.now();
        this.startTime = null;
        this.endTime = null;

        this.statusListener = Collections.synchronizedList(new ArrayList<>());

        delegateProgressable();
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

    public T getTask() {
        return task;
    }

    /**
     * Sets the new status.
     * 
     * @param status the new status
     */
    public void setStatus(MCRProcessableStatus status) {
        MCRProcessableStatus oldStatus = this.status;
        this.status = status;
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

    protected void delegateProgressable() {
        if (this.task instanceof MCRListenableProgressable) {
            ((MCRListenableProgressable) this.task).addProgressListener(new MCRProgressableListener() {
                @Override
                public void onProgressTextChange(MCRProgressable source, String oldProgressText, String newProgressText) {
                    setProgressText(newProgressText);
                }

                @Override
                public void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress) {
                    setProgress(newProgress);
                }
            });
        }
    }

}
