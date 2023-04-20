package org.mycore.services.queuedjob;

import java.util.List;
import java.util.Map;

public interface MCRJobDAO {

    /**
     * Retrieves jobs from some storage, matching the given parameters.
     * The jobs are ordered by their creation date, ascending.
     * @param action the action to filter for or null
     * @param params the parameters to filter for or null
     * @param status the status to filter for or null
     * @param maxResults the maximum number of results to return or null
     * @return the requested jobs
     */
    List<MCRJob> getJobs(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status,
        Integer maxResults,
        Integer offset);

    /**
     * Retrieves the count of jobs from some storage, matching the given parameters.
     * @param action the action to filter for or null
     * @param params the parameters to filter for or null
     * @param status the status to filter for or null
     * @return the requested jobs
     */
    int getJobCount(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status);

    /**
     * Removes jobs from some storage, matching the given parameters.
     * @param action the action to filter for or null
     * @param params the parameters to filter for or null
     * @param status the status to filter for or null
     * @return the number of removed jobs
     */
    int removeJobs(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status);

    /**
     * Returns a job matching the given parameter. 
     * @param action the action to filter for or null
     * @param params the parameters to filter for or null
     * @param status the status to filter for or null
     * @return the requested job or null
     * @throws IllegalArgumentException if more than one job matches the given parameters
     */
    MCRJob getJob(Class<? extends MCRJobAction> action,
        Map<String, String> params,
        List<MCRJobStatus> status);

    /**
     * Returns the next jobs to execute.
     * @param action the action to execute or null
     * @param amount the amount of jobs to return
     * @return the next jobs to execute
     */
    List<MCRJob> getNextJobs(Class<? extends MCRJobAction> action, Integer amount);

    /**
     * Returns the number of jobs which need to be executed.
     * @param action the action to execute
     * @return the number of jobs which need to be executed
     */
    int getRemainingJobCount(Class<? extends MCRJobAction> action);

    /**
     * Updates the given job in the storage.
     * @param job the job to update
     * @return true if the job was updated, false otherwise
     */
    boolean updateJob(MCRJob job);

    /**
     * Adds the given job to the storage.
     * @param job the job to add
     * @return true if the job was added, false otherwise
     */
    boolean addJob(MCRJob job);

    /**
     * Returns the actions which are present in the storage.
     * @return the actions which are present in the storage
     */
    List<? extends Class<? extends MCRJobAction>> getActions();

}
