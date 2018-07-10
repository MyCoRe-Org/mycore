package org.mycore.util.concurrent;

import java.util.Comparator;

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

    long getCreated();

    @Override
    default int compareTo(MCRPrioritizable o) {
        return Comparator.nullsLast(
            Comparator.comparingInt(MCRPrioritizable::getPriority)
                .reversed()
                .thenComparingLong(MCRPrioritizable::getCreated))
            .compare(this, o);
    }

}
