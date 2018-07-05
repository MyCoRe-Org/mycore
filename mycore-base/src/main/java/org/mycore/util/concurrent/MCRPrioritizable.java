package org.mycore.util.concurrent;

import java.util.Comparator;

import org.apache.commons.collections.ComparatorUtils;

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
        return ComparatorUtils.chainedComparator(
            Comparator.comparingInt(MCRPrioritizable::getPriority),
            Comparator.comparingLong(MCRPrioritizable::getCreated))
            .compare(this,o);
    }

}
