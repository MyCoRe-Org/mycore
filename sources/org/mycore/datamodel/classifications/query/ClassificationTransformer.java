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

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

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

import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.metadata.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class ClassificationTransformer {

    private static final String STANDARD_LABEL = "{text}";

    /**
     * transforms a <code>Classification</code> into a MCR Classification.
     * 
     * @param cl
     *            Classification
     * @return
     */
    public static Document getMetaDataDocument(Classification cl) {
        return MetaDataElementFactory.getDocument(cl);
    }

    /**
     * transforms a <code>Classification</code> into a MCR Editor definition (<code>&lt;items&gt;</code>).
     * 
     * @param cl
     *            Classification
     * @return
     */
    public static Document getEditorDocument(Classification cl, boolean sort) {
        return ItemElementFactory.getDocument(cl, STANDARD_LABEL, sort);
    }

    /**
     * transforms a MCR Classification into a <code>Classification</code>.
     * 
     * @param cl
     *            MCR Classification as a JDOM Document
     * @return null if <code>cl</code> is not valid
     */
    public static Classification getClassification(Document cl) {
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
    public static Category getCategory(Element categoryElement) {
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
    public static Document getEditorDocument(Classification cl, String labelFormat, boolean sort) {
        return ItemElementFactory.getDocument(cl, labelFormat, sort);
    }

    public static void addChildren(ClassificationObject c, MCRCategoryItem item, Map map, int levels, boolean withCounter) {
        CategoryFactory.fillCategory(c, item, map, levels, withCounter);
    }

    static Classification getClassification(Document cl, int levels, boolean withCounter) {
        return ClassificationFactory.getClassification(cl, levels, withCounter);
    }

    static Classification getClassification(MCRCategoryItem catItem, List<MCRCategoryItem> ancestors, int levels, boolean withCounter) {
        return ClassificationFactory.getClassification(catItem, ancestors, levels, withCounter);
    }

    private static class MetaDataElementFactory {
        static Document getDocument(Classification cl) {
            Document cd = new Document(new Element("mycoreclass"));
            cd.getRootElement().setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            cd.getRootElement().setAttribute("ID", cl.getId());
            cd.getRootElement().addNamespaceDeclaration(XLINK_NAMESPACE);
            for (Label label : cl.getLabels()) {
                cd.getRootElement().addContent(getElement(label));
            }
            Element categories = new Element("categories");
            cd.getRootElement().addContent(categories);
            for (Category category : cl.getCategories()) {
                categories.addContent(getElement(category, cl.isCounterEnabled()));
            }
            return cd;
        }

        static Element getElement(Label label) {
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

        static Element getElement(Category category, boolean withCounter) {
            Element ce = new Element("category");
            ce.setAttribute("ID", category.getId());
            if (withCounter) {
                ce.setAttribute("counter", Integer.toString(category.getNumberOfObjects()));
            }
            for (Label label : category.getLabels()) {
                ce.addContent(getElement(label));
            }
            if (category.getLink() != null) {
                ce.addContent(getElement(category.getLink()));
            }
            for (Category cat : category.getCategories()) {
                ce.addContent(getElement(cat, withCounter));
            }
            return ce;
        }

        static Element getElement(Link link) {
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
        static Document getDocument(Classification cl, String labelFormat, boolean sort) {
            Document cd = new Document(new Element("items"));
            for (Category category : cl.getCategories()) {
                cd.getRootElement().addContent(getElement(category, labelFormat, cl.isCounterEnabled()));
            }
            if (sort) {
                sortItems(cd.getRootElement().getChildren("item"));
            }
            return cd;
        }

        @SuppressWarnings("unchecked")
        private static void sortItems(List<Element> items) {
            sort(items, EditorItemComparator.CURRENT_LANG_TEXT_ORDER);
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

        static Element getElement(Label label, Category cat, String labelFormat, boolean withCounter) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
            }
            le.setText(getLabelText(label, cat, labelFormat, withCounter));
            return le;
        }

        static String getLabelText(Label label, Category cat, String labelFormat, boolean withCounter) {
            String text = TEXT_PATTERN.matcher(labelFormat).replaceAll(label.getText());
            text = ID_PATTERN.matcher(text).replaceAll(cat.getId());
            text = DESCR_PATTERN.matcher(text).replaceAll(label.description);
            if (withCounter) {
                text = COUNT_PATTERN.matcher(text).replaceAll(Integer.toString(cat.getNumberOfObjects()));
            }
            return text;
        }

        static Element getElement(Category category, String labelFormat, boolean withCounter) {
            Element ce = new Element("item");
            ce.setAttribute("value", category.getId());
            for (Label label : category.getLabels()) {
                ce.addContent(getElement(label, category, labelFormat, withCounter));
            }
            for (Category cat : category.getCategories()) {
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
        static Classification getClassification(Element e) {
            Classification c = new Classification();
            c.setId(new MCRObjectID(e.getAttributeValue("ID")).toString());
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            return c;
        }

        static Classification getClassification(MCRClassificationItem i) {
            Classification c = new Classification();
            c.setId(i.getID());
            c.getLabels().addAll(LabelFactory.getLabels(i.getLangArray(), i.getTextArray(), i.getDescriptionArray()));
            return c;
        }

        /**
         * @param cl
         * @param levels
         * @return
         */
        static Classification getClassification(Document cl, int levels, boolean withCounter) {
            Classification returns = getClassification(cl.getRootElement());
            returns.setCounterEnabled(withCounter);
            CategoryFactory.fillCategory(returns.getId(), returns, cl.getRootElement().getChild("categories"), levels, withCounter);
            return returns;
        }

        static Classification getClassification(MCRCategoryItem catItem, List<MCRCategoryItem> ancestors, int levels, boolean withCounter) {
            Classification cl = getClassification(catItem.getClassificationItem());
            // map of every categID with numberofObjects
            Map map = withCounter ? MCRLinkTableManager.instance().countReferenceCategory(cl.getId()) : null;
            Category cat = CategoryFactory.fillCategoryWithParents(cl, ancestors, map, withCounter);
            CategoryFactory.fillCategory(cat, catItem, map, levels, withCounter);
            return cl;
        }
    }

    private static class CategoryFactory {
        @SuppressWarnings("unchecked")
        static Category getCategory(Element e) {
            Category c = new Category();
            c.setId(e.getAttributeValue("ID"));
            c.getLabels().addAll(LabelFactory.getLabels(e.getChildren("label")));
            final Element url = e.getChild("url");
            if (url != null) {
                c.setLink(LinkFactory.getLink(url));
            }
            return c;
        }

        static Category getCategory(MCRCategoryItem i) {
            Category c = new Category();
            c.setId(i.getID());
            c.getLabels().addAll(LabelFactory.getLabels(i.getLangArray(), i.getTextArray(), i.getDescriptionArray()));
            if (i.getURL().length() > 0) {
                c.setLink(LinkFactory.getLink(null, i.getURL(), null, null));

            }
            return c;
        }

        private static Category fillCategoryWithParents(ClassificationObject c, List<MCRCategoryItem> ancestor, Map numDocs, boolean withCounter) {
            ClassificationObject co = c;
            Category cat = null;
            Iterator<MCRCategoryItem> it = ancestor.iterator();
            while (it.hasNext()) {
                MCRCategoryItem item = it.next();
                cat = getCategory(item);
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
                    Category childC = getCategory(child);
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
                    Category childC = getCategory(child);
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

    private static class LinkFactory {

        static Link getLink(Element e) {
            return getLink(e.getAttributeValue("type", XLINK_NAMESPACE), e.getAttributeValue("href", XLINK_NAMESPACE), e.getAttributeValue("title",
                    XLINK_NAMESPACE), e.getAttributeValue("label", XLINK_NAMESPACE));
        }

        static Link getLink(String type, String href, String title, String label) {
            Link link = new Link();
            link.setType(type);
            link.setHref(href);
            link.setTitle(title);
            link.setLabel(label);
            return link;
        }

    }

}