/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.queuedjob;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

/**
 * Container class handled by hibernate to store and retrieve job information.
 * 
 * @author Ren\u00E9 Adler
 */
@Entity
public class MCRJob implements Cloneable {
    /**
     * Possible states of the job can be:<br />
     * <table>
     *  <tr>
     *   <th>{@link #NEW}</th>
     *   <td>job added to queue</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #PROCESSING}</th>
     *   <td>job currently on processing</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #FINISHED}</th>
     *   <td>job processing is finished</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #BROKEN}</th>
     *   <td>an Exception occurred on execution</td>
     *  </tr>
     * </table>  
     */
    public enum Status {
        NEW, PROCESSING, FINISHED, BROKEN
    };

    private long id;

    private Class<? extends MCRJobAction> action;

    private Status status;

    private Date added;

    private Date start;

    private Date finished;

    private Map<String, String> parameters = new HashMap<String, String>();

    protected MCRJob() {
    }

    public MCRJob(Class<? extends MCRJobAction> actionClass) {
        action = actionClass;
    }

    /**
     * Returns the job Id.
     * 
     * @return the job Id
     */
    @Id
    @GeneratedValue
    @Column(name = "id")
    public long getId() {
        return id;
    }

    /**
     * Set the job Id.
     * 
     * @param id - the job id
     */
    protected void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the action class ({@link MCRJobAction}).
     * 
     * @return the action class
     */
    @Column(name = "action", nullable = false)
    public Class<? extends MCRJobAction> getAction() {
        return action;
    }

    /**
     * Set the action class ({@link MCRJobAction}).
     * 
     * @param action - the action class to set
     */
    public void setAction(Class<? extends MCRJobAction> actionClass) {
        this.action = actionClass;
    }

    /**
     * Returns the current state ({@link MCRJob.Status}) of the job.
     * 
     * @return
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public Status getStatus() {
        return status;
    }

    /**
     * Set the state ({@link MCRJob.Status}) of the job.
     * 
     * @param status - the job status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the adding date of job.
     * 
     * @return the add date of the job
     */
    @Column(name = "added", nullable = true)
    public Date getAdded() {
        return added;
    }

    /**
     * Set the adding date.
     * 
     * @param added
     */
    public void setAdded(Date added) {
        this.added = added;
    }

    /**
     * Returns the starting date of execution.
     *  
     * @return the job start date
     */
    @Column(name = "start", nullable = true)
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
     * 
     * @return the finishing date
     */
    @Column(name = "finished", nullable = true)
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
     * Returns all set parameters of the job.
     * 
     * @return the job parameters
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MCRJobParameter", joinColumns = @JoinColumn(name = "jobID"))
    @MapKeyColumn(name = "paramKey", length = 128)
    @Column(name = "paramValue", length = 255)
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
     * Returns an single parameter by it's name.
     * 
     * @param key - the parameter name.
     * @return the value of the parameter.
     */
    public String getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Set an single parameter.
     * 
     * @param key - the parameter name
     * @param value - the parameter value
     */
    public void setParameter(String key, String value) {
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
        clone.setParameters(getParameters());
        return clone;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return MessageFormat.format("MCRJob [id:{0}, action:{1}, status:{2}, added:{3}]", getId(), getAction().getName(), getStatus(),
                getAdded());
    }
}
