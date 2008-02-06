/*
 * 
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

package org.mycore.datamodel.metadata;

import org.mycore.common.MCRNormalizer;

/**
 * This class implements only static methods to normalize text values.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRNormalizeText {
    /**
     * This methode replace any characters of languages like german to
     * normalized values. It works over a MCRObject JDOM tree. The following
     * items will be changed:<br />
     * <li>Text()-Node of MCRMeatTextLang</li>
     * 
     * @param doc
     *            a JDOM tree of a MCRObject
     */
    public static final void normalizeJDOM(org.jdom.Document doc) {
        if (doc == null) {
            return;
        }

        org.jdom.Element root = doc.getRootElement();
        org.jdom.Element metadata = root.getChild("metadata");

        if (metadata == null) {
            return;
        }

        java.util.List metalist = metadata.getChildren();
        int lm = metalist.size();

        for (int i = 0; i < lm; i++) {
            org.jdom.Element metaitem = (org.jdom.Element) metalist.get(i);
            String metaname = metaitem.getAttributeValue("class");

            if (metaname.equals("MCRMetaLangText")) {
                java.util.List metaelms = metaitem.getChildren();
                int le = metaelms.size();

                for (int j = 0; j < le; j++) {
                    org.jdom.Element metaelm = (org.jdom.Element) metaelms.get(j);
                    metaelm.setText(MCRNormalizer.normalizeString(metaelm.getText()));
                }
            }

            if (metaname.equals("MCRMetaPersonName")) {
                java.util.List metaelms = metaitem.getChildren();
                int le = metaelms.size();

                for (int j = 0; j < le; j++) {
                    org.jdom.Element metaelm = (org.jdom.Element) metaelms.get(j);
                    org.jdom.Element sub = metaelm.getChild("firstname");
                    if (sub != null) {
                        sub.setText(MCRNormalizer.normalizeString(sub.getText()));
                    }
                    sub = metaelm.getChild("callname");
                    if (sub != null) {
                        sub.setText(MCRNormalizer.normalizeString(sub.getText()));
                    }
                    sub = metaelm.getChild("fullname");
                    if (sub != null) {
                        sub.setText(MCRNormalizer.normalizeString(sub.getText()));
                    }
                    sub = metaelm.getChild("surname");
                    if (sub != null) {
                        sub.setText(MCRNormalizer.normalizeString(sub.getText()));
                    }
                }
            }

            if (metaname.equals("MCRMetaInstitutionName")) {
                java.util.List metaelms = metaitem.getChildren();
                int le = metaelms.size();

                for (int j = 0; j < le; j++) {
                    org.jdom.Element metaelm = (org.jdom.Element) metaelms.get(j);
                    org.jdom.Element sub = metaelm.getChild("fullname");
                    if (sub != null) {
                        sub.setText(MCRNormalizer.normalizeString(sub.getText()));
                    }
                }
            }
        }
    }

    /**
     * This methode replace any characters of languages like german to
     * normalized values. It works over a MCRObject. The following items will be
     * changed:<br />
     * <li>Text()-Node of MCRMeatTextLang</li>
     * 
     * @param obj
     *            an instance of a MCRObject
     */
    public static final void normalizeMCRObject(MCRBase obj) {
        if (obj == null) {
            return;
        }

        if (obj instanceof MCRDerivate) {
            return;
        }

        MCRObjectMetadata metadata = ((MCRObject) obj).getMetadata();

        for (int i = 0; i < metadata.size(); i++) {
            MCRMetaElement metaelm = metadata.getMetadataElement(i);

            if (metaelm.getClassName().equals("MCRMetaLangText")) {
                for (int j = 0; j < metaelm.size(); j++) {
                    MCRMetaLangText item = (MCRMetaLangText) metaelm.getElement(j);
                    item.setText(MCRNormalizer.normalizeString(item.getText()));
                }
            }

            if (metaelm.getClassName().equals("MCRMetaPersonName")) {
                for (int j = 0; j < metaelm.size(); j++) {
                    MCRMetaPersonName item = (MCRMetaPersonName) metaelm.getElement(j);
                    String a = item.getAcademic();
                    String c = MCRNormalizer.normalizeString(item.getCallName());
                    String v = MCRNormalizer.normalizeString(item.getFirstName());
                    String f = MCRNormalizer.normalizeString(item.getFullName());
                    String s = MCRNormalizer.normalizeString(item.getSurName());
                    String p = item.getPeerage();
                    String z = item.getPrefix();
                    item.set(v, c, s, f, a, p, z);
                }
            }

            if (metaelm.getClassName().equals("MCRMetaInstitutionName")) {
                for (int j = 0; j < metaelm.size(); j++) {
                    MCRMetaInstitutionName item = (MCRMetaInstitutionName) metaelm.getElement(j);
                    String f = MCRNormalizer.normalizeString(item.getFullName());
                    String p = item.getProperty();
                    String n = item.getNickname();
                    item.set(f, n, p);
                }
            }
        }
    }
}
