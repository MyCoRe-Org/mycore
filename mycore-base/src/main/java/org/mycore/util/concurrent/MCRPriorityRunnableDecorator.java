package org.mycore.util.concurrent;

/**
 * Decorates a runnable with the capability of being compared by its priority.
 * 
 * @author Matthias Eichner
 */
public class MCRPriorityRunnableDecorator implements Runnable, Comparable<MCRPriorityRunnableDecorator> {

    protected Runnable delegate;
    
    protected int priority;

    /**
     * Creates a new priority runnable.
     * 
     * @param delegate runnable which should be delegated
     * @param priority priority of the task
     */
    public MCRPriorityRunnableDecorator(Runnable delegate, int priority) {
        this.delegate = delegate;
        this.priority = priority;
    }

    @Override
    public void run() {
        this.delegate.run();
    }

    public int compareTo(MCRPriorityRunnableDecorator o) {
        int diff = o.priority - this.priority;
        return Integer.signum(diff);
    }

    public int getPriority() {
        return priority;
    }

}
