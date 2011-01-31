/**
 * 
 */
package org.mycore.common.events;

import java.util.Comparator;

import org.mycore.common.events.MCRShutdownHandler.Closeable;

/**
 * @author shermann
 */
public class MCRCloseableComparator implements Comparator<MCRShutdownHandler.Closeable> {

    @Override
    public int compare(Closeable o1, Closeable o2) {
        return o1.getPriority() - o2.getPriority();
    }

}
