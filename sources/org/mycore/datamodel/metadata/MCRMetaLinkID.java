/*
 * $RCSfile$
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

import org.mycore.common.MCRException;

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
final public class MCRMetaLinkID extends MCRMetaLink {
    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts was set to
     * an empty value.
     */
    public MCRMetaLinkID() {
        super();
    }

    /**
     * This is the constructor. <br>
     * The language element was set. If the value of <em>default_lang</em> is
     * null, empty or false <b>en </b> was set. The subtag element was set to
     * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
     * is null or empty an exception was throwed.
     *
     * @param set_datapart     the global part of the elements like 'metadata'
     *                         or 'service' or so
     * @param set_subtag       the name of the subtag
     * @param default_lang     the default language
     * @param set_inherted     a value >= 0
     * @exception MCRException if the set_datapart or set_subtag value is null or
     * empty
     */
    public MCRMetaLinkID(String set_datapart, String set_subtag, String default_lang, int set_inherted) throws MCRException {
        super(set_datapart, set_subtag, default_lang, set_inherted);
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     * 
     * @param set_href
     *            the reference as MCRObjectID string
     * @param set_label
     *            the new label string
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the set_href value is null, empty or not a MCRObjectID
     */
    public final void setReference(String set_href, String set_label, String set_title) throws MCRException {
        try {
            MCRObjectID hrefid = new MCRObjectID(set_href);
            super.setReference(hrefid.getId(), set_label, set_title);
        } catch (Exception e) {
            throw new MCRException("The href value is not a MCRObjectID: " + set_href, e);
        }
    }

    /**
     * This method set a reference with xlink:href, xlink:label and xlink:title.
     * 
     * @param set_href
     *            the reference as MCRObjectID
     * @param set_label
     *            the new label string
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the set_href value is null, empty or not a MCRObjectID
     */
    public final void setReference(MCRObjectID set_href, String set_label, String set_title) throws MCRException {
        if (set_href == null) {
            throw new MCRException("The href value is null.");
        }

        super.setReference(set_href.getId(), set_label, set_title);
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and
     * xlink:title.
     * 
     * @param set_from
     *            the source MCRObjectID string
     * @param set_to
     *            the target MCRObjectID string
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is not a MCRObjectId
     */
    public final void setBiLink(String set_from, String set_to, String set_title) throws MCRException {
        try {
            MCRObjectID fromid = new MCRObjectID(set_from);
            MCRObjectID toid = new MCRObjectID(set_to);
            super.setBiLink(fromid.getId(), toid.getId(), set_title);
        } catch (Exception e) {
            linktype = null;
            throw new MCRException("The from/to value is not a MCRObjectID.");
        }
    }

    /**
     * This method set a bidirectional link with xlink:from, xlink:to and
     * xlink:title.
     * 
     * @param set_from
     *            the source MCRObjectID
     * @param set_to
     *            the target MCRObjectID
     * @param set_title
     *            the new title string
     * @exception MCRException
     *                if the from or to element is not a MCRObjectId
     */
    public final void setBiLink(MCRObjectID set_from, MCRObjectID set_to, String set_title) throws MCRException {
        if ((set_from == null) || (set_to == null)) {
            throw new MCRException("The from/to value is null.");
        }

        super.setBiLink(set_from.getId(), set_to.getId(), set_title);
    }

    /**
     * This method get the xlink:href element as MCRObjectID.
     * 
     * @return the xlink:href element as MCRObjectID
     */
    public final MCRObjectID getXLinkHrefID() {
        return new MCRObjectID(href);
    }

    /**
     * This method get the xlink:from element as MCRObjectID.
     * 
     * @return the xlink:from element as MCRObjectID
     */
    public final MCRObjectID getXLinkFromID() {
        return new MCRObjectID(from);
    }

    /**
     * This method get the xlink:to element as MCRObjectID.
     * 
     * @return the xlink:to element as MCRObjectID
     */
    public final MCRObjectID getXLinkToID() {
        return new MCRObjectID(to);
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
    public final void setFromDOM(org.jdom.Element element) {
        super.setFromDOM(element);

        if (linktype.equals("locator")) {
            try {
                MCRObjectID hrefid = new MCRObjectID(href);
                href = hrefid.getId();
            } catch (Exception e) {
                throw new MCRException("The xlink:href is not a MCRObjectID.");
            }
        } else {
            try {
                MCRObjectID fromid = new MCRObjectID(from);
                from = fromid.getId();
            } catch (Exception e) {
                throw new MCRException("The xlink:from is not a MCRObjectID.");
            }

            try {
                MCRObjectID toid = new MCRObjectID(to);
                to = toid.getId();
            } catch (Exception e) {
                throw new MCRException("The xlink:to is not a MCRObjectID.");
            }
        }
    }
}
