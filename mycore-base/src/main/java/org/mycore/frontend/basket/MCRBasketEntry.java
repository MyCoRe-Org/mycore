/*
 * $Revision$ 
 * $Date$
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

package org.mycore.frontend.basket;

import org.jdom.Element;
import org.mycore.common.xml.MCRURIResolver;

public class MCRBasketEntry {

    private String id;

    private String uri;

    private String comment;

    private Element content;

    public MCRBasketEntry(String id, String uri) {
        this.id = id;
        this.uri = uri;
    }

    public String getID() {
        return id;
    }

    public String getURI() {
        return uri;
    }

    public void resolveContent() {
        setContent(MCRURIResolver.instance().resolve(uri));
    }

    public Element getContent() {
        return content;
    }

    public void setContent(Element content) {
        this.content = (Element) (content.clone());
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Element buildXML(boolean addContent) {
        Element entry = new Element("entry");
        entry.setAttribute("id", id);
        entry.setAttribute("uri", uri);
        if (addContent && (content != null))
            entry.addContent((Element) (content.clone()));
        entry.addContent(new Element("comment").setText(comment));
        return entry;
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
