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

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.utils.XMLChar;
import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

import com.google.gson.JsonObject;
import org.mycore.common.MCRXlink;

/**
 * This class implements all method for generic handling with the MCRMetaLink part of a metadata object.
 * The MCRMetaLink class present two types. At once a reference to an URL.
 * At second a bidirectional link between two URL's. Optional you can append the reference with the label attribute.
 * See to W3C XLink Standard for more informations.
 * <p>
 * &lt;tag class="MCRMetaLink"&gt; <br>
 * &lt;subtag xlink:type="locator" xlink:href=" <em>URL</em>" xlink:label="..." xlink:title="..."/&gt; <br>
 * &lt;subtag xlink:type="arc" xlink:from=" <em>URL</em>" xlink:to="URL"/&gt; <br>
 * &lt;/tag&gt; <br>
 *
 * @author Jens Kupferschmidt
 */
public class MCRMetaLink extends MCRMetaDefault {

    private static final Logger LOGGER = LogManager.getLogger();

    // MetaLink data
    protected String href;
    protected String label;
    protected String title;
    protected String linktype;
    protected String role;
    protected String from;
    protected String to;

    /**
     * initializes with empty values.
     */
    public MCRMetaLink() {
        super();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is null, empty or false <b>en </b> was set.
     * The subtag element was set to the value of <em>subtag</em>.
     * If the value of <em>subtag</em> is null or empty an exception was throwed.
     * @param subtag
     *            the name of the subtag
     * @param inherited
     *            a value &gt;= 0
     * @exception MCRException
     *                if the set_datapart or subtag value is null or empty
     */
    public MCRMetaLink(String subtag, int inherited) throws MCRException {
        super(subtag, null, null, inherited);
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     *
     * @param href
     *            the reference
     * @param label
     *            the new label string
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the href value is null or empty
     */
    public void setReference(String href, String label, String title) throws MCRException {
        linktype = MCRXlink.TYPE_LOCATOR;

        this.href = MCRUtils.filterTrimmedNotEmpty(href)
            .orElseThrow(() -> new MCRException("The href value is null or empty."));
        setXLinkLabel(label);
        this.title = title;
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and xlink:title.
     *
     * @param from
     *            the source
     * @param to
     *            the target
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is null or empty
     */
    public void setBiLink(String from, String to, String title) throws MCRException {
        this.from = MCRUtils.filterTrimmedNotEmpty(from)
            .orElseThrow(() -> new MCRException("The from value is null or empty."));
        this.to = MCRUtils.filterTrimmedNotEmpty(to)
            .orElseThrow(() -> new MCRException("The to value is null or empty."));
        linktype = MCRXlink.TYPE_ARC;
        this.title = title;
    }

    /**
     * This method get the xlink:type element.
     *
     * @return the xlink:type
     */
    public final String getXLinkType() {
        return linktype;
    }

    /**
     * This method get the xlink:href element as string.
     *
     * @return the xlink:href element as string
     */
    public final String getXLinkHref() {
        return href;
    }

    /**
     * This method get the xlink:label element.
     *
     * @return the xlink:label
     */
    public final String getXLinkLabel() {
        return label;
    }

    /**
     * This method set the xlink:label
     *
     * @param label
     *            the xlink:label
     */
    public final void setXLinkLabel(String label) {
        if (label != null && !XMLChar.isValidNCName(label)) {
            throw new MCRException("xlink:label is not a valid NCName: " + label);
        }
        this.label = label;
    }

    /**
     * This method get the xlink:title element.
     *
     * @return the xlink:title
     */
    public final String getXLinkTitle() {
        return title;
    }

    /**
     * This method set the xlink:title
     *
     * @param title
     *            the xlink:title
     */
    public final void setXLinkTitle(String title) {
        this.title = title;
    }

    /**
     * This method get the xlink:from element as string.
     *
     * @return the xlink:from element as string
     */
    public final String getXLinkFrom() {
        return from;
    }

    /**
     * This method get the xlink:to element as string.
     *
     * @return the xlink:to element as string
     */
    public final String getXLinkTo() {
        return to;
    }

    /**
     * This method get the xlink:role element as string.
     *
     */
    public String getXLinkRole() {
        return role;
    }

    /**
     * This method sets the xlink:role.
     *
     */
    public void setXLinkRole(String role) {
        this.role = role;
    }

    /**
     * The method compare this instance of MCRMetaLink with a input object of the class type MCRMetaLink.
     * The both instances are equal, if: <br>
     * <ul>
     * <li>for the type 'arc' the 'from' and 'to' element is equal</li>
     * <li>for the type 'locator' the 'href' element is equal</li>
     * </ul>
     * <br>
     *
     * @param input
     *            the MCRMetaLink input
     * @return true if it is compare, else return false
     */
    public final boolean compare(MCRMetaLink input) {
        if (linktype.equals(MCRXlink.TYPE_LOCATOR) && linktype.equals(input.getXLinkType())
            && href.equals(input.getXLinkHref())) {
            return true;
        }
        if (linktype.equals(MCRXlink.TYPE_ARC)) {
            return linktype.equals(input.getXLinkType())
                && from.equals(input.getXLinkFrom())
                && to.equals(input.getXLinkTo());
        }
        return false;
    }

    /**
     * This method read the XML input stream part from a DOM part for the metadata of the document.
     *
     * @param element
     *            a relevant DOM element for the metadata
     * @exception MCRException
     *                if the xlink:type is not locator or arc or if href or from and to are null or empty
     */
    @Override
    public void setFromDOM(Element element) throws MCRException {
        super.setFromDOM(element);

        String temp = element.getAttributeValue("type", XLINK_NAMESPACE);

        if (temp != null && (temp.equals(MCRXlink.TYPE_LOCATOR) || temp.equals(MCRXlink.TYPE_ARC))) {
            linktype = temp;
        } else {
            throw new MCRException("The xlink:type is not locator or arc.");
        }

        if (linktype.equals(MCRXlink.TYPE_LOCATOR)) {
            String temp1 = element.getAttributeValue(MCRXlink.HREF, XLINK_NAMESPACE);
            String temp2 = element.getAttributeValue(MCRXlink.LABEL, XLINK_NAMESPACE);
            String temp3 = element.getAttributeValue(MCRXlink.TITLE, XLINK_NAMESPACE);
            setReference(temp1, temp2, temp3);
        } else {
            String temp1 = element.getAttributeValue(MCRXlink.FROM, XLINK_NAMESPACE);
            String temp2 = element.getAttributeValue(MCRXlink.TO, XLINK_NAMESPACE);
            String temp3 = element.getAttributeValue(MCRXlink.TITLE, XLINK_NAMESPACE);
            setBiLink(temp1, temp2, temp3);
        }

        setXLinkRole(element.getAttributeValue("role", XLINK_NAMESPACE));
    }

    /**
     * This method create a XML stream for all data in this class,
     * defined by the MyCoRe XML MCRMetaLink definition for the given subtag.
     *
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaLink part
     */
    @Override
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.setAttribute(MCRXlink.TYPE, linktype, XLINK_NAMESPACE);

        if (title != null) {
            elm.setAttribute(MCRXlink.TITLE, title, XLINK_NAMESPACE);
        }

        if (label != null) {
            elm.setAttribute(MCRXlink.LABEL, label, XLINK_NAMESPACE);
        }

        if (role != null) {
            elm.setAttribute(MCRXlink.ROLE, role, XLINK_NAMESPACE);
        }

        if (linktype.equals(MCRXlink.TYPE_LOCATOR)) {
            elm.setAttribute(MCRXlink.HREF, href, XLINK_NAMESPACE);
        } else {
            elm.setAttribute(MCRXlink.FROM, from, XLINK_NAMESPACE);
            elm.setAttribute(MCRXlink.TO, to, XLINK_NAMESPACE);
        }

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * <p>
     * For linktype equals 'locator':
     * <pre>
     *   {
     *     label: "MyCoRe Derivate Image",
     *     title: "MyCoRe Derivate Image",
     *     role: "image_reference",
     *     href: "mycore_derivate_00000001/image.tif"
     *   }
     * </pre>
     *
     * For all other linktypes (arc):
     * <pre>
     *   {
     *     label: "Link between Issue and Person",
     *     title: "Link between Issue and Person",
     *     role: "link",
     *     from: "mycore_issue_00000001",
     *     to: "mycore_person_00000001"
     *   }
     * </pre>
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        if (title != null) {
            obj.addProperty(MCRXlink.TITLE, title);
        }
        if (label != null) {
            obj.addProperty(MCRXlink.LABEL, label);
        }
        if (role != null) {
            obj.addProperty(MCRXlink.ROLE, role);
        }
        if (linktype.equals(MCRXlink.TYPE_LOCATOR)) {
            obj.addProperty(MCRXlink.HREF, href);
        } else {
            obj.addProperty(MCRXlink.FROM, from);
            obj.addProperty(MCRXlink.TO, to);
        }
        return obj;
    }

    /**
     * Validates this MCRMetaLink. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the xlink:type not "locator" or "arc"</li>
     * <li>the from or to are not valid</li>
     * </ul>
     *
     * @throws MCRException the MCRMetaLink is invalid
     */
    @Override
    public void validate() throws MCRException {
        super.validate();
        if (label != null && !label.isEmpty() && !XMLChar.isValidNCName(label)) {
            throw new MCRException(getSubTag() + ": label is no valid NCName:" + label);
        }
        if (linktype == null) {
            throw new MCRException(getSubTag() + ": linktype is null");
        }
        if (!linktype.equals(MCRXlink.TYPE_LOCATOR) && !linktype.equals(MCRXlink.TYPE_ARC)) {
            throw new MCRException(getSubTag() + ": linktype is unsupported: " + linktype);
        }
        if (linktype.equals(MCRXlink.TYPE_ARC)) {
            throwMCRExceptionIfNullOrInvalid(from);
            throwMCRExceptionIfNullOrInvalid(to);
        }
        if (linktype.equals(MCRXlink.TYPE_LOCATOR) && (href == null || href.isEmpty())) {
            throw new MCRException(getSubTag() + ": href is null or empty");
        }
    }

    private void throwMCRExceptionIfNullOrInvalid(String string) {
        if (string == null || string.isEmpty()) {
            throw new MCRException(getSubTag() + ": is null or empty");
        } else if (!XMLChar.isValidNCName(string)) {
            throw new MCRException(getSubTag() + ": is no valid NCName:" + string);
        }
    }

    @Override
    public MCRMetaLink clone() {
        MCRMetaLink clone = (MCRMetaLink) super.clone();

        clone.href = this.href;
        clone.label = this.label;
        clone.title = this.title;
        clone.linktype = this.linktype;
        clone.role = this.role;
        clone.from = this.from;
        clone.to = this.to;

        return clone;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Link Type          = {}", linktype);
            LOGGER.debug("Label              = {}", label);
            LOGGER.debug("Title              = {}", title);
            LOGGER.debug("HREF               = {}", href);
            LOGGER.debug("Role               = {}", role);
            LOGGER.debug("From               = {}", from);
            LOGGER.debug("To                 = {}", to);
            LOGGER.debug("");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(from, href, label, linktype, role, title, to);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaLink other = (MCRMetaLink) obj;
        return Objects.equals(from, other.from)
            && Objects.equals(href, other.href)
            && Objects.equals(label, other.label)
            && Objects.equals(linktype, other.linktype)
            && Objects.equals(role, other.role)
            && Objects.equals(title, other.title)
            && Objects.equals(to, other.to);
    }
}
