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

package org.mycore.frontend.xeditor;

import java.io.IOException;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCREditorSubmissionTest extends MCRTestCase {

    @Test
    public void testSubmitTextfields() throws JDOMException, SAXException, JaxenException, IOException {
         new MCRXEditorTestRunner("testSubmitTextfields");
    }
    
    /*
    @Test
    public void testSubmitSingleCheckbox() throws JaxenException, JDOMException {
        String template = "document[@archive='false']";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        session.getSubmission().setXPaths2CheckResubmission("@archive");
        session.getSubmission().emptyNotResubmittedNodes();

        assertEquals("", session.getEditedXML().getRootElement().getAttributeValue("archive"));

        session.getSubmission().setXPaths2CheckResubmission("@archive");

        Map<String, String[]> submittedValues = new HashMap<>();
        submittedValues.put("/document/@archive", new String[] { "true" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        assertEquals("true", session.getEditedXML().getRootElement().getAttributeValue("archive"));
    }

    @Test
    public void testSubmitSelectSingleOption()
        throws JaxenException, JDOMException {
        String template = "document[category='a']";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[1]", true, session.getRootBinding()));
        session.getSubmission().emptyNotResubmittedNodes();

        List<Element> categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(1, categories.size());
        assertEquals("", categories.get(0).getText());

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[1]", true, session.getRootBinding()));

        Map<String, String[]> submittedValues = new HashMap<>();
        submittedValues.put("/document/category[1]", new String[] { "b" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(1, categories.size());
        assertEquals("b", categories.get(0).getText());
    }

    @Test
    public void testSubmitSelectMultipleOptions()
        throws JaxenException, JDOMException {
        String template = "document[category[1]='a'][category[2]='b'][category[3]='c']";
        MCREditorSession session = new MCREditorSession();
        session.setEditedXML(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[1]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[2]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[3]", true, session.getRootBinding()));

        session.getSubmission().emptyNotResubmittedNodes();

        List<Element> categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("", categories.get(0).getText());
        assertEquals("", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[1]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[2]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[3]", true, session.getRootBinding()));

        Map<String, String[]> submittedValues = new HashMap<>();
        submittedValues.put("/document/category", new String[] { "c", "d" });
        session.getSubmission().setSubmittedValues(submittedValues);

        session.getSubmission().emptyNotResubmittedNodes();

        categories = session.getEditedXML().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("c", categories.get(0).getText());
        assertEquals("d", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());

        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[1]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[2]", true, session.getRootBinding()));
        session.getSubmission()
            .mark2checkResubmission(new MCRBinding("/document/category[3]", true, session.getRootBinding()));

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
*/    
}
