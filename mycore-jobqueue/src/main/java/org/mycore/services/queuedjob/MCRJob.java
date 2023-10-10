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

package org.mycore.services.queuedjob;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Container class handled by hibernate to store and retrieve job information.
 * 
 * @author René Adler
 */
@Entity
@NamedQueries({
    @NamedQuery(name = "mcrjob.classes",
        query = "select DISTINCT(o.action) from MCRJob o") })
@Table(name = "MCRJob")
public class MCRJob implements Cloneable {
    private Long id;

    private Class<? extends MCRJobAction> action;

    private MCRJobStatus status;

    private Date added;

    private Date start;

    private Date finished;

    private Integer tries;

    private String exception;

    private Map<String, String> parameters;

    /**
     * The maximum length of the exception message, which is stored in the database.
     */
    public static final int EXCEPTION_MAX_LENGTH = 9999;

    /**
     * Creates an empty job.
     */
    protected MCRJob() {
    }

    /**
     * Creates an empty job for the given action class.
     * @param actionClass the action class
     */
    public MCRJob(Class<? extends MCRJobAction> actionClass) {
        action = actionClass;
    }

    /**
     * Returns the job Id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long getId() {
        return id;
    }

    /**
     * Set the job Id.
     * 
     * @param id - the job id
     */
    protected void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the action class ({@link MCRJobAction}).
     */
    @Column(name = "action", nullable = false)
    @Convert(converter = MCRJobActionConverter.class)
    public Class<? extends MCRJobAction> getAction() {
        return action;
    }

    /**
     * Set the action class ({@link MCRJobAction}).
     * 
     * @param actionClass - the action class to set
     */
    public void setAction(Class<? extends MCRJobAction> actionClass) {
        this.action = actionClass;
    }

    /**
     * Returns the current state ({@link MCRJobStatus}) of the job.
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public MCRJobStatus getStatus() {
        return status;
    }

    /**
     * Set the state ({@link MCRJobStatus}) of the job.
     * 
     * @param status - the job status
     */
    public void setStatus(MCRJobStatus status) {
        this.status = status;
    }

    /**
     * Returns the adding date of job.
     */
    @Column(name = "added")
    public Date getAdded() {
        return added;
    }

    /**
     * Set the adding date.
     * @param added the date when the job was added to the queue
     */
    public void setAdded(Date added) {
        this.added = added;
    }

    /**
     * Returns the starting date of execution.
     */
    @Column(name = "start")
    public Date getStart() {
        return start;
    }

    /**
     * Set the job starting date.
     * 
     * @param start - the starting date
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * Returns the finishing date of execution.
     */
    @Column(name = "finished")
    public Date getFinished() {
        return finished;
    }

    /**
     * Set the finishing date of execution.
     * 
     * @param finished - the finishing date
     */
    public void setFinished(Date finished) {
        this.finished = finished;
    }

    /**
     * Returns the number of retries.
    
     */
    @Column(name = "tries")
    public Integer getTries() {
        return tries;
    }

    /**
     * Set the number of retries.
     * @param retries the number of retries
     */
    public void setTries(Integer retries) {
        this.tries = retries;
    }

    /**
     * Returns the exception message. Which was thrown during last execution.
     * @return the exception message or null if no exception was thrown
     */
    @Column(name = "exception", length = EXCEPTION_MAX_LENGTH)
    public String getException() {
        return exception;
    }

    /**
     * Set the exception message. Which was thrown during last execution.
     * @param exception the exception message
     */
    public void setException(String exception) {
        this.exception = exception;
    }

    /**
     * Returns all set parameters of the job.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MCRJobParameter", joinColumns = @JoinColumn(name = "jobID"))
    @MapKeyColumn(name = "paramKey", length = 128)
    @Column(name = "paramValue")
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Set all job parameters.
     * 
     * @param parameters - the job parameters
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns a single parameter by its name.
     * 
     * @param key - the parameter name.
     * @return the value of the parameter.
     */
    @Transient
    public String getParameter(String key) {
        if (parameters == null) {
            return null;
        }

        return parameters.get(key);
    }

    /**
     * Set a single parameter.
     * 
     * @param key - the parameter name
     * @param value - the parameter value
     */
    public void setParameter(String key, String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        parameters.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRJob clone() {
        MCRJob clone = new MCRJob();
        clone.setAction(getAction());
        clone.setAdded(getAdded());
        clone.setFinished(getFinished());
        clone.setId(getId());
        clone.setStart(getStart());
        clone.setStatus(getStatus());
        clone.setTries(getTries());
        clone.setException(getException());

        Map<String, String> map = new HashMap<>(getParameters());
        clone.setParameters(map);

        return clone;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new MessageFormat("MCRJob [id:{0}, action:{1}, status:{2}, added:{3}, parameters:{4}]", Locale.ROOT)
            .format(
                new Object[] { getId(), getAction().getName(), getStatus(), getAdded(), getParameters() });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MCRJob mcrJob = (MCRJob) o;
        return Objects.equals(getId(), mcrJob.getId()) && getAction().equals(mcrJob.getAction())
            && getStatus() == mcrJob.getStatus()
            && Objects.equals(getAdded(), mcrJob.getAdded()) && Objects.equals(getStart(), mcrJob.getStart())
            && Objects.equals(getFinished(), mcrJob.getFinished()) && Objects.equals(getTries(), mcrJob.getTries())
            && Objects.equals(getException(), mcrJob.getException())
            && Objects.equals(getParameters(), mcrJob.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAction(), getStatus(), getAdded(), getStart(), getFinished(), getTries(),
            getException(), getParameters());
    }
}
