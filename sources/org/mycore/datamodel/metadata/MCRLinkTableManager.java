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

package org.mycore.datamodel.metadata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;

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

    public static final String ENTRY_TYPE_CLASSID = "classid";

    public static final String ENTRY_TYPE_DERIVATE = "derivate";

    public static final String ENTRY_TYPE_PARTENT = "parent";

    public static final String ENTRY_TYPE_REFERNCE = "reference";

    /** The list of the table types */
    public static final String TYPE_CLASS = "class";

    public static final String TYPE_HREF = "href";

    public static final String[] LINK_TABLE_TYPES = { TYPE_CLASS, TYPE_HREF };

    /** The link table manager singleton */
    protected static MCRLinkTableManager singleton;

    // logger
    static Logger logger = Logger.getLogger(MCRLinkTableManager.class.getName());

    // the list of link table
    private String persistclassname = null;

    private ArrayList tablelist;

    /**
     * Returns the link table manager singleton.
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
        persistclassname = config.getString("MCR.linktable_store_class");

        Object obj = new Object();
        tablelist = new ArrayList();

        for (int i = 0; i < LINK_TABLE_TYPES.length; i++) {
            try {
                obj = Class.forName(persistclassname).newInstance();
            } catch (ClassNotFoundException e) {
                throw new MCRException(persistclassname + " ClassNotFoundException for " + LINK_TABLE_TYPES[i]);
            } catch (IllegalAccessException e) {
                throw new MCRException(persistclassname + " IllegalAccessException for " + LINK_TABLE_TYPES[i]);
            } catch (InstantiationException e) {
                throw new MCRException(persistclassname + " InstantiationException for " + LINK_TABLE_TYPES[i]);
            }

            try {
                ((MCRLinkTableInterface) obj).init(LINK_TABLE_TYPES[i]);
            } catch (Exception e) {
                e.printStackTrace();
                throw new MCRException(" UnknownException for " + LINK_TABLE_TYPES[i], e);
            }

            tablelist.add(obj);
        }
    }

    /**
     * The method check the type.
     * 
     * @param type
     *            the table type
     * @return true if the type is in the list, else return false
     */
    private final int checkType(String type) {
        if ((type == null) || ((type = type.trim()).length() == 0)) {
            return -1;
        }

        for (int i = 0; i < MCRLinkTableManager.LINK_TABLE_TYPES.length; i++) {
            if (type.equals(MCRLinkTableManager.LINK_TABLE_TYPES[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * The method add a reference link pair for the given type to the store.
     * 
     * @param table
     *            the table type
     * @param from
     *            the source of the reference as MCRObjectID
     * @param to
     *            the target of the reference as MCRObjectID
     * @param type
     *            the type of the reference as String
     */
    public void addReferenceLink(String table, MCRObjectID from, MCRObjectID to, String type) {
        addReferenceLink(table, from.getId(), to.getId(), type);
    }

    /**
     * The method add a reference link pair for the given type to the store.
     * 
     * @param table
     *            the table type
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     */
    public void addReferenceLink(String table, String from, String to, String type) {
        int i = checkType(table);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was " + "not added to the link table");
            return;
        }

        if ((from == null) || ((from = from.trim()).length() == 0)) {
            logger.warn("The from value of a reference link is false, the link was " + "not added to the link table");
            return;
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was " + "not added to the link table");
            return;
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            logger.warn("The type value of a reference link is false, the link was " + "not added to the link table");
            return;
        }

        StringBuffer sb = new StringBuffer().append("Link in table ").append(type).append(" add for ").append(from).append("<-->").append(to).append(" with ").append(type);
        logger.debug(sb.toString());

        try {
            ((MCRLinkTableInterface) tablelist.get(i)).create(from, to, type);
        } catch (Exception e) {
            logger.warn("An error was occured while add a dataset from the reference link table, add not succesful.");
        }
    }

    /**
     * The method delete a reference link pair for the given type to the store.
     * 
     * @param table
     *            the table type
     * @param from
     *            the source of the reference as MCRObjectID
     */
    public void deleteReferenceLink(String table, MCRObjectID from) {
        deleteReferenceLink(table, from.getId());
    }

    /**
     * The method delete a reference link pair for the given type to the store.
     * 
     * @param table
     *            the table type
     * @param from
     *            the source of the reference as String
     */
    public void deleteReferenceLink(String table, String from) {
        int i = checkType(table);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        if ((from == null) || ((from = from.trim()).length() == 0)) {
            logger.warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        try {
            ((MCRLinkTableInterface) tablelist.get(i)).delete(from);
        } catch (Exception e) {
            logger.warn("An error was occured while delete a dataset from the" + " reference link table, deleting is not succesful.");
        }
    }

    /**
     * The method delete a reference link pair for the given type to the store.
     * 
     * @param table
     *            the table type
     * @param from
     *            the source of the reference as String
     * @param to
     *            the target of the reference as String
     * @param type
     *            the type of the reference as String
     */
    public void deleteReferenceLink(String table, String from, String to, String type) {
        int i = checkType(table);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        if ((from == null) || ((from = from.trim()).length() == 0)) {
            logger.warn("The from value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        if ((type == null) || ((type = type.trim()).length() == 0)) {
            logger.warn("The type value of a reference link is false, the link was " + "not deleted from the link table");
            return;
        }

        try {
            ((MCRLinkTableInterface) tablelist.get(i)).delete(from, to, type);
        } catch (Exception e) {
            logger.warn("An error was occured while delete a dataset from the" + " reference link table, deleting is not succesful.");
        }
    }

    /**
     * The method add a classification link pair for the given type to the
     * store.
     * 
     * @param from
     *            the source of the reference as MCRObjectID
     * @param classid
     *            the target classification id as MCRObjectID
     * @param categid
     *            the target category id ad String
     */
    public void addClassificationLink(MCRObjectID from, MCRObjectID classid, String categid) {
        addClassificationLink(from.getId(), classid.getId(), categid);
    }

    /**
     * The method add a classification link pair for the given type to the
     * store.
     * 
     * @param from
     *            the source of the reference as String
     * @param classid
     *            the target classification id as String
     * @param categid
     *            the target category id ad String
     */
    public void addClassificationLink(String from, String classid, String categid) {
        addReferenceLink(TYPE_CLASS, from, classid + "##" + categid, ENTRY_TYPE_CLASSID);
        MCRClassificationItem classitem = MCRClassificationItem.getClassificationItem(classid);
        MCRCategoryItem categitem = classitem.getCategoryItem(categid);
        if (categitem.getParent() != null) {
            addClassificationLink(from, classid, categitem.getParentID());
        }
    }

    /**
     * The method delete a classification link pair for the given type to the
     * store.
     * 
     * @param from
     *            the source of the reference as MCRObjectID
     */
    public void deleteClassificationLink(MCRObjectID from) {
        deleteClassificationLink(from.getId());
    }

    /**
     * The method delete a classification link pair for the given type to the
     * store.
     * 
     * @param from
     *            the source of the reference as String
     */
    public void deleteClassificationLink(String from) {
        deleteReferenceLink(TYPE_CLASS, from);
    }

    /**
     * removes a link from an object to a category and it's ancestors.
     * 
     * WARNING: This could result in an inconsistent system if you don't keep
     * track of links in the childlist.
     * 
     * @param from
     *            the source of the reference as MCRObjectID
     * @param classid
     *            target Classification ID
     * @param categid
     *            target Category ID in Classification classid
     */
    public void deleteClassificationLink(String from, String classid, String categid) {
        deleteReferenceLink(TYPE_CLASS, from, classid + "##" + categid, ENTRY_TYPE_CLASSID);
        MCRClassificationItem classitem = MCRClassificationItem.getClassificationItem(classid);
        MCRCategoryItem categitem = classitem.getCategoryItem(categid);
        if (categitem.getParent() != null) {
            deleteClassificationLink(from, classid, categitem.getParentID());
        }
    }

    /**
     * The method coutn the reference links for a given object ID.
     * 
     * @param type
     *            the table type
     * @param to
     *            the object ID as String, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(String type, String to) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        if ((to == null) || ((to = to.trim()).length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        try {
            return ((MCRLinkTableInterface) tablelist.get(i)).countTo(to);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references of " + to + ".");
        }

        return 0;
    }

    /**
     * counts the reference links for a given object ID.
     * 
     * @param type
     *            the table type
     * @param target
     *            the reference link target object ID as String
     * @return the number of references
     */
    public int countReferenceLinkTo(String type, String target, String[] doctypes, String restriction) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        if ((target == null) || ((target = target.trim()).length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was " + "not added to the link table");

            return 0;
        }

        try {
            if (((doctypes != null) && (doctypes.length > 0))) {
                String mydoctype = "";
                int cnt = 0;
                int idt = 0;

                for (idt = 0; idt < doctypes.length; idt++) {
                    mydoctype = doctypes[idt];
                    cnt += ((MCRLinkTableInterface) tablelist.get(i)).countTo(target, mydoctype, restriction);
                }

                return cnt;
            }
            return ((MCRLinkTableInterface) tablelist.get(i)).countTo(target, "", restriction);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references of " + target + ".");
        }

        return 0;
    }

    /**
     * The method coutn the reference links for a given object ID.
     * 
     * @param type
     *            the table type
     * @param to
     *            the object ID as MCRObjectID, they was referenced
     * @return the number of references
     */
    public int countReferenceLinkTo(String type, MCRObjectID to) {
        return countReferenceLinkTo(type, to.getId());
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
    public Map countCategoryReferencesSharp(String classid) {
        int i = checkType(TYPE_CLASS);
        return ((MCRLinkTableInterface) tablelist.get(i)).getCountedMapOfMCRTO(classid);
    }

    /**
     * The method count the number of references to a category of a
     * classification without sub ID's.
     * 
     * @param classid
     *            the classification ID as MCRObjectID
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countCategoryReferencesSharp(MCRObjectID classid, String categid) {
        return countReferenceLinkTo(TYPE_CLASS, classid.getId() + "##" + categid);
    }

    /**
     * The method count the number of references to a category of a
     * classification without sub ID's.
     * 
     * @param classid
     *            the classification ID as String
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countCategoryReferencesSharp(String classid, String categid) {
        return countReferenceLinkTo(TYPE_CLASS, classid + "##" + categid);
    }

    /**
     * The method count the number of references to a category of a
     * classification including sub ID's.
     * 
     * @param classid
     *            the classification ID as MCRObjectID
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countCategoryReferencesFuzzy(MCRObjectID classid, String categid) {
        return countReferenceLinkTo(TYPE_CLASS, classid.getId() + "##" + categid + "%");
    }

    /**
     * The method count the number of references to a category of a
     * classification including sub ID's.
     * 
     * @param classid
     *            the classification ID as String
     * @param categid
     *            the category ID as String
     * @return the number of references
     */
    public int countCategoryReferencesFuzzy(String classid, String categid) {
        return countReferenceLinkTo(TYPE_CLASS, classid + "##" + categid + "%");
    }

    /**
     * The method count the number of references to a category of a
     * classification including sub ID's.
     * 
     * @param classid
     *            the classification ID as String
     * @param categid
     *            the category ID as String
     * @param doctypes
     *            Array of document type slected by the mcrfrom content
     * @return the number of references
     */
    public int countCategoryReferencesFuzzy(String classid, String categid, String[] doctypes, String restriction) {
        return countReferenceLinkTo(TYPE_CLASS, classid + "##" + categid + "%", doctypes, restriction);
    }

    public List getSourceOf(String type, String destination) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was found in the link table");

            return new LinkedList();
        }

        if ((destination == null) || (destination.length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");

            return new LinkedList();
        }

        try {
            return ((MCRLinkTableInterface) tablelist.get(i)).getSourcesOf(destination);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references to " + destination + ".");
            return new LinkedList();
        }
    }

    public List getSourceOf(String type, String[] destinations) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was found in the link table");

            return new LinkedList();
        }

        if ((destinations == null) || (destinations.length == 0)) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");

            return new LinkedList();
        }

        try {
            return ((MCRLinkTableInterface) tablelist.get(i)).getSourcesOf(destinations);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references to " + destinations + ".");
            return new LinkedList();
        }
    }

    public List getDestinationOf(String type, String source) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was found in the link table");

            return new LinkedList();
        }

        if ((source == null) || (source.length() == 0)) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");

            return new LinkedList();
        }

        try {
            return ((MCRLinkTableInterface) tablelist.get(i)).getDestinationsOf(source);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references from " + source + ".");
            return new LinkedList();
        }
    }

    public List getDestinationOf(String type, String[] sources) {
        int i = checkType(type);

        if (i == -1) {
            logger.warn("The type value of a reference link is false, the link was found in the link table");

            return new LinkedList();
        }

        if ((sources == null) || (sources.length == 0)) {
            logger.warn("The to value of a reference link is false, the link was not found in the link table");

            return new LinkedList();
        }

        try {
            return ((MCRLinkTableInterface) tablelist.get(i)).getDestinationsOf(sources);
        } catch (Exception e) {
            logger.warn("An error was occured while search for references from " + sources + ".");
            return new LinkedList();
        }
    }

    public List getLinksToCategory(String classid, String categid) {
        return getSourceOf(TYPE_CLASS, classid + "##" + categid);
    }

    public List getFirstLinksToCategory(String classid, String categid) {
        MCRClassificationItem classification = MCRClassificationItem.getClassificationItem(classid);
        MCRCategoryItem categ = classification.getCategoryItem(categid);
        List categList = getLinksToCategory(classid, categid);
        MCRCategoryItem[] childs = categ.getChildren();
        String[] childIDs = new String[childs.length];
        for (int i = 0; i < childIDs.length; i++) {
            childIDs[i] = new StringBuffer(classid).append("##").append(childs[i].getClassificationID()).toString();
        }
        List childrenList = getSourceOf(TYPE_CLASS, childIDs);
        categList.removeAll(childrenList);
        return categList;
    }

}
