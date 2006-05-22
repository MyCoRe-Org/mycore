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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.common.MCRDefaults;

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
    public static Document getEditorDocument(Classification cl) {
        return ItemElementFactory.getDocument(cl, STANDARD_LABEL);
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
    public static Document getEditorDocument(Classification cl, String labelFormat) {
        return ItemElementFactory.getDocument(cl, labelFormat);
    }

    private static class MetaDataElementFactory {
        static Document getDocument(Classification cl) {
            Document cd = new Document(new Element("mycoreclass"));
            cd.getRootElement().setAttribute("noNamespaceSchemaLocation", "MCRClassification.xsd", Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
            cd.getRootElement().setAttribute("ID", cl.getId());
            Iterator it = cl.getLabels().iterator();
            while (it.hasNext()) {
                // add Labels
                cd.getRootElement().addContent(getElement((Label) it.next()));
            }
            Element categories = new Element("categories");
            cd.getRootElement().addContent(categories);
            it = cl.getCatgegories().iterator();
            while (it.hasNext()) {
                // add child categories
                categories.addContent(getElement((Category) it.next(),cl.isCounterEnabled()));
            }
            return cd;
        }

        static Element getElement(Label label) {
            Element le = new Element("label");
            if (stringNotEmpty(label.getLang())) {
                le.setAttribute("lang", label.getLang(), Namespace.XML_NAMESPACE);
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
            Iterator it = category.getLabels().iterator();
            while (it.hasNext()) {
                // add labels
                ce.addContent(getElement((Label) it.next()));
            }
            it = category.getCatgegories().iterator();
            while (it.hasNext()) {
                // add child categories
                ce.addContent(getElement((Category) it.next(),withCounter));
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

    private static class ItemElementFactory {
        private static final Pattern TEXT_PATTERN = Pattern.compile("\\{text\\}");

        private static final Pattern ID_PATTERN = Pattern.compile("\\{id\\}");

        private static final Pattern DESCR_PATTERN = Pattern.compile("\\{description\\}");

        private static final Pattern COUNT_PATTERN = Pattern.compile("\\{count\\}");

        static Document getDocument(Classification cl, String labelFormat) {
            Document cd = new Document(new Element("items"));
            Iterator it = cl.getCatgegories().iterator();
            while (it.hasNext()) {
                // add child categories
                cd.getRootElement().addContent(getElement((Category) it.next(), labelFormat, cl.isCounterEnabled()));
            }
            sortItems(cd.getRootElement().getChildren("item"));
            return cd;
        }

        private static void sortItems(List items) {
            sort(items, EditorItemComparator.CURRENT_LANG_TEXT_ORDER);
            Iterator it = items.iterator();
            while (it.hasNext()) {
                Element item = (Element) it.next();
                List children = item.getChildren("item");
                if (children.size() > 0) {
                    sortItems(children);
                }
            }
        }

        private static void sort(List list, Comparator c) {
            Element[] a = (Element[]) list.toArray(new Element[list.size()]);
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
                le.setAttribute("lang", label.getLang(), Namespace.XML_NAMESPACE);
            }
            le.setText(getLabelText(label, cat, labelFormat, withCounter));
            return le;
        }

        static String getLabelText(Label label, Category cat, String labelFormat, boolean withCounter) {
            String text = TEXT_PATTERN.matcher(labelFormat).replaceAll(label.getText());
            text = ID_PATTERN.matcher(text).replaceAll(cat.getId());
            text = DESCR_PATTERN.matcher(text).replaceAll(label.description);
            if (withCounter){
                text = COUNT_PATTERN.matcher(text).replaceAll(Integer.toString(cat.getNumberOfObjects()));
            }
            return text;
        }

        static Element getElement(Category category, String labelFormat, boolean withCounter) {
            Element ce = new Element("item");
            ce.setAttribute("value", category.getId());
            Iterator it = category.getLabels().iterator();
            while (it.hasNext()) {
                // add labels
                ce.addContent(getElement((Label) it.next(), category, labelFormat, withCounter));
            }
            it = category.getCatgegories().iterator();
            while (it.hasNext()) {
                // add child categories
                ce.addContent(getElement((Category) it.next(), labelFormat, withCounter));
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

}