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

package org.mycore.solr.index.cs;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;

/**
 * A content stream class to index a bunch of xml elements. Use the {@link #getList()}
 * method to add elements.
 * 
 * @author Matthias Eichner
 */
public class MCRSolrBulkXMLStream extends MCRSolrContentStream {

    public List<Element> elementList;

    public MCRSolrBulkXMLStream(String name) {
        super(name, null);
        this.elementList = new ArrayList<>();
    }

    public List<Element> getList() {
        return this.elementList;
    }

    @Override
    public MCRContent getSource() {
        Element objCollector = new Element("mcrObjs");
        for (Element e : this.elementList) {
            e = e.detach();
            objCollector.addContent(e);
        }
        return new MCRJDOMContent(objCollector);
    }

}
