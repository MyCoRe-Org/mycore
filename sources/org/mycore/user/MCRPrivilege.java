/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user;

import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;

/**
 * This class defines a privilege of the MyCoRe user management system.
 * Privileges must not be confused with access control lists (ACL) which control
 * if some principal (user, host, subnet etc) is allowed to access data in the
 * system. Privileges are used to define roles in the user management component
 * of the mycore system.
 * 
 * @author Detlev Degenhardt
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @deprecated
 */
public class MCRPrivilege {
    /** The length of the decription */
    public static final int description_len = MCRUserObject.description_len;

    /** The length of the privilege */
    public static final int privilege_len = MCRUserObject.privilege_len;

    /** The name of the privilege */
    protected String privName;

    /** The description of the privilege */
    protected String privDescription;

    /**
     * constructor
     * 
     * @param name
     *            The name of the privilege
     * @param description
     *            The description of the privilege
     */
    public MCRPrivilege(String name, String description) {
        privName = MCRUserObject.trim(name, privilege_len);
        privDescription = MCRUserObject.trim(description, description_len);
    }

    /**
     * copy constructor
     */
    public MCRPrivilege(MCRPrivilege priv) {
        this.privName = priv.privName;
        this.privDescription = priv.privDescription;
    }

    /**
     * constructor
     * 
     * @param priv
     *            the jdom.element representation of the privilege
     */
    public MCRPrivilege(org.jdom.Element priv) {
        privName = "";
        privDescription = "";

        if (!priv.getName().equals("privilege")) {
            return;
        }

        privName = MCRUserObject.trim(priv.getAttributeValue("name"), privilege_len);

        List listelm = priv.getChildren();

        for (int i = 0; i < listelm.size(); i++) {
            org.jdom.Element elm = (org.jdom.Element) listelm.get(i);

            if (!elm.getName().equals("privilege.description")) {
                return;
            }

            privDescription = MCRUserObject.trim(elm.getText(), description_len);
        }
    }

    /** @return returns the name of the privilege */
    public String getName() {
        return privName;
    }

    /** @return returns the description of the privilege */
    public String getDescription() {
        return privDescription;
    }

    /**
     * @return This method returns the privilege object as a JDOM element. This
     *         is needed if one wants to get a representation of several
     *         privileges in one xml document.
     */
    public org.jdom.Element toJDOMElement() throws MCRException {
        org.jdom.Element priv = new org.jdom.Element("privilege").setAttribute("name", privName);
        org.jdom.Element Description = new org.jdom.Element("privilege.description").setText(privDescription);

        // Aggregate privilege element
        priv.addContent(Description);

        return priv;
    }

    /**
     * The method check the validation of this class.
     * 
     * @return true if name is not null or empty, else return false
     */
    public boolean isValid() {
        return (privName.length() == 0) ? false : true;
    }

    /**
     * This method puts debug data to the logger (if it is set to debug mode).
     */
    public final void debug() {
        Logger logger = Logger.getLogger(MCRPrivilege.class.getName());
        logger.debug("privName           = " + privName);
        logger.debug("privDescription    = " + privDescription);
    }
}
