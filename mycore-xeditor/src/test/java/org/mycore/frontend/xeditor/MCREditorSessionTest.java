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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSessionTest {

    @Test
    public void testResubmittingEditedValues() throws ParseException, JDOMException, UnsupportedEncodingException, IOException {
        MCREditorSession session = new MCREditorSession();

        // Simulate reading source XML
        Document document = buildDocument("document[title='Titel'][author[@firstName='John'][@lastName='Doe']][category='a'][category='b'][category='c']");
        session.setEditedXML(document);

        // Simulate transformation to input fields
        MCRBinding rootBinding = new MCRBinding(document);
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/title"));
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/author/@firstName"));
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/author/@lastName"));
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/category"));
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/category[2]"));
        session.markAsTransformedToInputField(xPath2Node(rootBinding, "/document/category[3]"));

        // Simulate resubmission of edited values
        session.setSubmittedValues("/document/title", new String[] { "Title" });
        session.setSubmittedValues("/document/author/@firstName", new String[] { "Jim" });
        session.setSubmittedValues("/document/category", new String[] { "a", "", "c", "d" });
        session.removeDeletedNodes();

        // Test result against expected
        Document result = buildDocument("document[title='Title'][author/@firstName='Jim'][category='a'][category='c'][category='d']");
        assertTrue(MCRXMLHelper.deepEqual(document, result));
    }

    private Document buildDocument(String xPath) throws ParseException, JDOMException {
        return new Document((Element) (MCRNodeBuilder.build(xPath, null, null, null)));
    }

    private Object xPath2Node(MCRBinding rootBinding, String xPath) throws JDOMException, ParseException {
        return new MCRBinding(xPath, rootBinding).getBoundNode();
    }
}
