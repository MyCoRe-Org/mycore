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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorSubmissionTest extends MCRTestCase {

    @Test
    public void testSubmitTextfields() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[title='Titel'][author[@firstName='John'][@lastName='Doe']]";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        Map<String, String[]> submittedValues = new HashMap<String, String[]>();
        submittedValues.put("/document/title", new String[] { "Title" });
        submittedValues.put("/document/author/@firstName", new String[] { "Jim" });
        submittedValues.put("/document/author/@lastName", new String[] { "" });
        session.getSubmission().setSubmittedValues(submittedValues);
        session.getSubmission().emptyNotResubmittedNodes();

        template = "document[title='Title'][author[@firstName='Jim'][@lastName='']]";
        Document expected = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        Document result = session.getEditedXML();
        result = MCRChangeTracker.removeChangeTracking(result);
        assertTrue(MCRXMLHelper.deepEqual(expected, result));
    }

    @Test
    public void testSubmitCheckbox() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[@archive='false']";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        session.getSubmission().setXPaths2CheckResubmission("@archive");
        session.getSubmission().emptyNotResubmittedNodes();

        assertEquals("", session.getEditedXML().getRootElement().getAttributeValue("archive"));

        session.getSubmission().setXPaths2CheckResubmission("@archive");

        Map<String, String[]> submittedValues = new HashMap<String, String[]>();
        submittedValues.put("/document/@archive", new String[] { "true" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        assertEquals("true", session.getEditedXML().getRootElement().getAttributeValue("archive"));
    }

    @Test
    public void testSubmitSelectOptions()
        throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[category='a'][category[2]='b'][category[3]='c']";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category", true, session.getRootBinding()));
        session.getSubmission().emptyNotResubmittedNodes();

        List<Element> categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("", categories.get(0).getText());
        assertEquals("", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category", true, session.getRootBinding()));

        Map<String, String[]> submittedValues = new HashMap<String, String[]>();
        submittedValues.put("/document/category", new String[] { "c", "d" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("c", categories.get(0).getText());
        assertEquals("d", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category", true, session.getRootBinding()));

        submittedValues.clear();
        submittedValues.put("/document/category", new String[] { "a", "b", "c", "d" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(4, categories.size());
        assertEquals("a", categories.get(0).getText());
        assertEquals("b", categories.get(1).getText());
        assertEquals("c", categories.get(2).getText());
        assertEquals("d", categories.get(3).getText());
    }
}
