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

import java.util.ArrayList;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;

/**
 * This class defines the set of privileges of the MyCoRe user management. It is
 * implemented as a singleton since there must not be two instances of this
 * class.
 * 
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRPrivilegeSet {
    /** This vector holds all privileges of the org.mycore.user management system */
    private ArrayList privileges = null;

    /** the class responsible for persistent datastore (configurable ) */
    private MCRUserStore mcrUserStore;

    /** The one and only instance of this class */
    private static MCRPrivilegeSet theInstance = null;

    /** private constructor to create the singleton instance. */
    private MCRPrivilegeSet() throws MCRException {
        ArrayList privs = new ArrayList();

        // Get the user store name
        try {
            String userStoreName = MCRConfiguration.instance().getString("MCR.userstore_class_name");
            mcrUserStore = (MCRUserStore) Class.forName(userStoreName).newInstance();
            privs = mcrUserStore.retrievePrivilegeSet();
        } catch (Exception e) {
            throw new MCRException("Can't instancied MCRPrivilegeSet.");
        }

        if (!(privs == null)) { // There already exists a privilege set
            this.privileges = privs;
        }
    }

    /**
     * This method is the only way to get an instance of this class. It calls
     * the private constructor to create the singleton.
     * 
     * @return returns the one and only instance of <CODE>MCRPrivilegeSet
     *         </CODE>
     */
    public final static synchronized MCRPrivilegeSet instance() throws MCRException {
        if (theInstance == null) {
            theInstance = new MCRPrivilegeSet();
        }

        return theInstance;
    }

    /**
     * This method takes a ArrayList of privileges as parameter and creates
     * resp. updates them in the persistent datastore.
     * 
     * @param privList
     *            ArrayList containing privilege objects
     */
    public final void loadPrivileges(ArrayList privList) throws MCRException {
        privList.add(new MCRPrivilege("user administrator", "Users with this privilege has administrator rights in the system."));
        this.privileges = removeDouble(privList);

        if (mcrUserStore.existsPrivilegeSet()) {
            mcrUserStore.updatePrivilegeSet(this);
        } else {
            mcrUserStore.createPrivilegeSet(this);
        }
    }

    /**
     * The method return the privileg set without doubles.
     */
    private final ArrayList removeDouble(ArrayList privList) {
        ArrayList n = new ArrayList();

        for (int i = 0; i < privList.size(); i++) {
            boolean test = false;
            String name = ((MCRPrivilege) privList.get(i)).getName();

            for (int j = 0; j < n.size(); j++) {
                if (name.equals(((MCRPrivilege) n.get(j)).getName())) {
                    test = true;
                }
            }

            if (!test) {
                n.add(privList.get(i));
            }
        }

        return n;
    }

    /**
     * @returns This method returns a ArrayList of strings containing all names
     *          of the privileges of the system.
     */
    public final ArrayList getPrivileges() {
        return privileges;
    }

    /**
     * @return This method returns the privilege set object as a DOM document.
     */
    public synchronized org.jdom.Document toJDOMDocument() throws MCRException {
        // Build the DOM
        org.jdom.Element root = new org.jdom.Element("mycoreprivilege");
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        root.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xlink", MCRDefaults.XLINK_URL));
        root.setAttribute("noNamespaceSchemaLocation", "MCRPrivilege.xsd", org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

        for (int i = 0; i < privileges.size(); i++) {
            MCRPrivilege currentPriv = (MCRPrivilege) privileges.get(i);
            root.addContent(currentPriv.toJDOMElement());
        }

        org.jdom.Document jdomDoc = new org.jdom.Document(root);

        return jdomDoc;
    }

}
