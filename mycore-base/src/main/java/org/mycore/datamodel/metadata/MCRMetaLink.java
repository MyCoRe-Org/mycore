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

import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.util.XMLChar;
import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This class implements all method for generic handling with the MCRMetaLink part of a metadata object. The MCRMetaLink class present two types. At once a reference to an URL. At second a bidirectional link between two URL's. Optional you can append the reference with the label attribute. See to W3C XLink Standard for more informations.
 * <p>
 * &lt;tag class="MCRMetaLink"&gt; <br>
 * &lt;subtag xlink:type="locator" xlink:href=" <em>URL</em>" xlink:label="..." xlink:title="..."/&gt; <br>
 * &lt;subtag xlink:type="arc" xlink:from=" <em>URL</em>" xlink:to="URL"/&gt; <br>
 * &lt;/tag&gt; <br>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRMetaLink extends MCRMetaDefault {
    // MetaLink data
    protected String href;

    protected String label;

    protected String title;

    protected String linktype;

    protected String role;

    protected String from;

    protected String to;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts was set to an empty value.
     */
    public MCRMetaLink() {
        super();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is null, empty or false <b>en </b> was set. The subtag element was set to the value of <em>set_subtag</em>. If the value of <em>set_subtag</em> is null or empty an exception was throwed.
     * @param set_subtag
     *            the name of the subtag
     * @param set_inherted
     *            a value &gt;= 0
     * @exception MCRException
     *                if the set_datapart or set_subtag value is null or empty
     */
    public MCRMetaLink(String set_subtag, int set_inherted) throws MCRException {
        super(set_subtag, null, null, set_inherted);
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     * 
     * @param set_href
     *            the reference
     * @param set_label
     *            the new label string
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the set_href value is null or empty
     */
    public void setReference(String set_href, String set_label, String set_title) throws MCRException {
        linktype = "locator";

        if (set_href == null || (set_href = set_href.trim()).length() == 0) {
            throw new MCRException("The href value is null or empty.");
        }

        href = set_href;
        setXLinkLabel(set_label);
        title = set_title;
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and xlink:title.
     * 
     * @param set_from
     *            the source
     * @param set_to
     *            the target
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is null or empty
     */
    public void setBiLink(String set_from, String set_to, String set_title) throws MCRException {
        if (set_from == null || (set_from = set_from.trim()).length() == 0) {
            throw new MCRException("The from value is null or empty.");
        }
        if (set_to == null || (set_to = set_to.trim()).length() == 0) {
            throw new MCRException("The to value is null or empty.");
        }
        linktype = "arc";
        from = set_from;
        to = set_to;
        title = set_title;
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
     * This method sets the xlink:role.
     * 
     */
    public void setXLinkRole(String role) {
        this.role = role;
    }

    /**
     * This method get the xlink:role element as string.
     * 
     */
    public String getXLinkRole() {
        return role;
    }

    /**
     * The method compare this instance of MCRMetaLink with a input object of the class type MCRMetaLink. The both instances are equal, if: <br>
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
        if (linktype.equals("locator")) {
            if (linktype.equals(input.getXLinkType()) && href.equals(input.getXLinkHref())) {
                return true;
            }
        }

        if (linktype.equals("arc")) {
            if (linktype.equals(input.getXLinkType()) && from.equals(input.getXLinkFrom())
                && to.equals(input.getXLinkTo())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), from, href, label, linktype, role, title, to);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        MCRMetaLink other = (MCRMetaLink) obj;
        if (!Objects.equals(from, other.from)) {
            return false;
        } else if (!Objects.equals(href, other.href)) {
            return false;
        } else if (!Objects.equals(label, other.label)) {
            return false;
        } else if (!Objects.equals(linktype, other.linktype)) {
            return false;
        } else if (!Objects.equals(role, other.role)) {
            return false;
        } else if (!Objects.equals(title, other.title)) {
            return false;
        } else if (!Objects.equals(to, other.to)) {
            return false;
        }
        return true;
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
    public void setFromDOM(org.jdom2.Element element) throws MCRException {
        super.setFromDOM(element);

        String temp = element.getAttributeValue("type", XLINK_NAMESPACE);

        if (temp != null && (temp.equals("locator") || temp.equals("arc"))) {
            linktype = temp;
        } else {
            throw new MCRException("The xlink:type is not locator or arc.");
        }

        if (linktype.equals("locator")) {
            String temp1 = element.getAttributeValue("href", XLINK_NAMESPACE);
            String temp2 = element.getAttributeValue("label", XLINK_NAMESPACE);
            String temp3 = element.getAttributeValue("title", XLINK_NAMESPACE);
            setReference(temp1, temp2, temp3);
        } else {
            String temp1 = element.getAttributeValue("from", XLINK_NAMESPACE);
            String temp2 = element.getAttributeValue("to", XLINK_NAMESPACE);
            String temp3 = element.getAttributeValue("title", XLINK_NAMESPACE);
            setBiLink(temp1, temp2, temp3);
        }

        setXLinkRole(element.getAttributeValue("role", XLINK_NAMESPACE));
    }

    /**
     * This method create a XML stream for all data in this class, defined by the MyCoRe XML MCRMetaLink definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaLink part
     */
    @Override
    public org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.setAttribute("type", linktype, XLINK_NAMESPACE);

        if (title != null) {
            elm.setAttribute("title", title, XLINK_NAMESPACE);
        }

        if (label != null) {
            elm.setAttribute("label", label, XLINK_NAMESPACE);
        }

        if (role != null) {
            elm.setAttribute("role", role, XLINK_NAMESPACE);
        }

        if (linktype.equals("locator")) {
            elm.setAttribute("href", href, XLINK_NAMESPACE);
        } else {
            elm.setAttribute("from", from, XLINK_NAMESPACE);
            elm.setAttribute("to", to, XLINK_NAMESPACE);
        }

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
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
            obj.addProperty("title", title);
        }
        if (label != null) {
            obj.addProperty("label", label);
        }
        if (role != null) {

        }
        if (linktype.equals("locator")) {
            obj.addProperty("href", href);
        } else {
            obj.addProperty("from", from);
            obj.addProperty("to", to);
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
    public void validate() throws MCRException {
        super.validate();
        if (label != null && label.length() > 0) {
            if (!XMLChar.isValidNCName(label)) {
                throw new MCRException(getSubTag() + ": label is no valid NCName:" + label);
            }
        }
        if (linktype == null) {
            throw new MCRException(getSubTag() + ": linktype is null");
        }
        if (!linktype.equals("locator") && !linktype.equals("arc")) {
            throw new MCRException(getSubTag() + ": linktype is unsupported: " + linktype);
        }
        if (linktype.equals("arc")) {
            if (from == null || from.length() == 0) {
                throw new MCRException(getSubTag() + ": from is null or empty");
            } else if (!XMLChar.isValidNCName(from)) {
                throw new MCRException(getSubTag() + ": from is no valid NCName:" + from);
            }

            if (to == null || to.length() == 0) {
                throw new MCRException(getSubTag() + ": to is null or empty");
            } else if (!XMLChar.isValidNCName(to)) {
                throw new MCRException(getSubTag() + ": to is no valid NCName:" + to);
            }
        }
        if (linktype.equals("locator")) {
            if (href == null || href.length() == 0) {
                throw new MCRException(getSubTag() + ": href is null or empty");
            }
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public final MCRMetaLink clone() {
        MCRMetaLink out = new MCRMetaLink(subtag, inherited);
        out.linktype = linktype;
        out.title = title;
        out.type = type;
        out.href = href;
        out.role = role;
        out.to = to;
        out.from = from;

        return out;
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        if (LOGGER.isDebugEnabled()) {
            super.debugDefault();
            LOGGER.debug("Link Type          = " + linktype);
            LOGGER.debug("Label              = " + label);
            LOGGER.debug("Title              = " + title);
            LOGGER.debug("HREF               = " + href);
            LOGGER.debug("Role               = " + role);
            LOGGER.debug("From               = " + from);
            LOGGER.debug("To                 = " + to);
            LOGGER.debug("");
        }
    }
}
