package org.mycore.services.queuedjob;

/**
 * Possible states of the job can be:
 * <dl>
 *   <dt>{@link #NEW}</dt>
 *   <dd>job added to queue</dd>
 *   <dt>{@link #PROCESSING}</dt>
 *   <dd>job currently on processing</dd>
 *   <dt>{@link #FINISHED}</dt>
 *   <dd>job processing is finished</dd>
 * </dl>  
 */
public enum MCRJobStatus {
    NEW, PROCESSING, FINISHED
}
