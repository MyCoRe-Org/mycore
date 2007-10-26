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

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRClassificationTransformer {

    private static final String STANDARD_LABEL = "{text}";

    /**
     * transforms a <code>MCRClassificationItem</code> into a JDOM Document.
     * 
     * @param cl
     *            Classification
     * @return
     */
    public static Document getMetaDataDocument(MCRClassificationItem cl) {
        return MetaDataElementFactory.getDocument(cl);
    }

    /**
     * transforms a <code>MCRCategoryItem</code> into a JDOM Element.
     * 
     * @param item
     *            the category item
     * @param withCounter
     *            a flag that the result holds the counter too
     * @return
     */
    public static Element getMetaDataElement(MCRCategoryItem item, boolean withCounter) {
        return MetaDataElementFactory.getElement(item, withCounter);
    }

    /**
     * transforms a <code>Classification</code> into a MCR Editor definition (<code>&lt;items&gt;</code>).
     * 
     * @param cl
     *            Classification
     * @return
     */
    public static Document getEditorDocument(MCRClassificationItem cl, boolean sort) {
        return ItemElementFactory.getDocument(cl, STANDARD_LABEL, sort);
    }

    /**
     * transforms a MCR Classification into a <code>Classification</code>.
     * 
     * @param cl
     *            MCR Classification as a JDOM Document
     * @return null if <code>cl</code> is not valid
     */
    public static MCRClassificationItem getClassification(Document cl) {
        Element categories = cl.getRootElement().getChild("categories");
        if (categories == null) {
            return null;
        }
        boolean withCounter = (categories.getChild("category").getAttribute("counter") != null);
        return ClassificationFactory.getClassification(cl, -1, withCounter);
    }

    /**
     * transforms a MCR category into a <code>Category</code>
     * 
     * @param categoryElement
     *            MCR Category as a JDOM Element
     * @return
     */
    public static MCRCategoryItem getCategory(Element categoryElement) {
        return CategoryFactory.getCategory(categoryElement);
    }

    /**
     * transforms a <code>Classification</code> into a MCR Editor definition (<code>&lt;items&gt;</code>).
     * 
     * This method allows you to specify how the labels will look like.
     * <code>labelFormat</code> is simply a String that is parsed for a few
     * key words, that will be replaced by a dynamic value. The following
     * keywords can be used at the moment:
     * <ul>
     * <li>{id}</li>
     * <li>{text}</li>
     * <li>{description}</li>
     * <li>{count}</li>
     * </ul>
     * 
     * @param cl
     *            Classification
     * @param labelFormat
     *            format String as specified above
     * @return
     */
    public static Document getEditorDocument(MCRClassificationItem cl, String labelFormat, boolean sort) {
        return ItemElementFactory.getDocument(cl, labelFormat, sort);
    }

    public static void addChildren(String ID, MCRClassificationObject item, Element elm, int levels, boolean withCounter) {
        CategoryFactory.fillCategory(ID, item, elm, levels, withCounter);
    }

    public static void addChildren(MCRClassificationObject c, MCRCategoryItem item, Map map, int levels, boolean withCounter) {
        CategoryFactory.fillCategory(c, item, map, levels, withCounter);
    }

    static MCRClassificationItem getClassification(Document cl, int levels, boolean withCounter) {
        return ClassificationFactory.getClassification(cl, levels, withCounter);
    }

    static MCRClassificationItem getClassification(MCRCategoryItem catItem, List<MCRCategoryItem> ancestors, int levels, boolean withCounter) {
        return ClassificationFactory.getClassification(catItem, ancestors, levels, withCounter);
    }

    private static class MetaDataElementFactory {
        static Document getDocument(MCRClassificationItem cl) {
            Document cd = new Document(new Element("mycoreclass"));
            cd.getRootElement().setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            cd.getRootElement().setAttribute("ID", cl.getId());
            cd.getRootElement().addNamespaceDeclaration(XLINK_NAMESPACE);
            for (MCRLabel label : cl.getLabels()) {
                cd.getRootElement().addContent(getElement(label));
            }
            Element categories = new Element("categories");
            cd.getRootElement().addContent(categories);
            for (MCRCategoryItem category : cl.getCategories()) {
                categories.addContent(getElement(category, cl.isCounterEnabled()));
            }
            return cd;
        }

        static Element getElement(MCRLabel label) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
            }
            if (stringNotEmpty(label.getText())) {
                le.setAttribute("text", label.getText());
            }
            if (stringNotEmpty(label.getDescription())) {
                le.setAttribute("description", label.getDescription());
            }
            return le;
        }

        static Element getElement(MCRCategoryItem category, boolean withCounter) {
            Element ce = new Element("category");
            ce.setAttribute("ID", category.getId());
            if (withCounter) {
                ce.setAttribute("counter", Integer.toString(category.getNumberOfObjects()));
            }
            for (MCRLabel label : category.getLabels()) {
                ce.addContent(getElement(label));
            }
            if (category.getLink() != null) {
                MCRLink link = category.getLink();
                if ((link.getHref() != null) && (link.getHref().length() != 0)) {
                    ce.addContent(getElement(link));
                }
            }
            for (MCRCategoryItem cat : category.getCategories()) {
                ce.addContent(getElement(cat, withCounter));
            }
            return ce;
        }

        static Element getElement(MCRLink link) {
            Element le = new Element("url");
            if (link.getHref() != null) {
                le.setAttribute("href", link.getHref(), XLINK_NAMESPACE);
            }
            if (link.getLabel() != null) {
                le.setAttribute("label", link.getLabel(), XLINK_NAMESPACE);
            }
            if (link.getTitle() != null) {
                le.setAttribute("title", link.getTitle(), XLINK_NAMESPACE);
            }
            if (link.getType() != null) {
                le.setAttribute("type", link.getType(), XLINK_NAMESPACE);
            }
            return le;
        }

        static boolean stringNotEmpty(String test) {
            if (test != null && test.length() > 0) {
                return true;
            }
            return false;
        }
    }

    private static class ItemElementFactory {
        private static final Pattern TEXT_PATTERN = Pattern.compile("\\{text\\}");

        private static final Pattern ID_PATTERN = Pattern.compile("\\{id\\}");

        private static final Pattern DESCR_PATTERN = Pattern.compile("\\{description\\}");

        private static final Pattern COUNT_PATTERN = Pattern.compile("\\{count\\}");

        @SuppressWarnings("unchecked")
        static Document getDocument(MCRClassificationItem cl, String labelFormat, boolean sort) {
            Document cd = new Document(new Element("items"));
            for (MCRCategoryItem category : cl.getCategories()) {
                cd.getRootElement().addContent(getElement(category, labelFormat, cl.isCounterEnabled()));
            }
            if (sort) {
                sortItems(cd.getRootElement().getChildren("item"));
            }
            return cd;
        }

        @SuppressWarnings("unchecked")
        private static void sortItems(List<Element> items) {
            sort(items, MCREditorItemComparator.CURRENT_LANG_TEXT_ORDER);
            Iterator<Element> it = items.iterator();
            while (it.hasNext()) {
                Element item = it.next();
                List<Element> children = item.getChildren("item");
                if (children.size() > 0) {
                    sortItems(children);
                }
            }
        }

        private static void sort(List<Element> list, Comparator<Element> c) {
            Element[] a = list.toArray(new Element[list.size()]);
            Arrays.sort(a, c);
            for (int i = 0; i < a.length; i++) {
                a[i].detach();
            }
            for (int i = 0; i < a.length; i++) {
                list.add(a[i]);
            }
        }

        static Element getElement(MCRLabel label, MCRCategoryItem cat, String labelFormat, boolean withCounter) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
            }
            le.setText(getLabelText(label, cat, labelFormat, withCounter));
            return le;
        }

        static String getLabelText(MCRLabel label, MCRCategoryItem cat, String labelFormat, boolean withCounter) {
            String labtext = (label.getText() != null) ? label.getText() : "";
            String text = TEXT_PATTERN.matcher(labelFormat).replaceAll(labtext);
            text = ID_PATTERN.matcher(text).replaceAll(cat.getId());
            String labdesc = (label.getDescription() != null) ? label.getDescription() : "";
            text = DESCR_PATTERN.matcher(text).replaceAll(labdesc);
            if (withCounter) {
                text = COUNT_PATTERN.matcher(text).replaceAll(Integer.toString(cat.getNumberOfObjects()));
            }
            return text;
        }

        static Element getElement(MCRCategoryItem category, String labelFormat, boolean withCounter) {
            Element ce = new Element("item");
            ce.setAttribute("value", category.getId());
            for (MCRLabel label : category.getLabels()) {
                ce.addContent(getElement(label, category, labelFormat, withCounter));
            }
            for (MCRCategoryItem cat : category.getCategories()) {
                ce.addContent(getElement(cat, labelFormat, withCounter));
            }
            return ce;
        }

        static boolean stringNotEmpty(String test) {
            if (test != null && test.length() > 0) {
                return true;
            }
            return false;
        }
    }

    private static class ClassificationFactory {
        @SuppressWarnings("unchecked")
        static MCRClassificationItem getClassification(Element e) {
            MCRClassificationItem c = new MCRClassificationItem();
            c.setId(new MCRObjectID(e.getAttributeValue("ID")).toString());
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            return c;
        }

        /**
         * @param cl
         * @param levels
         * @return
         */
        static MCRClassificationItem getClassification(Document cl, int levels, boolean withCounter) {
            MCRClassificationItem returns = getClassification(cl.getRootElement());
            returns.setCounterEnabled(withCounter);
            CategoryFactory.fillCategory(returns.getId(), returns, cl.getRootElement().getChild("categories"), levels, withCounter);
            return returns;
        }

        static MCRClassificationItem getClassification(MCRCategoryItem catItem, List<MCRCategoryItem> ancestors, int levels, boolean withCounter) {
            MCRClassificationItem cl = MCRClassificationManager.instance().retrieveClassificationItem(catItem.getClassID());
            // map of every categID with numberofObjects
            Map map = withCounter ? MCRLinkTableManager.instance().countReferenceCategory(cl.getId()) : null;
            MCRCategoryItem cat = CategoryFactory.fillCategoryWithParents(cl, ancestors, map, withCounter);
            CategoryFactory.fillCategory(cat, catItem, map, levels, withCounter);
            return cl;
        }
    }

    private static class CategoryFactory {

        @SuppressWarnings("unchecked")
        static MCRCategoryItem getCategory(Element e) {
            MCRCategoryItem c = new MCRCategoryItem();
            c.setId(e.getAttributeValue("ID"));
            Element parent = (Element) e.getParent();
            if ((parent != null) && (parent.getName().equals("category"))) {
                c.setParentID(parent.getAttributeValue("ID"));
            } else {
                c.setParentID(null);
            }
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            final Element url = e.getChild("url");
            if (url != null) {
                c.setLink(LinkFactory.getLink(url));
            }
            return c;
        }

        private static MCRCategoryItem fillCategoryWithParents(MCRClassificationObject co, List<MCRCategoryItem> ancestor, Map numDocs, boolean withCounter) {
            MCRCategoryItem cat = null;
            Iterator<MCRCategoryItem> it = ancestor.iterator();
            while (it.hasNext()) {
                MCRCategoryItem item = it.next();
                cat = item;
                if (withCounter) {
                    cat.setNumberOfObjects(getNumberOfObjects(cat.getClassID(), cat.getId(), numDocs));
                }
                co.getCategories().add(cat);
                co = cat;
            }
            return cat;
        }

        private static void fillCategory(MCRClassificationObject c, MCRCategoryItem item, Map map, int levels, boolean withCounter) {
            if (levels != 0) {
                item.getCategories().addAll(Arrays.asList(MCRClassificationManager.instance().retrieveChildren(item.getClassID(), item.getId())));
                for (MCRCategoryItem child : item.getCategories()) {
                    if (withCounter) {
                        int count = getNumberOfObjects(item.getClassID(), item.getId(), map);
                        child.setNumberOfObjects(count);
                    }
                    child.setClassID(item.getClassID());
                    fillCategory(child, child, map, levels - 1, withCounter);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private static void fillCategory(String classID, MCRClassificationObject c, Element e, int levels, boolean withCounter) {
            if (levels != 0) {
                List<Element> children = e.getChildren("category");
                Iterator<Element> it = children.iterator();
                while (it.hasNext()) {
                    Element child = it.next();
                    MCRCategoryItem childC = getCategory(child);
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
            int count = (map.get(mapKey) != null) ? ((Number) map.get(mapKey)).intValue() : 0;
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
    }

    private static class LabelFactory {
        static MCRLabel getLabel(Element e) {
            return getLabel(e.getAttributeValue("lang", Namespace.XML_NAMESPACE), e.getAttributeValue("text"), e.getAttributeValue("description"));
        }

        static MCRLabel getLabel(String lang, String text, String description) {
            MCRLabel label = new MCRLabel(lang, text, description);
            return label;
        }

        static List<MCRLabel> getLabels(List<Element> labels) {
            List<MCRLabel> returns = new ArrayList<MCRLabel>(labels.size());
            Iterator<Element> it = labels.iterator();
            while (it.hasNext()) {
                returns.add(getLabel(it.next()));
            }
            return returns;
        }

        static List<MCRLabel> getLabels(List lang, List text, List description) {
            List<MCRLabel> returns = new ArrayList<MCRLabel>(lang.size());
            for (int i = 0; i < lang.size(); i++) {
                returns.add(getLabel(lang.get(i).toString(), text.get(i).toString(), description.get(i).toString()));
            }
            return returns;
        }
    }

    private static class LinkFactory {

        static MCRLink getLink(Element e) {
            return getLink(e.getAttributeValue("type", XLINK_NAMESPACE), e.getAttributeValue("href", XLINK_NAMESPACE), e.getAttributeValue("title", XLINK_NAMESPACE), e.getAttributeValue("label", XLINK_NAMESPACE));
        }

        static MCRLink getLink(String type, String href, String title, String label) {
            MCRLink link = new MCRLink(type, href, title, label);
            return link;
        }

    }

}