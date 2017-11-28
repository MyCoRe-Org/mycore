/*
* This file is part of *** M y C o R e ***
* See http://www.mycore.de/ for details.
*
* This program is free software; you can use it, redistribute it
* and / or modify it under the terms of the GNU General Public License
* (GPL) as published by the Free Software Foundation; either version 2
* of the License or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but
* WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program, in a file called gpl.txt or license.txt.
* If not, write to the Free Software Foundation Inc.,
* 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
*/

package org.mycore.orcid;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.jdom2.Namespace;

/**
 * Utility class to hold constants and namespace representation used in the XML of the ORCID API.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public abstract class MCRORCIDConstants {

    public final static MediaType ORCID_XML_MEDIA_TYPE = MediaType.valueOf("application/vnd.orcid+xml");

    public final static List<Namespace> NAMESPACES = new ArrayList<Namespace>();

    public final static Namespace NS_ACTIVITIES = buildNamespace("activities");

    public final static Namespace NS_WORK = buildNamespace("work");

    private static Namespace buildNamespace(String prefix) {
        Namespace namespace = Namespace.getNamespace(prefix, "http://www.orcid.org/ns/" + prefix);
        NAMESPACES.add(namespace);
        return namespace;
    }
}
