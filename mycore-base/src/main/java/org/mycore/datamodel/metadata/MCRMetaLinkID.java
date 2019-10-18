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

import org.mycore.common.MCRException;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class implements all method for special handling with the MCRMetaLink
 * part of a metadata object. The MCRMetaLinkID class present two types. At once
 * a reference only of a other MCRObject. At second a bidirectional link between
 * two MCRObject's. Optional you can append the reference with the label
 * attribute. See to W3C XLink Standard for more informations.
 * <p>
 * &lt;tag class="MCRMetaLinkID"&gt;
 * <br>
 * &lt;subtag xlink:type="locator" xlink:href=" <em>MCRObjectID</em>"
 * xlink:label="..." xlink:title="..."/&gt; <br>
 * &lt;subtag xlink:type="arc" xlink:from=" <em>MCRObjectID</em>"
 * xlink:to="MCRObjectID"/&gt; <br>
 * &lt;/tag&gt; <br>
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
@JsonClassDescription("Links to other objects or derivates")
public class MCRMetaLinkID extends MCRMetaLink {
    /**
     * initializes with empty values.
     */
    public MCRMetaLinkID() {
        super();
    }

    /**
     * initializes with given values.
     * @see MCRMetaLink#MCRMetaLink(String, int)
     */
    public MCRMetaLinkID(String subtag, int inherted) {
        super(subtag, inherted);
    }

    /**
     * initializes with all values needed to link to an MCRObject.
     *
     *  This is the same as running {@link #MCRMetaLinkID(String, int)} and
     *  {@link #setReference(MCRObjectID, String, String)}.
     */
    public MCRMetaLinkID(String subtag, MCRObjectID id, String label, String title) {
        this(subtag, id, label, title, null);
    }

    /**
     * initializes with all values needed to link to an MCRObject.
     *
     */
    public MCRMetaLinkID(String subtag, MCRObjectID id, String label, String title, String role) {
        this(subtag, 0);
        setReference(id, label, title);
        setXLinkRole(role);
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     *
     * @param href
     *            the reference as MCRObjectID string
     * @param label
     *            the new label string
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the set_href value is null, empty or not a MCRObjectID
     */
    @Override
    public final void setReference(String href, String label, String title) throws MCRException {
        try {
            MCRObjectID hrefid = MCRObjectID.getInstance(href);
            super.setReference(hrefid.toString(), label, title);
        } catch (Exception e) {
            throw new MCRException("The href value is not a MCRObjectID: " + href, e);
        }
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     *
     * @param href
     *            the reference as MCRObjectID
     * @param label
     *            the new label string
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the href value is null, empty or not a MCRObjectID
     */
    public final void setReference(MCRObjectID href, String label, String title) throws MCRException {
        if (href == null) {
            throw new MCRException("The href value is null.");
        }

        super.setReference(href.toString(), label, title);
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and
     * xlink:title.
     *
     * @param from
     *            the source MCRObjectID string
     * @param to
     *            the target MCRObjectID string
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is not a MCRObjectId
     */
    @Override
    public final void setBiLink(String from, String to, String title) throws MCRException {
        try {
            MCRObjectID fromid = MCRObjectID.getInstance(from);
            MCRObjectID toid = MCRObjectID.getInstance(to);
            super.setBiLink(fromid.toString(), toid.toString(), title);
        } catch (Exception e) {
            linktype = null;
            throw new MCRException("The from/to value is not a MCRObjectID.");
        }
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and
     * xlink:title.
     *
     * @param from
     *            the source MCRObjectID
     * @param to
     *            the target MCRObjectID
     * @param title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is not a MCRObjectId
     */
    public final void setBiLink(MCRObjectID from, MCRObjectID to, String title) throws MCRException {
        if (from == null || to == null) {
            throw new MCRException("The from/to value is null.");
        }

        super.setBiLink(from.toString(), to.toString(), title);
    }

    /**
     * This method get the xlink:href element as MCRObjectID.
     *
     * @return the xlink:href element as MCRObjectID
     */
    @JsonIgnore
    public final MCRObjectID getXLinkHrefID() {
        return MCRObjectID.getInstance(href);
    }

    /**
     * This method get the xlink:from element as MCRObjectID.
     *
     * @return the xlink:from element as MCRObjectID
     */
    @JsonIgnore
    public final MCRObjectID getXLinkFromID() {
        return MCRObjectID.getInstance(from);
    }

    /**
     * This method get the xlink:to element as MCRObjectID.
     *
     * @return the xlink:to element as MCRObjectID
     */
    @JsonIgnore
    public final MCRObjectID getXLinkToID() {
        return MCRObjectID.getInstance(to);
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     *
     * @param element
     *            a relevant DOM element for the metadata
     * @exception MCRException
     *                if the xlink:type is not locator or arc or if href or from
     *                and to are not a MCRObjectID
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);

        if (linktype.equals("locator")) {
            try {
                MCRObjectID hrefid = MCRObjectID.getInstance(href);
                href = hrefid.toString();
            } catch (Exception e) {
                throw new MCRException("The xlink:href is not a MCRObjectID.");
            }
        } else {
            try {
                MCRObjectID fromid = MCRObjectID.getInstance(from);
                from = fromid.toString();
            } catch (Exception e) {
                throw new MCRException("The xlink:from is not a MCRObjectID.");
            }

            try {
                MCRObjectID toid = MCRObjectID.getInstance(to);
                to = toid.toString();
            } catch (Exception e) {
                throw new MCRException("The xlink:to is not a MCRObjectID.");
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        MCRMetaLinkID other = (MCRMetaLinkID) obj;
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
        } else {
            return Objects.equals(to, other.to);
        }
    }

    @Override
    public MCRMetaLinkID clone() {
        return (MCRMetaLinkID) super.clone();
    }

    @Override
    public final String toString() {
        return getXLinkHref();
    }
}
