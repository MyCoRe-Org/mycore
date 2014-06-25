/*
 * 
 * $Revision: 13085 $ $Date: 02.02.2012 22:18:40 $
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

import static org.jdom2.Namespace.XML_NAMESPACE;

import org.jdom2.Element;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer.MetaDataElementFactory;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLabelTransformer {

    public static Element getElement(MCRLabel label) {
        Element le = new Element("label");
        if (MetaDataElementFactory.stringNotEmpty(label.getLang())) {
            le.setAttribute("lang", label.getLang(), XML_NAMESPACE);
        }
        le.setAttribute("text", label.getText() != null ? label.getText() : "");
        if (MetaDataElementFactory.stringNotEmpty(label.getDescription())) {
            le.setAttribute("description", label.getDescription());
        }
        return le;
    }

}
