package org.mycore.common.processing;

import java.time.Duration;
import java.time.Instant;

/**
 * Describes an object which can be processed. A processable has a
 * name, a status, a create-, start- and end time and is 
 * {@link MCRProgressable}.
 * 
 * @author Matthas Eichner
 */
public interface MCRProcessable extends MCRProgressable {

    /**
     * Returns a human readable name.
     * 
     * @return name of this process
     */
    public String getName();

    /**
     * The status of this process.
     * 
     * @return the status
     */
    public MCRProcessableStatus getStatus();

    /**
     * Returns true if this task was created but not started yet.
     * 
     * @return true if this task is just created.
     */
    public default boolean isCreated() {
        return MCRProcessableStatus.created.equals(getStatus());
    }

    /**
     * Returns true if this task is currently processing.
     * 
     * @return true if this task is processing
     */
    public default boolean isProcessing() {
        return MCRProcessableStatus.processing.equals(getStatus());
    }

    /**
     * Returns true if this task was cancelled before it completed normally.
     * 
     * @return true if this task is cancelled
     */
    public default boolean isCanceled() {
        return MCRProcessableStatus.canceled.equals(getStatus());
    }

    /**
     * Returns true if this task failed before it completed normally. One can assume
     * that {@link #getError()} does not return null in such case.
     * 
     * @return true if the process failed
     */
    public default boolean isFailed() {
        return MCRProcessableStatus.failed.equals(getStatus());
    }

    /**
     * Returns true if this task was successful and completed normally.
     * 
     * @return true if this task was successful
     */
    public default boolean isSuccessful() {
        return MCRProcessableStatus.successful.equals(getStatus());
    }

    /**
     * Returns true if this processable completed. Completion may be due to normal termination,
     * an exception, or cancellation -- in all of these cases, this method will return true.
     * 
     * @return true if this processable completed
     */
    public default boolean isDone() {
        return isCanceled() || isFailed() || isSuccessful();
    }

    /**
     * Returns the error if the processable failed. This will return
     * null if the status != failed.
     * 
     * @return the error occurred while processing
     */
    public Throwable getError();

    /**
     * Time (instant) the process was started (the {@link #call()} was
     * executed). Returns null if the process was not started yet.
     * 
     * @return the time the process was started
     */
    public Instant getStartTime();

    /**
     * Time (instant) this processable was created.
     * 
     * @return the time the processable was created
     */
    public Instant getCreateTime();

    /**
     * Time (instant) this processable finished. Either successfully, canceled
     * or with an error. If the processable is not finished this will return
     * null.
     * 
     * @return the time the processable finished
     */
    public Instant getEndTime();

    /**
     * Calculates the duration between starting and finishing the processable.
     * 
     * @return the duration or null if the processable is not finished yet
     */
    public default Duration took() {
        if (getStartTime() == null || getEndTime() == null) {
            return null;
        }
        return Duration.between(getStartTime(), getEndTime());
    }

    /**
     * Adds a new {@link MCRProcessableStatusListener} to this {@link MCRProcessable}.
     * 
     * @param listener the listener to add
     */
    public void addStatusListener(MCRProcessableStatusListener listener);

    /**
     * Removes a {@link MCRProcessableStatusListener} from this {@link MCRProcessable}.
     * 
     * @param listener the listener to remove
     */
    public void removeStatusListener(MCRProcessableStatusListener listener);

}
