package org.mycore.common.processing;

/**
 * Interface to apply a progress to a process.
 * 
 * @author Matthias Eichner
 */
public interface MCRProgressable {

    /**
     * Returns a value between 0-100 which determines the progress.
     * Can return null if the process is not started yet.
     * 
     * @return the progress between 0-100 or null
     */
    public Integer getProgress();

    /**
     * Returns a human readable text indicating the state of the progress.
     * 
     * @return progress text
     */
    public String getProgressText();

}
