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

package org.mycore.datamodel.classifications2.utils;

import static org.jdom.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;

import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
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
     * @return
     */
    public static Document getMetaDataDocument(MCRCategory cl, boolean withCounter) {
        Map<MCRCategoryID, Number> countMap = null;
        if (withCounter) {
            MCRCategLinkServiceFactory.getInstance().countLinks(getAllCategIDs(cl));
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
     * @return
     */
    public static Element getMetaDataElement(MCRCategory category, boolean withCounter) {
        Map<MCRCategoryID, Number> countMap = null;
        if (withCounter) {
            MCRCategLinkServiceFactory.getInstance().countLinks(getAllCategIDs(category));
        }
        return MetaDataElementFactory.getElement(category, countMap);
    }

    /**
     * transforms a <code>Classification</code> into a MCR Editor definition (<code>&lt;items&gt;</code>).
     * 
     * @param cl
     *            Classification
     * @return
     */
    public static Document getEditorDocument(MCRCategory cl, boolean sort) {
        return ItemElementFactory.getDocument(cl, STANDARD_LABEL, sort);
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
    public static Document getEditorDocument(MCRCategory cl, String labelFormat, boolean sort) {
        return ItemElementFactory.getDocument(cl, labelFormat, sort);
    }

    private static class MetaDataElementFactory {
        static Document getDocument(MCRCategory cl, Map<MCRCategoryID, Number> countMap) {
            Document cd = new Document(new Element("mycoreclass"));
            cd.getRootElement().setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", XSI_NAMESPACE);
            cd.getRootElement().setAttribute("ID", cl.getId().getRootID());
            cd.getRootElement().addNamespaceDeclaration(XLINK_NAMESPACE);
            for (MCRLabel label : cl.getLabels().values()) {
                cd.getRootElement().addContent(getElement(label));
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

        static Element getElement(MCRCategory category, Map<MCRCategoryID, Number> countMap) {
            Element ce = new Element("category");
            ce.setAttribute("ID", category.getId().getID());
            Number number = (countMap == null) ? null : countMap.get(category.getId());
            if (number != null) {
                ce.setAttribute("counter", Integer.toString(number.intValue()));
            }
            for (MCRLabel label : category.getLabels().values()) {
                ce.addContent(getElement(label));
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
        static Document getDocument(MCRCategory cl, String labelFormat, boolean sort) {
            Document cd = new Document(new Element("items"));
            Map<MCRCategoryID, Number> countMap = null;
            if (COUNT_PATTERN.matcher(labelFormat).find()) {
                countMap = MCRCategLinkServiceFactory.getInstance().countLinks(getAllCategIDs(cl));
            }
            for (MCRCategory category : cl.getChildren()) {
                cd.getRootElement().addContent(getElement(category, labelFormat, countMap));
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

        static Element getElement(MCRLabel label, MCRCategory cat, String labelFormat, Map<MCRCategoryID, Number> countMap) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
            }
            le.setText(getLabelText(label, cat, labelFormat, countMap));
            return le;
        }

        static String getLabelText(MCRLabel label, MCRCategory cat, String labelFormat, Map<MCRCategoryID, Number> countMap) {
            String labtext = (label.getText() != null) ? label.getText() : "";
            String text = TEXT_PATTERN.matcher(labelFormat).replaceAll(labtext);
            text = ID_PATTERN.matcher(text).replaceAll(cat.getId().getID());
            String labdesc = (label.getDescription() != null) ? label.getDescription() : "";
            text = DESCR_PATTERN.matcher(text).replaceAll(labdesc);
            Number number = (countMap == null) ? null : countMap.get(cat.getId());
            if (number != null) {
                text = COUNT_PATTERN.matcher(text).replaceAll(Integer.toString(number.intValue()));
            }
            return text;
        }

        static Element getElement(MCRCategory category, String labelFormat, Map<MCRCategoryID, Number> countMap) {
            Element ce = new Element("item");
            ce.setAttribute("value", category.getId().getID());
            for (MCRLabel label : category.getLabels().values()) {
                ce.addContent(getElement(label, category, labelFormat, countMap));
            }
            for (MCRCategory cat : category.getChildren()) {
                ce.addContent(getElement(cat, labelFormat, countMap));
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

    private static Collection<MCRCategoryID> getAllCategIDs(MCRCategory category) {
        HashSet<MCRCategoryID> ids = new HashSet<MCRCategoryID>();
        ids.add(category.getId());
        for (MCRCategory cat : category.getChildren()) {
            ids.addAll(getAllCategIDs(cat));
        }
        return ids;
    }

}