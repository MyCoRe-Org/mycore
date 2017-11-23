package org.mycore.datamodel.common;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mycore.common.MCRCatchException;

/**
 * This exception holds information about a link condition that did not allow a
 * certain action to be performed.
 * 
 * As this exception does not extend RuntimeException it has to be caught for
 * data integrity reasons.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRActiveLinkException extends MCRCatchException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    Map<String, Collection<String>> linkTable = new ConcurrentHashMap<>();

    /**
     * 
     * @return a Hashtable with destinations (key) and List of sources (value)
     */
    public Map<String, Collection<String>> getActiveLinks() {
        return linkTable;
    }

    /**
     * collects information on active links that do not permit a certain action
     * on the repository.
     * 
     * @param source
     *            the source of a link
     * @param dest
     *            the destination of a link
     */
    public void addLink(String source, String dest) {
        if (!linkTable.containsKey(dest)) {
            linkTable.put(dest, new ConcurrentLinkedQueue<>());
        }
        linkTable.get(dest).add(source);
    }

    /**
     * @see MCRCatchException#MCRCatchException(String)
     */
    public MCRActiveLinkException(String message) {
        super(message);
    }

    /**
     * @see MCRCatchException#MCRCatchException(String, Throwable)
     */
    public MCRActiveLinkException(String message, Throwable cause) {
        super(message, cause);
    }

}
