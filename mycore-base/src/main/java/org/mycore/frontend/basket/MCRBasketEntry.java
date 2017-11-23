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

package org.mycore.frontend.basket;

import org.jdom2.Element;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Represents an entry in a basket. Each entry has at least
 * a unique ID, for example the MCRObjectID, and an URI that
 * can be used to read the object's XML representation to
 * render the object in the basket UI. Each basket entry
 * may have an optional comment. The basket entry provides 
 * methods to resolve the object's XML and to set in in the
 * basket entry. This can be used by applications that wish
 * to edit XML in the basket itself.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRBasketEntry {

    /** The ID of the object contained in this basket entry */
    private String id;

    /** The URI where to read the object's XML data from */
    private String uri;

    /** Optional comment for this basket entry */
    private String comment;

    /** The XML data of the object in the basket, read from the URI */
    private Element content;

    /**
     * Creates a new basket entry. The XML that represents the object
     * is not immediately read from the given URI. Call resolveContent() to
     * read the content. 
     * 
     * @param id the ID of the object to add to the basket.
     * @param uri the URI where to read the object's xml data from
     */
    public MCRBasketEntry(String id, String uri) {
        this.id = id;
        this.uri = uri;
    }

    /** Returns the ID of the object contained in this basket entry */
    public String getID() {
        return id;
    }

    /** Returns the URI where to read the object's XML data from */
    public String getURI() {
        return uri;
    }

    /** 
     * Reads the XML data of the object in the basket entry, using the given URI, 
     * and stores it in the basket entry.
     */
    public void resolveContent() {
        if ((uri != null) && !uri.isEmpty())
            setContent(MCRURIResolver.instance().resolve(uri));
    }

    /**
     * Returns the XML data of the object in the basket entry, or null if 
     * setContent() or resolveContent() was not called yet.
     */
    public Element getContent() {
        return content;
    }

    /**
     * Sets the XML data of the object in the basket entry.
     */
    public void setContent(Element content) {
        this.content = content.clone();
    }

    /**
     * Returns the optional comment set for this basket entry.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the optional comment for this basket entry.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRBasketEntry)
            return ((MCRBasketEntry) obj).id.equals(id);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
