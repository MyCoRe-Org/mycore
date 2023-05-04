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
     * the value of <em>subtag</em>. If the
     * value of <em>subtag</em> is null or empty an exception was throwed.
     * The type element was set to an empty string.
     * the <em>classid</em> and the <em>categid</em> must be not null
     * or empty!
     * @param subtag       the name of the subtag
     * @param inherted     a value &gt;= 0
     * @param type         the type attribute
     * @param classid      the classification ID
     * @param categid      the category ID
     *
     * @exception MCRException if the subtag value, the classid value or
     * the categid are null, empty, too long or not a MCRObjectID
     */
    public MCRMetaClassification(String subtag, int inherted, String type, String classid,
        String categid) throws MCRException {
        super(subtag, null, type, inherted);
        setValue(classid, categid);
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>subtag</em>. If the
     * value of <em>subtag</em> is null or empty an exception was throwed.
     * The type element was set to an empty string.
     * the <em>classid</em> and the <em>categid</em> must be not null
     * or empty!
     * @param subtag       the name of the subtag
     * @param inherted     a value &gt;= 0
     * @param type         the type attribute
     * @param category         a category id
     *
     * @exception MCRException if the subtag value is empty, too long or not a MCRObjectID
     */
    public MCRMetaClassification(String subtag, int inherted, String type, MCRCategoryID category)
        throws MCRException {
        super(subtag, null, type, inherted);
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
     * @param classid
     *            the classification ID
     * @param categid
     *            the category ID
     * @exception MCRException
     *                if the classid value or the categid are null,
     *                empty, too long or not a MCRObjectID
     */
    public final void setValue(String classid, String categid) throws MCRException {
        category = new MCRCategoryID(classid, categid);
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     * @exception MCRException
     *                if the classid value or the categid are null,
     *                empty, too long or not a MCRObjectID
     */
    @Override
    public void setFromDOM(Element element) throws MCRException {
        super.setFromDOM(element);

        String classid = element.getAttributeValue("classid");
        String categid = element.getAttributeValue("categid");
        setValue(classid, categid);
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
     * clone of this instance
     * 
     * you will get a (deep) clone of this element
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MCRMetaClassification clone() {
        MCRMetaClassification clone = (MCRMetaClassification) super.clone();

        clone.category = this.category; // immutable so shallow copy ok

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Category            = {}", category);
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
