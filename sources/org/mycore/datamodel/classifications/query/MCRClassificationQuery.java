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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.classifications.MCRClassificationItem;
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
        return getClassification(cl, levels, withCounter);
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
            fillCategory(cat, catItem, map, levels, withCounter);
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
        return getClassification(catItem, list, levels, withCounter);

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
        Classification returns = getClassification(doc, -1, withCounter);
        LOGGER.debug("-getClassification finished");
        return returns;
    }

    public static void main(String[] arg) {
        boolean withCounter = true;
        Classification c = MCRClassificationQuery.getClassification(arg[0], 1, withCounter);
        MainHelper.print(c, 0);
        c = MCRClassificationQuery.getClassification(arg[0], arg[1], 0, withCounter);
        MainHelper.print(c, 0);
        Document doc = ClassificationTransformer.getMetaDataDocument(c);
        MainHelper.print(doc);
        doc = ClassificationTransformer.getEditorDocument(c);
        MainHelper.print(doc);
        doc = MCRClassification.receiveClassificationAsJDOM(arg[0]);
        MainHelper.print(doc);
        c = MCRClassificationQuery.getClassificationHierarchie(arg[0], arg[1], -1, withCounter);
        doc = ClassificationTransformer.getMetaDataDocument(c);
        MainHelper.print(doc);
    }

    /**
     * @param cl
     * @param levels
     * @return
     */
    static Classification getClassification(Document cl, int levels, boolean withCounter) {
        Classification returns = ClassificationFactory.getClassification(cl.getRootElement());
        returns.setCounterEnabled(withCounter);
        fillCategory(returns.getId(), returns, cl.getRootElement().getChild("categories"), levels, withCounter);
        return returns;
    }

    private static Classification getClassification(MCRCategoryItem catItem, List<MCRCategoryItem> ancestors, int levels, boolean withCounter) {
        Classification cl = ClassificationFactory.getClassification(catItem.getClassificationItem());
        // map of every categID with numberofObjects
        Map map = withCounter ? MCRLinkTableManager.instance().countReferenceCategory(cl.getId()) : null;
        Category cat = fillCategoryWithParents(cl, ancestors, map, withCounter);
        fillCategory(cat, catItem, map, levels, withCounter);
        return cl;
    }

    private static Category fillCategoryWithParents(ClassificationObject c, List<MCRCategoryItem> ancestor, Map numDocs, boolean withCounter) {
        ClassificationObject co = c;
        Category cat = null;
        Iterator<MCRCategoryItem> it = ancestor.iterator();
        while (it.hasNext()) {
            MCRCategoryItem item = it.next();
            cat = CategoryFactory.getCategory(item);
            if (withCounter) {
                cat.setNumberOfObjects(getNumberOfObjects(cat.getClassID(), cat.getId(), numDocs));
            }
            co.getCategories().add(cat);
            co = cat;
        }
        return cat;
    }

    private static void fillCategory(ClassificationObject c, MCRCategoryItem item, Map map, int levels, boolean withCounter) {
        if (levels != 0) {
            MCRCategoryItem[] children = item.getChildren();
            for (int i = 0; i < children.length; i++) {
                MCRCategoryItem child = children[i];
                Category childC = CategoryFactory.getCategory(child);
                if (withCounter) {
                    int count = getNumberOfObjects(item.getClassificationID(), item.getID(), map);
                    childC.setNumberOfObjects(count);
                }
                childC.setClassID(item.getClassificationID());
                c.getCategories().add(childC);
                fillCategory(childC, child, map, levels - 1, withCounter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void fillCategory(String classID, ClassificationObject c, Element e, int levels, boolean withCounter) {
        if (levels != 0) {
            List<Element> children = e.getChildren("category");
            Iterator<Element> it = children.iterator();
            while (it.hasNext()) {
                Element child = it.next();
                Category childC = CategoryFactory.getCategory(child);
                if (withCounter) {
                    childC.setNumberOfObjects(getNumberOfObjects(child));
                }
                childC.setClassID(classID);
                c.getCategories().add(childC);
                fillCategory(classID, childC, child, levels - 1, withCounter);
            }
        }
    }

    private static int getNumberOfObjects(String classID, String categID, Map map) {
        String mapKey = classID + "##" + categID;
        int count = (map.get(mapKey) != null) ? ((Integer) map.get(mapKey)).intValue() : 0;
        return count;
    }

    private static int getNumberOfObjects(Element e) {
        String counter = e.getAttributeValue("counter");
        int returns = 0;
        if (counter != null) {
            returns = Integer.parseInt(counter);
        }
        return returns;
    }

    private static class ClassificationFactory {
        @SuppressWarnings("unchecked")
        static Classification getClassification(Element e) {
            Classification c = new Classification();
            c.setId(e.getAttributeValue("ID"));
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            return c;
        }

        static Classification getClassification(MCRClassificationItem i) {
            Classification c = new Classification();
            c.setId(i.getID());
            c.getLabels().addAll(LabelFactory.getLabels(i.getLangArray(), i.getTextArray(), i.getDescriptionArray()));
            return c;
        }
    }

    private static class CategoryFactory {
        @SuppressWarnings("unchecked")
        static Category getCategory(Element e) {
            Category c = new Category();
            c.setId(e.getAttributeValue("ID"));
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            return c;
        }

        static Category getCategory(MCRCategoryItem i) {
            Category c = new Category();
            c.setId(i.getID());
            c.getLabels().addAll(LabelFactory.getLabels(i.getLangArray(), i.getTextArray(), i.getDescriptionArray()));
            return c;
        }
    }

    private static class LabelFactory {
        static Label getLabel(Element e) {
            return getLabel(e.getAttributeValue("lang", Namespace.XML_NAMESPACE), e.getAttributeValue("text"), e.getAttributeValue("description"));
        }

        static Label getLabel(String lang, String text, String description) {
            Label label = new Label();
            label.setText(text);
            label.setDescription(description);
            label.setLang(lang);
            return label;
        }

        static List<Label> getLabels(List<Element> labels) {
            List<Label> returns = new ArrayList<Label>(labels.size());
            Iterator<Element> it = labels.iterator();
            while (it.hasNext()) {
                returns.add(getLabel(it.next()));
            }
            return returns;
        }

        static List<Label> getLabels(List lang, List text, List description) {
            List<Label> returns = new ArrayList<Label>(lang.size());
            for (int i = 0; i < lang.size(); i++) {
                returns.add(getLabel(lang.get(i).toString(), text.get(i).toString(), description.get(i).toString()));
            }
            return returns;
        }
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

        private static void print(Document doc) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            try {
                xout.output(doc, System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static void intend(int a) {
            for (int i = 0; i < a; i++) {
                System.out.print(' ');
            }
        }

    }
}
