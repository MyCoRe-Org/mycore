/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.classifications2.utils;

import static org.jdom2.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 *
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date: 2008-02-06 17:27:24 +0000 (Mi, 06 Feb
 *          2008) $
 */
public class MCRCategoryTransformer {

    private static final String STANDARD_LABEL = "{text}";

    /**
     * transforms a <code>MCRCategory</code> into a JDOM Document.
     *
     * The Document will have a root tag with name "mycoreclass".
     *
     * @param cl
     *            Classification
     */
    public static Document getMetaDataDocument(MCRCategory cl, boolean withCounter) {
        Map<MCRCategoryID, Number> countMap = null;
        if (withCounter) {
            countMap = MCRCategLinkServiceFactory.getInstance().countLinks(cl, false);
        }
        return MetaDataElementFactory.getDocument(cl, countMap);
    }

    /**
     * transforms a <code>MCRCategory</code> into a JDOM Element.
     *
     * The element will have the tag name "category".
     *
     * @param category
     *            a category of a classification
     */
    public static Element getMetaDataElement(MCRCategory category, boolean withCounter) {
        Map<MCRCategoryID, Number> countMap = null;
        if (withCounter) {
            countMap = MCRCategLinkServiceFactory.getInstance().countLinks(category, false);
        }
        return MetaDataElementFactory.getElement(category, countMap);
    }

    /**
     * transforms a <code>Classification</code> into a MCR Editor definition (
     * <code>&lt;items&gt;</code>).
     *
     * @param cl
     *            Classification
     * @param sort
     *            if true, sort items
     * @param emptyLeaves
     *            if true, also include empty leaves
     * @param completeId
     *            if true, category ID is given in form {classID}':'{categID} 
     */
    public static Element getEditorItems(MCRCategory cl, boolean sort, boolean emptyLeaves, boolean completeId) {
        return new ItemElementFactory(cl, STANDARD_LABEL, sort, emptyLeaves, completeId).getResult();
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
     * @param sort
     *            if true, sort items
     * @param emptyLeaves
     *            if true, also include empty leaves
     */
    public static Element getEditorItems(MCRCategory cl, String labelFormat, boolean sort, boolean emptyLeaves,
        boolean completeId) {
        return new ItemElementFactory(cl, labelFormat, sort, emptyLeaves, completeId).getResult();
    }

    static class MetaDataElementFactory {
        static Document getDocument(MCRCategory cl, Map<MCRCategoryID, Number> countMap) {
            Document cd = new Document(new Element("mycoreclass"));
            cd.getRootElement().setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            cd.getRootElement().setAttribute("ID", cl.getId().getRootID());
            cd.getRootElement().addNamespaceDeclaration(XLINK_NAMESPACE);
            MCRCategory root = cl.isClassification() ? cl : cl.getRoot();
            for (MCRLabel label : root.getLabels()) {
                cd.getRootElement().addContent(MCRLabelTransformer.getElement(label));
            }
            Element categories = new Element("categories");
            cd.getRootElement().addContent(categories);
            if (cl.isClassification()) {
                for (MCRCategory category : cl.getChildren()) {
                    categories.addContent(getElement(category, countMap));
                }
            } else {
                categories.addContent(getElement(cl, countMap));
            }
            return cd;
        }

        static Element getElement(MCRCategory category, Map<MCRCategoryID, Number> countMap) {
            Element ce = new Element("category");
            ce.setAttribute("ID", category.getId().getID());
            Number number = countMap == null ? null : countMap.get(category.getId());
            if (number != null) {
                ce.setAttribute("counter", Integer.toString(number.intValue()));
            }
            for (MCRLabel label : category.getLabels()) {
                ce.addContent(MCRLabelTransformer.getElement(label));
            }
            if (category.getURI() != null) {
                URI link = category.getURI();
                ce.addContent(getElement(link));
            }
            for (MCRCategory cat : category.getChildren()) {
                ce.addContent(getElement(cat, countMap));
            }
            return ce;
        }

        static Element getElement(URI link) {
            Element le = new Element("url");
            le.setAttribute("href", link.toString(), XLINK_NAMESPACE);
            // TODO: Have to check url here: any samples?
            le.setAttribute("type", "locator", XLINK_NAMESPACE);
            return le;
        }

        static boolean stringNotEmpty(String test) {
            return test != null && test.length() > 0;
        }
    }

    private static class ItemElementFactory {
        private static final Pattern TEXT_PATTERN = Pattern.compile("\\{text\\}");

        private static final Pattern ID_PATTERN = Pattern.compile("\\{id\\}");

        private static final Pattern DESCR_PATTERN = Pattern.compile("\\{description\\}");

        private static final Pattern COUNT_PATTERN = Pattern.compile("\\{count(:([^\\)]+))?\\}");

        private String labelFormat;

        private boolean emptyLeaves, completeId;

        private Map<MCRCategoryID, Number> countMap = null;

        private Map<MCRCategoryID, Boolean> linkedMap = null;

        private Element root;

        ItemElementFactory(MCRCategory cl, String labelFormat, boolean sort, boolean emptyLeaves, boolean completeId) {
            this.labelFormat = labelFormat;
            this.emptyLeaves = emptyLeaves;
            this.completeId = completeId;

            Matcher countMatcher = COUNT_PATTERN.matcher(labelFormat);
            /*
             * countMatcher.group(0) is the whole expression string like
             * {count:document} countMatcher.group(1) is first inner expression
             * string like :document countMatcher.group(2) is most inner
             * expression string like document
             */
            if (countMatcher.find()) {
                if (countMatcher.group(1) == null) {
                    countMap = MCRCategLinkServiceFactory.getInstance().countLinks(cl, false);
                } else {
                    // group(2) contains objectType
                    String objectType = countMatcher.group(2);
                    countMap = MCRCategLinkServiceFactory.getInstance().countLinksForType(cl, objectType, false);
                }
            }
            if (!emptyLeaves) {
                linkedMap = MCRCategLinkServiceFactory.getInstance().hasLinks(cl);
            }

            root = new Element("items");
            for (MCRCategory category : cl.getChildren()) {
                addChildren(root, category);
            }
            if (sort) {
                final List<Element> items = root.getChildren("item");
                sortItems(items);
            }
        }

        Element getResult() {
            return root;
        }

        void addChildren(Element parent, MCRCategory category) {
            if (!emptyLeaves && !linkedMap.get(category.getId())) {
                return;
            }

            Element ce = new Element("item");
            ce.setAttribute("value", completeId ? category.getId().toString() : category.getId().getID());
            parent.addContent(ce);

            for (MCRLabel label : category.getLabels()) {
                addLabel(ce, label, category);
            }
            for (MCRCategory cat : category.getChildren()) {
                addChildren(ce, cat);
            }
        }

        void addLabel(Element item, MCRLabel label, MCRCategory cat) {
            Element le = new Element("label");
            item.addContent(le);
            if (label.getLang() != null && label.getLang().length() > 0) {
                le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
            }

            String labtext = label.getText() != null ? label.getText() : "";

            String text;
            try {
                text = TEXT_PATTERN.matcher(labelFormat).replaceAll(labtext);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error while inserting '" + labtext + "' into: " + labelFormat, e);
            }
            text = ID_PATTERN.matcher(text).replaceAll(cat.getId().getID());
            text = DESCR_PATTERN.matcher(text)
                .replaceAll(label.getDescription().replace("\\", "\\\\").replace("$", "\\$"));
            int num = countMap == null ? -1 : countMap.get(cat.getId()).intValue();
            if (num >= 0) {
                text = COUNT_PATTERN.matcher(text).replaceAll(String.valueOf(num));
            }

            le.setText(text);
        }

        private void sortItems(List<Element> items) {
            sort(items, MCREditorItemComparator.getCurrentLangComperator());
            for (Element item : items) {
                List<Element> children = item.getChildren("item");
                if (children.size() > 0) {
                    sortItems(children);
                }
            }
        }

        private void sort(List<Element> list, Comparator<Element> c) {
            Element[] a = list.toArray(new Element[list.size()]);
            Arrays.sort(a, c);
            for (Element element : a) {
                element.detach();
            }
            Collections.addAll(list, a);
        }
    }
}
