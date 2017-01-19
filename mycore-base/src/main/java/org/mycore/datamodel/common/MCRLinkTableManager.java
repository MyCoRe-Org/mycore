/*
 * 
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

package org.mycore.datamodel.common;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class manage all accesses to the link table database. This database
 * holds all informations about links between MCRObjects/MCRClassifications.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRLinkTableManager {
    /** The list of entry types */
    public static final String ENTRY_TYPE_CHILD = "child";

    public static final String ENTRY_TYPE_DERIVATE = "derivate";

    public static final String ENTRY_TYPE_PARENT = "parent";

    public static final String ENTRY_TYPE_REFERENCE = "reference";

    /** The link table manager singleton */
    protected static MCRLinkTableManager singleton;

    // logger
    static Logger logger = LogManager.getLogger(MCRLinkTableManager.class.getName());

    private MCRLinkTableInterface persistenceclass = null;

    /**
     * Returns the link table manager singleton.
     * 
     * @return Returns a MCRLinkTableManager instance.
     */
    public static synchronized MCRLinkTableManager instance() {
        if (singleton == null) {
            singleton = new MCRLinkTableManager();
        }

        return singleton;
    }

    /**
     * The constructor of this class.
     */
    protected MCRLinkTableManager() {
        MCRConfiguration config = MCRConfiguration.instance();

        // Load the persistence class
        String persistclassname = config.getString("MCR.Persistence.LinkTable.Store.Class");

        Object obj = new Object();
        try {
            obj = Class.forName(persistclassname).newInstance();
        } catch (ClassNotFoundException e) {
            throw new MCRException(persistclassname + " ClassNotFoundException");
        } catch (IllegalAccessException e) {
            throw new MCRException(persistclassname + " IllegalAccessException");
        } catch (InstantiationException e) {
            throw new MCRException(persistclassname + " InstantiationException");
        }

        persistenceclass = (MCRLinkTableInterface) obj;
    }

    /**
     * This method check the type of link.
     * 
     * @param type
     * @return true if it is a defined type, else return false and send a
     *         warning to the logger.
     */
    private boolean checkType(String type) {
        if (type.equals(ENTRY_TYPE_CHILD) || type.equals(ENTRY_TYPE_DERIVATE) || type.equals(ENTRY_TYPE_PARENT)
                || type.equals(ENTRY_TYPE_REFERENCE)) {
            return true;
        }
        logger.warn("The value " + type + " is not a defined type for the link table.");
        return false;
    }

    /**
     * The method add a reference link pair.
     * 
     * @param from
     *            the source of the reference as MCRObjectID
     * @param to
     *            the target of the reference as MCRObjectID
     * @param type
     *            the type of the reference as String
     * @param attr
     *            the optional attribute of the reference as String
     */
    public void addReferenceLink(MCRObjectID from, MCRObjectID to, String type, String attr) {
        addReferenceLink(from.toString(), to.toString(), type, attr);
    }

    /**
     * The method add a reference link pair.
     * 
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     * @param attr
     *            the optional attribute of the reference as String
     */
    public void addReferenceLink(String from, String to, String type, String attr) {
        if (from == null || (from = from.trim()).length() == 0) {
            logger.warn("The from value of a reference link is false, the link was not added to the link table");
            return;
        }

        if (to == null || (to = to.trim()).length() == 0) {
            logger.warn("The to value of a reference link is false, the link was not added to the link table");
            return;
        }

        if (type == null || (type = type.trim()).length() == 0) {
            logger.warn("The type value of a reference link is false, the link was not added to the link table");
            return;
        }

        if (attr == null) {
            attr = "";
        }

        String sb = "Link in table " + type + " add for " + from + "<-->" + to + " with " + type + " and " + attr;
        logger.debug(sb.toString());

        try {
            persistenceclass.create(from, to, type, attr);
        } catch (Exception e) {
            logger.warn("An error occured while adding a dataset from the reference link table, adding not succesful.", e);
        }
    }

    /**
     * The method delete a reference link.
     * 
     * @param from
     *            the source of the reference as MCRObjectID
     */
    public void deleteReferenceLink(MCRObjectID from) {
        deleteReferenceLink(from.toString());
    }

    /**
     * The method delete a reference link.
     * 
     * @param from
     *            the source of the reference as String
     */
    public void deleteReferenceLink(String from) {
        if (from == null || (from = from.trim()).length() == 0) {
            logger.warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        try {
            persistenceclass.delete(from, null, null);
        } catch (Exception e) {
            logger.warn("An error occured while deleting a dataset from the" + from
                    + " reference link table, deleting could be not succesful.", e);
        }
    }

    /**
     * The method delete a reference link pair for the given type to the store.
     * 
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     */
    public void deleteReferenceLink(String from, String to, String type) {
        if (from == null || (from = from.trim()).length() == 0) {
            logger.warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }
        try {
            persistenceclass.delete(from, to, type);
        } catch (Exception e) {
            logger.warn("An error occured while deleting a dataset from the" + " reference link table, deleting is not succesful.", e);
        }
    }

    /**
     * The method count the reference links for a given target MCRobjectID.
     * 
     * @param to
     *            the object ID as MCRObjectID, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(MCRObjectID to) {
        return countReferenceLinkTo(to.toString());
    }

    /**
     * The method count the reference links for a given target object ID.
     * 
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(String to) {
        if (to == null || (to = to.trim()).length() == 0) {
            logger.warn("The to value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        try {
            return persistenceclass.countTo(null, to, null, null);
        } catch (Exception e) {
            logger.warn("An error occured while searching for references of " + to + ".", e);
        }

        return 0;
    }

    /**
     * counts the reference links for a given to object ID.
     * 
     * @param types
     *            Array of document type slected by the mcrfrom content
     * @param restriction
     *            a first part of the to ID as String, it can be null
     * @return the number of references
     */
    public int countReferenceLinkTo(String to, String[] types, String restriction) {
        if (to == null || (to = to.trim()).length() == 0) {
            logger.warn("The to value of a reference link is false, the link was " + "not added to the link table");
            return 0;
        }

        try {
            if (types != null && types.length > 0) {
                String mydoctype = "";
                int cnt = 0;
                int idt = 0;

                for (idt = 0; idt < types.length; idt++) {
                    mydoctype = types[idt];
                    cnt += persistenceclass.countTo(null, to, mydoctype, restriction);
                }

                return cnt;
            }
            return persistenceclass.countTo(null, to, null, restriction);
        } catch (Exception e) {
            logger.warn("An error occured while searching for references of " + to + ".", e);
        }

        return 0;
    }

    /**
     * The method count the number of references to a category of a
     * classification without sub ID's and returns it as a Map
     * 
     * @param classid
     *            the classification ID as MCRObjectID
     * 
     * @return a Map with key=categID and value=counted number of references
     */
    public Map<String, Number> countReferenceCategory(String classid) {
        return persistenceclass.getCountedMapOfMCRTO(classid);
    }

    /**
     * The method count the number of references to a category of a
     * classification.
     * 
     * @param classid
     *            the classification ID as String
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countReferenceCategory(String classid, String categid) {
        return countReferenceLinkTo(classid + "##" + categid, null, null);
    }

    /**
     * Returns a List of all link sources of <code>to</code>
     * 
     * @param to
     *            The MCRObjectID to referenced.
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(MCRObjectID to) {
        return getSourceOf(to.toString());
    }

    /**
     * Returns a List of all link sources of <code>to</code>
     * 
     * @param to
     *            The ID to referenced.
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(String to) {
        if (to == null || to.length() == 0) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }

        try {
            return persistenceclass.getSourcesOf(to, null);
        } catch (Exception e) {
            logger.warn("An error occured while searching for references to " + to + ".", e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            link reference type
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(MCRObjectID to, String type) {
        return getSourceOf(to.toString(), type);
    }

    /**
     * Returns a List of all link sources of <code>to</code> and a special
     * <code>type</code>
     * 
     * @param to
     *            Destination-ID
     * @param type
     *            link reference type
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getSourceOf(String to, String type) {
        if (to == null || to.length() == 0) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        if (type == null || type.length() == 0) {
            logger.warn("The type value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        checkType(type);

        try {
            return persistenceclass.getSourcesOf(to, type);
        } catch (Exception e) {
            logger.warn("An error occured while searching for references to " + to + " with " + type + ".", e);
            return Collections.emptyList();
        }
    }

    /**
     * The method return a list of all source ID's of the refernce target to
     * with the given type.
     * 
     * @param to
     *            the refernce target to
     * @param type
     *            type of the refernce
     * @return a list of ID's
     */
    public Collection<String> getSourceOf(String[] to, String type) {
        if (to == null || to.length == 0) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        LinkedList<String> ll = new LinkedList<String>();
        try {
            for (String singleTo : to) {
                ll.addAll(persistenceclass.getSourcesOf(singleTo, type));
            }
            return ll;
        } catch (Exception e) {
            logger.warn("An error occured while searching for references to " + to + ".", e);
            return ll;
        }
    }

    /**
     * Returns a List of all link destinations of <code>from</code> and a
     * special <code>type</code>
     * 
     * @param from
     *            Destination-ID
     * @param type
     *            link reference type
     * @return List of Strings (Source-IDs)
     */
    public Collection<String> getDestinationOf(MCRObjectID from, String type) {
        return getDestinationOf(from.toString(), type);
    }

    /**
     * Returns a List of all link destination of <code>from</code> and a
     * special <code>type</code>
     * 
     * @param from
     *            Source-ID
     * @param type
     *            Link reference type, this can be null. Current types are
     *            classid, child, parent, reference and derivate.
     * @return List of Strings (Destination-IDs)
     */
    public Collection<String> getDestinationOf(String from, String type) {
        if (from == null || from.length() == 0) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        if (type == null || type.length() == 0) {
            logger.warn("The type value of a reference link is false, the link was not found in the link table");
            return Collections.emptyList();
        }
        checkType(type);

        try {
            return persistenceclass.getDestinationsOf(from, type);
        } catch (Exception e) {
            logger.warn("An error occured while searching for references from " + from + ".", e);
            return Collections.emptyList();
        }
    }

}
