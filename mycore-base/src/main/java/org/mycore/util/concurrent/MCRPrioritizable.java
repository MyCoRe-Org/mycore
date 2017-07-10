package org.mycore.util.concurrent;

/**
 * Objects can implement this interface if they are capable of being prioritized.
 *
 * @author Matthias Eichner
 */
public interface MCRPrioritizable extends Comparable<MCRPrioritizable> {

    /**
     * Returns the priority.
     */
    public Integer getPriority();

    @Override
    default int compareTo(MCRPrioritizable o) {
        if (o == null) {
            return -1;
        }
        return o.getPriority().compareTo(getPriority());
    }

}
