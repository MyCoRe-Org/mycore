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

package org.mycore.services.fieldquery;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

public class MCRQueryTest {

    @Test
    public final void testQueryAsXML() {
        Document doc = new Document();
        Element query = new Element("query");
        query.setAttribute("maxResults", "0");
        query.setAttribute("numPerPage", "10");
        doc.addContent(query);
        Element return_fields = new Element("returnFields");
        return_fields.setText("id,returnId,objectType");
        query.addContent(return_fields);
        Element conditions = new Element("conditions");
        conditions.setAttribute("format", "xml");
        query.addContent(conditions);
        Element bool = new Element("boolean");
        bool.setAttribute("operator", "and");
        conditions.addContent(bool);
        Element condition01 = new Element("condition");
        condition01.setAttribute("field", "objectType");
        condition01.setAttribute("opertor", "=");
        condition01.setAttribute("value", "viaf");
        bool.addContent(condition01);
        Element condition02 = new Element("condition");
        condition02.setAttribute("field", "title");
        condition02.setAttribute("opertor", "contains");
        condition02.setAttribute("value", "Amt");
        bool.addContent(condition02);
        Element sortby = new Element("sortBy");
        query.addContent(sortby);
        Element sortfield = new Element("field");
        sortfield.setAttribute("name", "dict01_sort");
        sortfield.setAttribute("order", "ascending");
        sortby.addContent(sortfield);
        try {
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(doc, System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //MCRQuery mcrquery = MCRQuery.parseXML(doc);
    }
    
}
