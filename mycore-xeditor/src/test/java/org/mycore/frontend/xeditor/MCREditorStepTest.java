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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREditorStepTest {

    @Test
    public void testSubmitTextfields() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[title='Titel'][author[@firstName='John'][@lastName='Doe']]";
        MCREditorStep step = new MCREditorStep(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        step.setSubmittedValues("/document/title", new String[] { "Title" });
        step.setSubmittedValues("/document/author/@firstName", new String[] { "Jim" });
        step.setSubmittedValues("/document/author/@lastName", new String[] { "" });
        step.emptyNotResubmittedNodes();

        template = "document[title='Title'][author[@firstName='Jim'][@lastName='']]";
        Document expected = new Document(new MCRNodeBuilder().buildElement(template, null, null));
        Document result = step.getDocument();
        MCRChangeTracker.removeChangeTracking(result);
        assertTrue(MCRXMLHelper.deepEqual(expected, result));
    }

    @Test
    public void testSubmitCheckbox() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[@archive='false']";
        MCREditorStep step = new MCREditorStep(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        step.setXPaths2CheckResubmission(new String[] { "/document/@archive" });
        step.emptyNotResubmittedNodes();

        assertEquals("", step.getDocument().getRootElement().getAttributeValue("archive"));

        step.setXPaths2CheckResubmission(new String[] { "/document/@archive" });
        step.setSubmittedValues("/document/@archive", new String[] { "true" });
        step.emptyNotResubmittedNodes();

        assertEquals("true", step.getDocument().getRootElement().getAttributeValue("archive"));
    }

    @Test
    public void testSubmitSelectOptions() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        String template = "document[category='a'][category[2]='b'][category[3]='c']";
        MCREditorStep step = new MCREditorStep(new Document(new MCRNodeBuilder().buildElement(template, null, null)));

        step.mark2checkResubmission(new MCRBinding("/document/category", step.getRootBinding()));
        step.emptyNotResubmittedNodes();

        List<Element> categories = step.getDocument().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("", categories.get(0).getText());
        assertEquals("", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());

        step.mark2checkResubmission(new MCRBinding("/document/category", step.getRootBinding()));
        step.setSubmittedValues("/document/category", new String[] { "c", "d" });
        step.emptyNotResubmittedNodes();

        categories = step.getDocument().getRootElement().getChildren("category");
        assertEquals(3, categories.size());
        assertEquals("c", categories.get(0).getText());
        assertEquals("d", categories.get(1).getText());
        assertEquals("", categories.get(2).getText());
        
        step.mark2checkResubmission(new MCRBinding("/document/category", step.getRootBinding()));
        step.setSubmittedValues("/document/category", new String[] { "a", "b", "c", "d" });
        step.emptyNotResubmittedNodes();

        categories = step.getDocument().getRootElement().getChildren("category");
        assertEquals(4, categories.size());
        assertEquals("a", categories.get(0).getText());
        assertEquals("b", categories.get(1).getText());
        assertEquals("c", categories.get(2).getText());
        assertEquals("d", categories.get(3).getText());
    }
}
