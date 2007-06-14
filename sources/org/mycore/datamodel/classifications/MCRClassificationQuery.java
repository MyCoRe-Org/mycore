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

package org.mycore.datamodel.classifications;

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;

import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRLinkTableManager;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationQuery {

    private static final Logger LOGGER = Logger.getLogger(MCRClassificationQuery.class);

    /**
     * returns a classification as POJO.
     * 
     * @param ID
     *            MCR classification ID.
     * @param levels
     *            of category depth.
     * @return
     */
    public static MCRClassificationItem getClassification(String ID, int levels, boolean withCounter) {
        Document cl = MCRClassification.retrieveClassificationAsJDOM(ID, withCounter);
        return MCRClassificationTransformer.getClassification(cl, levels, withCounter);
    }

    /**
     * returns a classification as POJO. Only the given Category (and its
     * children to <code>levels</code> depth) is returned.
     * 
     * @param ID
     *            MCR classification ID.
     * @param categID
     *            MCR category ID.
     * @param levels
     *            of category depth.
     * @return
     */
    public static MCRClassificationItem getClassification(String classID, String categID, int levels, boolean withCounter) {
        LOGGER.debug("start ClassCategSearch");
        MCRClassificationItem returns = getClassification(classID, categID, withCounter);
        if (levels != 0) {
            LOGGER.debug("getCategoryItem");
            MCRCategoryItem catItem = MCRClassification.retrieveCategoryItem(classID, categID);
            // map of every categID with numberofObjects
            LOGGER.debug("countReferenceCategory");
            Map map = withCounter ? MCRLinkTableManager.instance().countReferenceCategory(classID) : null;
            LOGGER.debug("select category");
            MCRCategoryItem cat = returns.getCategories().get(0);
            LOGGER.debug("fillCategory");
            MCRClassificationTransformer.addChildren(cat, catItem, map, levels, withCounter);
            LOGGER.debug("finished ClassCategSearch");
        }
        return returns;
    }

    /**
     * returns a classification as POJO. Only the given Category, its ancestors
     * (and its children to <code>levels</code> depth) is returned.
     * 
     * @param ID
     *            MCR classification ID.
     * @param categID
     *            MCR category ID.
     * @param levels
     *            of category depth.
     * @return
     */
    public static MCRClassificationItem getClassificationHierarchie(String classID, String categID, int levels, boolean withCounter) {
        MCRCategoryItem catItem = MCRClassification.retrieveCategoryItem(classID, categID);
        MCRCategoryItem parent = (catItem.getParentID() != null) ? MCRClassification.retrieveCategoryItem(classID,catItem.getParentID()):null;
        LinkedList<MCRCategoryItem> list = new LinkedList<MCRCategoryItem>();
        list.add(0, catItem);
        while (parent != null) {
            // build the ancestor axis
            list.add(0, parent);
            if (parent.getParentID() == null) break;
            parent = MCRClassification.retrieveCategoryItem(classID,parent.getParentID());
        }
        return MCRClassificationTransformer.getClassification(catItem, list, levels, withCounter);
    }

    /**
     * returns a classification as POJO. Only the given Category is returned.
     * 
     * @param ID
     *            MCR classification ID.
     * @param categID
     *            MCR category ID.
     * @return
     */
    public static MCRClassificationItem getClassification(String classID, String categID, boolean withCounter) {
        LOGGER.debug("-receiveCategoryAsJDOM");
        Document doc = MCRClassificationQuery.retrieveCategoryAsJDOM(classID, categID, withCounter);
        LOGGER.debug("-getClassification");
        MCRClassificationItem returns = MCRClassificationTransformer.getClassification(doc, -1, withCounter);
        LOGGER.debug("-getClassification finished");
        return returns;
    }

    /**
     * returns <code>Category</code> with id <code>categID</code> in ClassificationObject <code>co</code>.
     * @param co
     * @param categID
     * @return <code>null</code> if no <code>Category</code> with id <code>categID</code> is present as a child of <code>co</code>.
     */
    public static MCRCategoryItem findCategory(MCRClassificationObject co, String categID) {
        if (co instanceof MCRCategoryItem && co.getId().equals(categID)) {
            return (MCRCategoryItem) co;
        }
        for (MCRCategoryItem category:co.getCategories()) {
            MCRCategoryItem returns = findCategory(category, categID);
            if (returns != null)
                return returns;
        }
        return null;
    }

    /**
     * returns parent <code>Category</code> of <code>category</code>.
     * @param classification Classification to search for parent in
     * @param category child category
     * @return <code>classification</code> if category has no parent category and is in <code>classification</code>,
     * parent <code>Category</code> or <code>null</code> if <code>category</code> is not in <code>classification</code>.
     */
    public static MCRClassificationObject findParent(MCRClassificationItem classification, MCRCategoryItem category) {
        return findParentInClassificationObject(classification, category);
    }

    private static MCRClassificationObject findParentInClassificationObject(MCRClassificationObject co, MCRCategoryItem cat){
        if (co.getCategories().contains(cat)){
            return co;
        }
        for (MCRCategoryItem category:co.getCategories()){
            if (findParentInClassificationObject(category, cat) != null){
                return category;
            }
        }
        return null;
    }

    /**
     * 
     * @author Thomas Scheffler (yagee)
     * 
     * This class provides some helper methods, that the main() method depend
     * on.
     * 
     */
    private static final class MainHelper {
        // TODO: After setting up JUnit persitence test remove this class

        private static void print(MCRClassificationObject c, int depth) {
            intend(depth);
            System.out.println("ID: " + c.getId());
            for (MCRCategoryItem category : c.getCategories()) {
                print(category, depth + 1);
            }
        }

        private static void intend(int a) {
            for (int i = 0; i < a; i++) {
                System.out.print(' ');
            }
        }

    }

    /**
     * The method return the category as XML byte array.
     * 
     * @param classID
     *            the classification ID
     * @param categID
     *            the category ID
     * @return the classification as XML
     */
    private static final org.jdom.Document retrieveCategoryAsJDOM(String classID, String categID, boolean withCounter) {
        MCRCategoryItem item = MCRCategoryItem.retrieveCategoryItem(classID, categID);
        org.jdom.Element elm = new org.jdom.Element("mycoreclass");
        org.jdom.Document doc = new org.jdom.Document(elm);
        elm.addNamespaceDeclaration(XSI_NAMESPACE);
        elm.addNamespaceDeclaration(XLINK_NAMESPACE);
        elm.setAttribute("ID", classID);
        org.jdom.Element cats = new org.jdom.Element("categories");
        elm.addContent(cats);
        org.jdom.Element cat = MCRClassificationTransformer.getMetaDataElement(item, withCounter);
        cats.addContent(cat);
        return doc;
    }
}
