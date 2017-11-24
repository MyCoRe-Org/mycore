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
