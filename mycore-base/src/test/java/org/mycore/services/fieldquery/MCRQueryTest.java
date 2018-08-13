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

import static org.junit.Assert.*;

import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRXMLHelper;

public class MCRQueryTest extends MCRTestCase  {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }
    @Test
    public final void testQueryAsXML() {
        Document doc = new Document();
        Element query = new Element("query");
        query.setAttribute("maxResults", "50");
        query.setAttribute("numPerPage", "25");
        doc.addContent(query);
        Element sortby = new Element("sortBy");
        query.addContent(sortby);
        Element sortfield = new Element("field");
        sortfield.setAttribute("name", "dict01_sort");
        sortfield.setAttribute("order", "ascending");
        sortby.addContent(sortfield);
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
        condition01.setAttribute("operator", "=");
        condition01.setAttribute("value", "viaf");
        bool.addContent(condition01);
        Element condition02 = new Element("condition");
        condition02.setAttribute("field", "title");
        condition02.setAttribute("operator", "contains");
        condition02.setAttribute("value", "Amt");
        bool.addContent(condition02);
        Element not = new Element("boolean");
        not.setAttribute("operator", "not");
        bool.addContent(not);
        Element condition03 = new Element("condition");
        condition03.setAttribute("field", "title");
        condition03.setAttribute("operator", "contains");
        condition03.setAttribute("value", "Ehre");
        not.addContent(condition03);
        try {
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(doc, System.out);
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            MCRQuery mcrquery = MCRQuery.parseXML(doc);
            Document mcrquerydoc = mcrquery.buildXML();
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            xmlOutputter.output(mcrquerydoc, System.out);
            System.out.println();
            assertTrue("Elements should be equal", MCRXMLHelper.deepEqual(doc, mcrquerydoc));
            } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
}
