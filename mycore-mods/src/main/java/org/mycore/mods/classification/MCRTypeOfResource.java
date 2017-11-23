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

package org.mycore.mods.classification;

import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.w3c.dom.Element;

/**
 * Authority information that is a static mapping for mods:typeOfResource. This element is always mapped to a
 * classification with the ID typeOfResource.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
class MCRTypeOfResource extends MCRAuthorityInfo {

    /**
     * The name of the MODS element typeOfResource, which is same as the classification ID used to map the codes to
     * categories.
     */
    public static final String TYPE_OF_RESOURCE = "typeOfResource";

    /**
     * The mods:typeOfResource code, which is same as the category ID
     */
    private String code;

    public MCRTypeOfResource(String code) {
        this.code = code;
    }

    /**
     * If the given element is mods:typeOfResource, returns the MCRTypeOfResource mapping.
     */
    public static MCRTypeOfResource getAuthorityInfo(org.jdom2.Element modsElement) {
        if (modsElement == null) {
            return null;
        }
        String name = modsElement.getName();
        String code = modsElement.getTextTrim();
        return getTypeOfResource(name, code);
    }

    /**
     * If the given element is mods:typeOfResource, returns the MCRTypeOfResource mapping.
     */
    public static MCRTypeOfResource getAuthorityInfo(Element modsElement) {
        if (modsElement == null) {
            return null;
        }
        String name = modsElement.getLocalName();
        String code = MCRMODSClassificationSupport.getText(modsElement).trim();
        return getTypeOfResource(name, code);
    }

    /**
     * If the given element name is typeOfResource, returns the MCRTypeOfResource mapping.
     */
    private static MCRTypeOfResource getTypeOfResource(String name, String code) {
        return (name.equals(TYPE_OF_RESOURCE) && isClassificationPresent()) ? new MCRTypeOfResource(code) : null;
    }

    @Override
    public String toString() {
        return TYPE_OF_RESOURCE + "#" + code;
    }

    @Override
    protected MCRCategoryID lookupCategoryID() {
        return new MCRCategoryID(TYPE_OF_RESOURCE, code.replace(" ", "_")); // Category IDs can not contain spaces
    }

    @Override
    public void setInElement(org.jdom2.Element element) {
        element.setText(code);

    }

    @Override
    public void setInElement(Element element) {
        element.setTextContent(code);
    }

    public static boolean isClassificationPresent() {
        return MCRCategoryDAOFactory.getInstance().exist(MCRCategoryID.rootID(TYPE_OF_RESOURCE));
    }
}
