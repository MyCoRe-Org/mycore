package org.mycore.util.concurrent;

import java.time.Instant;

/**
 * Objects can implement this interface if they are capable of being prioritized.
 *
 * @author Matthias Eichner
 */
public interface MCRPrioritizable extends Comparable<MCRPrioritizable> {

    /**
     * Returns the priority.
     */
    public int getPriority();

    public Instant getCreated();

    @Override
    default int compareTo(MCRPrioritizable o) {
        if (o == null) {
            return -1;
        }
        if (o.getPriority() == getPriority()) {
            return getCreated().compareTo(o.getCreated());
        }
        return Integer.compare(o.getPriority(), getPriority());
    }

}
