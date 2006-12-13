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

package org.mycore.datamodel.classifications.query;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.metadata.MCRLinkTableManager;

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
    public static Classification getClassification(String ID, int levels, boolean withCounter) {
        Document cl = MCRClassification.receiveClassificationAsJDOM(ID, withCounter);
        return ClassificationTransformer.getClassification(cl, levels, withCounter);
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
    public static Classification getClassification(String classID, String categID, int levels, boolean withCounter) {
        LOGGER.debug("start ClassCategSearch");
        Classification returns = getClassification(classID, categID, withCounter);
        if (levels != 0) {
            LOGGER.debug("getCategoryItem");
            MCRCategoryItem catItem = MCRCategoryItem.getCategoryItem(classID, categID);
            // map of every categID with numberofObjects
            LOGGER.debug("countReferenceCategory");
            Map map = withCounter ? MCRLinkTableManager.instance().countReferenceCategory(classID) : null;
            LOGGER.debug("select category");
            Category cat = returns.getCategories().get(0);
            LOGGER.debug("fillCategory");
            ClassificationTransformer.addChildren(cat, catItem, map, levels, withCounter);
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
    public static Classification getClassificationHierarchie(String classID, String categID, int levels, boolean withCounter) {
        MCRCategoryItem catItem = MCRCategoryItem.getCategoryItem(classID, categID);
        MCRCategoryItem parent = catItem.getParent();
        LinkedList<MCRCategoryItem> list = new LinkedList<MCRCategoryItem>();
        list.add(0, catItem);
        while (parent != null) {
            // build the ancestor axis
            list.add(0, parent);
            parent = parent.getParent();
        }
        return ClassificationTransformer.getClassification(catItem, list, levels, withCounter);

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
    public static Classification getClassification(String classID, String categID, boolean withCounter) {
        LOGGER.debug("-receiveCategoryAsJDOM");
        Document doc = MCRClassification.receiveCategoryAsJDOM(classID, categID, withCounter);
        LOGGER.debug("-getClassification");
        Classification returns = ClassificationTransformer.getClassification(doc, -1, withCounter);
        LOGGER.debug("-getClassification finished");
        return returns;
    }

    /**
     * returns <code>Category</code> with id <code>categID</code> in ClassificationObject <code>co</code>.
     * @param co
     * @param categID
     * @return <code>null</code> if no <code>Category</code> with id <code>categID</code> is present as a child of <code>co</code>.
     */
    public static Category findCategory(ClassificationObject co, String categID) {
        if (co instanceof Category && co.getId().equals(categID)) {
            return (Category) co;
        }
        for (Iterator it = co.getCategories().iterator(); it.hasNext();) {
            Category cat = (Category) it.next();
            if (findCategory(cat, categID) != null)
                return cat;
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
    public static ClassificationObject findParent(Classification classification, Category category) {
        return findParentInClassificationObject(classification, category);
    }

    private static ClassificationObject findParentInClassificationObject(ClassificationObject co, Category cat){
        if (co.getCategories().contains(cat)){
            return co;
        }
        for (Category category:co.getCategories()){
            if (findParentInClassificationObject(category, cat) != null){
                return category;
            }
        }
        return null;
    }

    public static void main(String[] arg) {
        boolean withCounter = true;
        Classification c = MCRClassificationQuery.getClassification(arg[0], -1, withCounter);
        MainHelper.print(c, 0);
        try {
            MCRUtils.writeJDOMToSysout(ClassificationTransformer.getMetaDataDocument(c));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // c = MCRClassificationQuery.getClassification(arg[0], arg[1], 0,
        // withCounter);
        // MainHelper.print(c, 0);
        // Document doc = ClassificationTransformer.getMetaDataDocument(c);
        // MainHelper.print(doc);
        // doc = ClassificationTransformer.getEditorDocument(c, true);
        // MainHelper.print(doc);
        // doc = MCRClassification.receiveClassificationAsJDOM(arg[0]);
        // MainHelper.print(doc);
        // c = MCRClassificationQuery.getClassificationHierarchie(arg[0],
        // arg[1], -1, withCounter);
        // doc = ClassificationTransformer.getMetaDataDocument(c);
        // MainHelper.print(doc);
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

        private static void print(ClassificationObject c, int depth) {
            intend(depth);
            System.out.println("ID: " + c.getId());
            for (Category category : c.getCategories()) {
                print(category, depth + 1);
            }
        }

        private static void intend(int a) {
            for (int i = 0; i < a; i++) {
                System.out.print(' ');
            }
        }

    }
}
