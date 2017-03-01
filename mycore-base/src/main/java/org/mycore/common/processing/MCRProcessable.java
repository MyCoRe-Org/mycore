package org.mycore.common.processing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Describes an object which can be processed. A processable has a
 * name, a user id, a status, a create-, start- and end time and is 
 * {@link MCRProgressable}.
 * 
 * @author Matthas Eichner
 */
public interface MCRProcessable extends MCRListenableProgressable {

    /**
     * Returns a human readable name.
     * 
     * @return name of this process
     */
    String getName();

    /**
     * Returns the id of the user who created this processable.
     * 
     * @return the user id responsible for the processable
     */
    String getUserId();

    /**
     * The status of this process.
     * 
     * @return the status
     */
    MCRProcessableStatus getStatus();

    /**
     * Returns true if this task was created but not started yet.
     * 
     * @return true if this task is just created.
     */
    default boolean isCreated() {
        return MCRProcessableStatus.created.equals(getStatus());
    }

    /**
     * Returns true if this task is currently processing.
     * 
     * @return true if this task is processing
     */
    default boolean isProcessing() {
        return MCRProcessableStatus.processing.equals(getStatus());
    }

    /**
     * Returns true if this task was cancelled before it completed normally.
     * 
     * @return true if this task is cancelled
     */
    default boolean isCanceled() {
        return MCRProcessableStatus.canceled.equals(getStatus());
    }

    /**
     * Returns true if this task failed before it completed normally. One can assume
     * that {@link #getError()} does not return null in such case.
     * 
     * @return true if the process failed
     */
    default boolean isFailed() {
        return MCRProcessableStatus.failed.equals(getStatus());
    }

    /**
     * Returns true if this task was successful and completed normally.
     * 
     * @return true if this task was successful
     */
    default boolean isSuccessful() {
        return MCRProcessableStatus.successful.equals(getStatus());
    }

    /**
     * Returns true if this processable completed. Completion may be due to normal termination,
     * an exception, or cancellation -- in all of these cases, this method will return true.
     * 
     * @return true if this processable completed
     */
    default boolean isDone() {
        return isCanceled() || isFailed() || isSuccessful();
    }

    /**
     * Returns the error if the processable failed. This will return
     * null if the status != failed.
     * 
     * @return the error occurred while processing
     */
    Throwable getError();

    /**
     * Time (instant) the process was started. Returns null if the process was not started yet.
     * 
     * @return the time the process was started
     */
    Instant getStartTime();

    /**
     * Time (instant) this processable was created.
     * 
     * @return the time the processable was created
     */
    Instant getCreateTime();

    /**
     * Time (instant) this processable finished. Either successfully, canceled
     * or with an error. If the processable is not finished this will return
     * null.
     * 
     * @return the time the processable finished
     */
    Instant getEndTime();

    /**
     * Calculates the duration between starting and finishing the processable.
     * 
     * @return the duration or null if the processable is not finished yet
     */
    default Duration took() {
        if (getStartTime() == null || getEndTime() == null) {
            return null;
        }
        return Duration.between(getStartTime(), getEndTime());
    }

    /**
     * Returns a map of properties assigned to this processable.
     * 
     * @return the properties map
     */
    Map<String, Object> getProperties();

    /**
     * A shortcut for getProperties().get(name).
     * 
     * @param name the name of the property
     * @return the property value or null
     */
    default Object getProperty(String name) {
        return getProperties().get(name);
    }

    /**
     * Returns the property for the given name. The property
     * will be cast to the specified type. Be aware that a
     * ClassCastException is thrown if the type does not match.
     * 
     * @param name name of property
     * @param type object type of the property
     * @return the property value or null
     */
    @SuppressWarnings("unchecked")
    default <T> T getPropertyAs(String name, Class<T> type) {
        Object property = getProperty(name);
        if (property == null) {
            return null;
        }
        return (T) property;
    }

    /**
     * Adds a new {@link MCRProcessableStatusListener} to this {@link MCRProcessable}.
     * 
     * @param listener the listener to add
     */
    void addStatusListener(MCRProcessableStatusListener listener);

    /**
     * Removes a {@link MCRProcessableStatusListener} from this {@link MCRProcessable}.
     * 
     * @param listener the listener to remove
     */
    void removeStatusListener(MCRProcessableStatusListener listener);

}
