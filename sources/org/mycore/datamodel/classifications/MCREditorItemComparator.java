/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.datamodel.classifications;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

import com.ibm.icu.text.Normalizer;

public class MCREditorItemComparator implements Comparator<Element> {
    
    public static final MCREditorItemComparator CURRENT_LANG_TEXT_ORDER=new MCREditorItemComparator();

    private MCREditorItemComparator() {
        super();
    }

    public int compare(Element o1, Element o2) {
        if (!((o1.getName().equals("item"))&&(o2.getName().equals("item")))){
            //NO Editor Items
            return 0;
        }
        // Handle Problems with sorting of german umlauts and other diacritics 
        // Tänzerin is now before Träumer
        String la = Normalizer.decompose(getCurrentLangLabel(o1), false);
        String lb = Normalizer.decompose(getCurrentLangLabel(o2), false);
        return String.CASE_INSENSITIVE_ORDER.compare(la,lb);
    }
    
    @SuppressWarnings("unchecked")
    private static String getCurrentLangLabel(Element item){
        MCRSession session=MCRSessionMgr.getCurrentSession();
        String currentLang=session.getCurrentLanguage();
        List<Element> labels=item.getChildren("label");
        Iterator<Element> it=labels.iterator();
        while (it.hasNext()){
            Element label=it.next();
            if (label.getAttributeValue("lang",Namespace.XML_NAMESPACE).equals(currentLang)){
                return label.getText();
            }
        }
        if (labels.size()>0){
            //fallback to first label if currentLang label is not found
            return labels.get(0).getText();
        }
        return "";
    }

}
