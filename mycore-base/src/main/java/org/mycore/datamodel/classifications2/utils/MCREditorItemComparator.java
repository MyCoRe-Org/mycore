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

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.mycore.common.MCRSessionMgr;

public class MCREditorItemComparator implements Comparator<Element> {

    private static final HashMap<String, MCREditorItemComparator> MY_COLLATORS = new HashMap<>();

    private Collator myCollator;

    private String language;

    private MCREditorItemComparator(Collator myCollator, String language) {
        super();
        this.myCollator = myCollator;
        this.language = language;
    }

    public int compare(Element o1, Element o2) {
        if (!(o1.getName().equals("item") && o2.getName().equals("item"))) {
            //NO Editor Items
            return 0;
        }
        return myCollator.compare(getCurrentLangLabel(o1, language), getCurrentLangLabel(o2, language));
    }

    private static String getCurrentLangLabel(Element item, String language) {
        List<Element> labels = item.getChildren("label");
        for (Element label : labels) {
            if (label.getAttributeValue("lang", Namespace.XML_NAMESPACE).equals(language)) {
                return label.getText();
            }
        }
        if (labels.size() > 0) {
            //fallback to first label if currentLang label is not found
            return labels.get(0).getText();
        }
        return "";
    }

    private static MCREditorItemComparator getLangCollator(String lang) {
        MCREditorItemComparator comperator = MY_COLLATORS.get(lang);
        if (comperator == null) {
            Locale l = Locale.forLanguageTag(lang);
            Collator collator = Collator.getInstance(l);
            collator.setStrength(Collator.SECONDARY);
            comperator = new MCREditorItemComparator(collator, lang);
            MY_COLLATORS.put(lang, comperator);
        }
        return comperator;
    }

    public static Comparator<Element> getCurrentLangComperator() {
        String currentLanguage = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return getLangCollator(currentLanguage);
    }

}
