package org.mycore.common.processing;

/**
 * The status of one {@link MCRProcessable}. Can be one of created,
 * processing, canceled, failed or successful.
 * 
 * @author Matthias Eichner
 */
public enum MCRProcessableStatus {

    /**
     * The process is created and not started yet.
     */
    created,

    /**
     * The process is currently running.
     */
    processing,

    /**
     * Canceled by the user and not by an error.
     */
    canceled,

    /**
     * An exception/error occurred while processing.
     */
    failed,

    /**
     * The process is successfully done.
     */
    successful

}
