/**
 * $RCSfile$
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
package org.mycore.datamodel.metadata;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    Hashtable linkTable = new Hashtable();

    /**
     * 
     * @return a Hashtable with destinations (key) and List of sources (value)
     */
    public Map getActiveLinks() {
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
            linkTable.put(dest, new LinkedList());
        }
        ((List) linkTable.get(dest)).add(source);
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
