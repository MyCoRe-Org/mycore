package org.mycore.services.queuedjob;

import java.util.concurrent.ExecutionException;

/**
 * <code>MCRJobAction</code> must be extended to do some work for given {@link MCRJob}.
 * 
 * @author Ren\u00E9 Adler
 *
 */
public abstract class MCRJobAction {
    protected MCRJob job;

    /**
     * The constructor of the job action.
     */
    public MCRJobAction() {
    }

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     */
    public MCRJobAction(MCRJob job) {
        this.job = job;
    }

    /**
     * Returns if this action is activated.
     * 
     * @return <code>true</code> if activated, <code>false</code> if isn't
     */
    public abstract boolean isActivated();

    /**
     * Returns the name of the action.
     * 
     * @return the name
     */
    public abstract String name();

    /**
     * Does the work for given {@link MCRJob}.
     */
    public abstract void execute() throws ExecutionException;

    /**
     * When errors occurs during executing it can be necessary to rollback
     * performed actions
     */
    public abstract void rollback();
}
