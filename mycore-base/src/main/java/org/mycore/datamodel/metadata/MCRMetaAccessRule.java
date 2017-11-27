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

package org.mycore.datamodel.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * This class implements all method for handling with the MCRMetaAccessRule part
 * of a metadata object. The MCRMetaAccessRule class present a single item,
 * which hold an ACL condition for a defined permission.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRMetaAccessRule extends MCRMetaDefault {
    // MCRMetaAccessRule data
    protected org.jdom2.Element condition;

    protected String permission;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The constructor of the MCRMetaDefault runs. The <em>permission</em> Attribute
     * is set to 'READ'. The <b>condition</b> is set to 'null'.
     */
    public MCRMetaAccessRule() {
        super();
        condition = null;
        permission = "READ";
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en</b> was set. This is not use in other methods
     * of this class. The subtag element was set to the value of
     * <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception will be throwed. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The condition element was set to the value of
     * <em>set_condition</em>, if it is null, an exception will be throwed.
     * @param set_subtag       the name of the subtag
     * @param set_type         the optional type string
     * @param set_inherted     a value &gt;= 0
     * @param set_permission   permission
     * @param set_condition    the JDOM Element included the condition tree
     * @exception MCRException if the set_subtag value or set_condition is null or empty
     */
    public MCRMetaAccessRule(String set_subtag, String set_type, int set_inherted, String set_permission,
        org.jdom2.Element set_condition) throws MCRException {
        super(set_subtag, null, set_type, set_inherted);
        if (set_condition == null || !set_condition.getName().equals("condition")) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        set(set_permission, set_condition);
    }

    /**
     * This method set the permission and the condition.
     * 
     * @param set_permission the format string, if it is empty 'READ' will be set.
     * @param set_condition
     *            the JDOM Element included the condition tree
     * @exception MCRException
     *                if the set_condition is null or empty
     */
    public final void set(String set_permission, org.jdom2.Element set_condition) throws MCRException {
        setCondition(set_condition);
        setPermission(set_permission);
    }

    /**
     * This method set the condition.
     * 
     * @param set_condition
     *            the JDOM Element included the condition tree
     * @exception MCRException
     *                if the set_condition is null or empty
     */
    public final void setCondition(org.jdom2.Element set_condition) throws MCRException {
        if (set_condition == null || !set_condition.getName().equals("condition")) {
            throw new MCRException("The condition Element of MCRMetaAccessRule is null.");
        }
        condition = set_condition.clone();
    }

    /**
     * This method set the permission attribute.
     * 
     * @param set_permission
     *            the new permission string.
     */
    public final void setPermission(String set_permission) {
        permission = set_permission;
    }

    /**
     * This method get the condition.
     * 
     * @return the condition as JDOM Element
     */
    public final org.jdom2.Element getCondition() {
        return condition;
    }

    /**
     * This method get the permission attribute.
     * 
     * @return the permission attribute
     */
    public final String getPermission() {
        return permission;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);

        setCondition(element.getChild("condition"));
        setPermission(element.getAttributeValue("permission"));
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaAccessRule definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaAccessRule part
     */
    @Override
    public org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.setAttribute("permission", permission);
        elm.addContent(condition.clone());
        return elm;
    }

    /**
     * Validates this MCRMetaAccessRule. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the condition is null</li>
     * <li>the permission is null or empty</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaAccessRule is invalid
     */
    @Override
    public void validate() throws MCRException {
        super.validate();
        if (condition == null) {
            throw new MCRException(getSubTag() + ": condition is null");
        }
        if (permission == null || permission.length() == 0) {
            throw new MCRException(getSubTag() + ": permission is null or empty");
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public MCRMetaAccessRule clone() {
        return new MCRMetaAccessRule(subtag, type, inherited, permission, condition);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Permission         = {}", permission);
            LOGGER.debug("Rule               = " + "condition");
            LOGGER.debug(" ");
        }
    }
}
