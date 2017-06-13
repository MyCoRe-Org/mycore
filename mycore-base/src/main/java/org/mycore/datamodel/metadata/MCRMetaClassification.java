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

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategoryID;

import com.google.gson.JsonObject;

/**
 * This class implements all method for handling with the MCRMetaClassification
 * part of a metadata object. The MCRMetaClassification class present a link to
 * a category of a classification.
 * <p>
 * &lt;tag class="MCRMetaClassification" heritable="..."&gt; <br>
 * &lt;subtag classid="..." categid="..." /&gt; <br>
 * &lt;/tag&gt; <br>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date: 2008-03-18 22:53:44 +0000 (Di, 18 Mrz
 *          2008) $
 */
public class MCRMetaClassification extends MCRMetaDefault {
    private static final Logger LOGGER = LogManager.getLogger();

    // MCRMetaClassification data
    protected MCRCategoryID category;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The classid and categid value
     * was set to an empty string.
     */
    public MCRMetaClassification() {
        super();
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the
     * value of <em>set_subtag</em> is null or empty an exception was throwed.
     * The type element was set to an empty string.
     * the <em>set_classid</em> and the <em>categid</em> must be not null
     * or empty!
     * @param set_subtag       the name of the subtag
     * @param set_inherted     a value &gt;= 0
     * @param set_type         the type attribute
     * @param set_classid      the classification ID
     * @param set_categid      the category ID
     *
     * @exception MCRException if the set_subtag value, the set_classid value or
     * the set_categid are null, empty, too long or not a MCRObjectID
     */
    public MCRMetaClassification(String set_subtag, int set_inherted, String set_type, String set_classid,
        String set_categid) throws MCRException {
        super(set_subtag, null, set_type, set_inherted);
        setValue(set_classid, set_categid);
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the
     * value of <em>set_subtag</em> is null or empty an exception was throwed.
     * The type element was set to an empty string.
     * the <em>set_classid</em> and the <em>categid</em> must be not null
     * or empty!
     * @param set_subtag       the name of the subtag
     * @param set_inherted     a value &gt;= 0
     * @param set_type         the type attribute
     * @param category         a category id
     *
     * @exception MCRException if the set_subtag value is empty, too long or not a MCRObjectID
     */
    public MCRMetaClassification(String set_subtag, int set_inherted, String set_type, MCRCategoryID category)
        throws MCRException {
        super(set_subtag, null, set_type, set_inherted);
        if (category == null) {
            throw new MCRException("Category is not set in " + getSubTag());
        }
        this.category = category;
    }

    /**
     * The method return the classification ID.
     * 
     * @return the classId
     */
    public final String getClassId() {
        return category.getRootID();
    }

    /**
     * The method return the category ID.
     * 
     * @return the categId
     */
    public final String getCategId() {
        return category.getID();
    }

    /**
     * This method set values of classid and categid.
     * 
     * @param set_classid
     *            the classification ID
     * @param set_categid
     *            the category ID
     * @exception MCRException
     *                if the set_classid value or the set_categid are null,
     *                empty, too long or not a MCRObjectID
     */
    public final void setValue(String set_classid, String set_categid) throws MCRException {
        if (set_classid == null || (set_classid = set_classid.trim()).length() == 0) {
            throw new MCRException("The classid is empty.");
        }

        if (set_categid == null || (set_categid = set_categid.trim()).length() == 0) {
            throw new MCRException("The categid is empty.");
        }
        category = new MCRCategoryID(set_classid, set_categid);
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     * @exception MCRException
     *                if the set_classid value or the set_categid are null,
     *                empty, too long or not a MCRObjectID
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) throws MCRException {
        super.setFromDOM(element);

        String set_classid = element.getAttributeValue("classid");
        String set_categid = element.getAttributeValue("categid");
        setValue(set_classid, set_categid);
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaClassification definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRClassification part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.setAttribute("classid", getClassId());
        elm.setAttribute("categid", getCategId());

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     classid: "mycore_class_00000001",
     *     categid: "category1"
     *   }
     * </pre>
     * 
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        obj.addProperty("classid", category.getRootID());
        obj.addProperty("categid", category.getID());
        return obj;
    }

    /**
     * Validates this MCRMetaClassification. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the category is null</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaClassification is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (category == null) {
            throw new MCRException(getSubTag() + ": category is not yet set");
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public MCRMetaClassification clone() {
        return new MCRMetaClassification(subtag, inherited, type, category);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Category            = " + category);
            LOGGER.debug(" ");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaClassification other = (MCRMetaClassification) obj;
        return Objects.equals(this.category, other.category);
    }

}
