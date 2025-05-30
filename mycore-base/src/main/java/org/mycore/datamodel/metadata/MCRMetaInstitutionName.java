/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

import com.google.gson.JsonObject;

/**
 * This class implements all methods for handling with the
 * MCRMetaInstitutionName part of a metadata object. The MCRMetaInstitutionName
 * class represents a name of an institution or corporation.
 *
 * @author J. Kupferschmidt
 */
public final class MCRMetaInstitutionName extends MCRMetaDefault {
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
     * The language element was set. If the value of <em>lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>subtag</em>. If the value of <em>subtag</em>
     * is null or empty an exception was throwed. The type element was set to
     * the value of <em>type</em>, if it is null, an empty string was set
     * to the type element. The fullname, nickname  and property element
     * was set to the value of <em>set_...</em>, if they are null,
     * an empty string was set to this element.
     * @param subtag      the name of the subtag
     * @param lang    the default language
     * @param type        the optional type string
     * @param inherted    a value &gt;= 0
     * @param fullname    the full name
     * @param nickname    the nickname
     * @param property       the property title
     *
     * @exception MCRException if the parameter values are invalid
     */
    public MCRMetaInstitutionName(String subtag, String lang, String type, int inherted,
        String fullname, String nickname, String property) throws MCRException {
        super(subtag, lang, type, inherted);
        this.fullname = "";
        this.nickname = "";
        this.property = "";
        set(fullname, nickname, property);
    }

    /**
     * This methode set all name componets.
     *
     * @param fullname
     *            the full name
     * @param nickname
     *            the nickname
     * @param property
     *            the property title
     */
    public void set(String fullname, String nickname, String property) {
        if (fullname == null || nickname == null || property == null) {
            throw new MCRException("One parameter is null.");
        }

        this.fullname = fullname.trim();
        this.nickname = nickname.trim();
        this.property = property.trim();
    }

    /**
     * This method get the name text element.
     *
     * @return the fullname
     */
    public String getFullName() {
        return fullname;
    }

    /**
     * This method get the nickname text element.
     *
     * @return the nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * This method get the property text element.
     *
     * @return the property
     */
    public String getProperty() {
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
    public void setFromDOM(Element element) {
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
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.addContent(new Element("fullname").addContent(fullname));

        nickname = nickname.trim();
        if (nickname.length() != 0) {
            elm.addContent(new Element("nickname").addContent(nickname));
        }

        property = property.trim();
        if (property.length() != 0) {
            elm.addContent(new Element("property").addContent(property));
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
    @Override
    public void validate() throws MCRException {
        super.validate();
        fullname = MCRUtils.filterTrimmedNotEmpty(fullname)
            .orElseThrow(() -> new MCRException(getSubTag() + ": fullname is null or empty"));
    }

    @Override
    public MCRMetaInstitutionName clone() {
        MCRMetaInstitutionName clone = (MCRMetaInstitutionName) super.clone();

        clone.fullname = this.fullname;
        clone.nickname = this.nickname;
        clone.property = this.property;

        return clone;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(fullname, nickname, property);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaInstitutionName other = (MCRMetaInstitutionName) obj;
        return Objects.equals(fullname, other.fullname)
            && Objects.equals(nickname, other.nickname)
            && Objects.equals(property, other.property);
    }

}
