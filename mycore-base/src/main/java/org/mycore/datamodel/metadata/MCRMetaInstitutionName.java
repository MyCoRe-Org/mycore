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

import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This class implements all methods for handling with the
 * MCRMetaInstitutionName part of a metadata object. The MCRMetaInstitutionName
 * class represents a name of an institution or corporation.
 * 
 * @author J. Kupferschmidt
 * @version $Revision$ $Date$
 */
final public class MCRMetaInstitutionName extends MCRMetaDefault {
    // data
    private String fullname;

    private String nickname;

    private String property;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts are set to
     * an empty string.
     */
    public MCRMetaInstitutionName() {
        super();
        fullname = "";
        nickname = "";
        property = "";
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag</em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>set_type</em>, if it is null, an empty string was set
     * to the type element. The fullname, nickname  and property element
     * was set to the value of <em>set_...</em>, if they are null,
     * an empty string was set to this element.
     * @param set_subtag      the name of the subtag
     * @param default_lang    the default language
     * @param set_type        the optional type string
     * @param set_inherted    a value &gt;= 0
     * @param set_fullname    the full name
     * @param set_nickname    the nickname
     * @param set_property       the property title
     *
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaInstitutionName(String set_subtag, String default_lang, String set_type, int set_inherted,
        String set_fullname, String set_nickname, String set_property) throws MCRException {
        super(set_subtag, default_lang, set_type, set_inherted);
        fullname = "";
        nickname = "";
        property = "";
        set(set_fullname, set_nickname, set_property);
    }

    /**
     * This methode set all name componets.
     * 
     * @param set_fullname
     *            the full name
     * @param set_nickname
     *            the nickname
     * @param set_property
     *            the property title
     */
    public final void set(String set_fullname, String set_nickname, String set_property) {
        if (set_fullname == null || set_nickname == null || set_property == null) {
            throw new MCRException("One parameter is null.");
        }

        fullname = set_fullname.trim();
        nickname = set_nickname.trim();
        property = set_property.trim();
    }

    /**
     * This method get the name text element.
     * 
     * @return the fullname
     */
    public final String getFullName() {
        return fullname;
    }

    /**
     * This method get the nickname text element.
     * 
     * @return the nickname
     */
    public final String getNickname() {
        return nickname;
    }

    /**
     * This method get the property text element.
     * 
     * @return the property
     */
    public final String getProperty() {
        return property;
    }

    /**
     * This method reads the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant DOM element for the metadata
     */
    @Override
    public final void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        fullname = element.getChildTextTrim("fullname");

        if (fullname == null) {
            fullname = "";
        }

        nickname = element.getChildTextTrim("nickname");

        if (nickname == null) {
            nickname = "";
        }

        property = element.getChildTextTrim("property");

        if (property == null) {
            property = "";
        }
    }

    /**
     * This method creates a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaInstitutionName definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaInstitutionName part
     */
    @Override
    public final org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.addContent(new org.jdom2.Element("fullname").addContent(fullname));

        if ((nickname = nickname.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("nickname").addContent(nickname));
        }

        if ((property = property.trim()).length() != 0) {
            elm.addContent(new org.jdom2.Element("property").addContent(property));
        }

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     fullname: "library of congress",
     *     nickname: "LOC",
     *     property: "USA"
     *   }
     * </pre>
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        obj.addProperty("fullname", fullname);
        if (nickname != null) {
            obj.addProperty("nickname", nickname);
        }
        if (property != null) {
            obj.addProperty("property", property);
        }
        return obj;
    }

    /**
     * Validates this MCRMetaInstitutionName. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the trimmed fullname is null or empty</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaInstitutionName is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (fullname == null || (fullname = fullname.trim()).length() == 0) {
            throw new MCRException(getSubTag() + ": fullname is null or empty");
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public final MCRMetaInstitutionName clone() {
        return new MCRMetaInstitutionName(subtag, lang, type, inherited, fullname, nickname, property);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaInstitutionName other = (MCRMetaInstitutionName) obj;
        return Objects.equals(fullname, other.fullname) && Objects.equals(nickname, other.nickname)
            && Objects.equals(property, other.property);
    }

}
