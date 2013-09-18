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
    public void testResubmittingEditedValues() throws JaxenException, JDOMException, UnsupportedEncodingException, IOException {
        // Simulate reading source XML
        String template = "document[title='Titel'][author[@firstName='John'][@lastName='Doe']][category='a'][category='b'][category='c']";
        MCREditorStep step = new MCREditorStep(xPath2Document(template));

        // Simulate transformation to input fields
        step.markAsTransformedToInputField(step.bind("/document/title").getBoundNode());
        step.markAsTransformedToInputField(step.bind("/document/author/@firstName").getBoundNode());
        step.markAsTransformedToInputField(step.bind("/document/author/@lastName").getBoundNode());
        step.markAsTransformedToInputField(step.bind("/document/category").getBoundNode());
        step.markAsTransformedToInputField(step.bind("/document/category[2]").getBoundNode());
        step.markAsTransformedToInputField(step.bind("/document/category[3]").getBoundNode());

        // Simulate resubmission of edited values
        step.setSubmittedValues("/document/title", new String[] { "Title" });
        step.setSubmittedValues("/document/author/@firstName", new String[] { "Jim" });
        step.setSubmittedValues("/document/category", new String[] { "a", "", "c", "d" });
        step.emptyNotResubmittedNodes();

        // Test result against expected
        template = "document[title='Title'][author[@firstName='Jim'][@lastName='']][category='a'][category=''][category='c'][category='d']";
        Document expected = xPath2Document(template);
        assertTrue(MCRXMLHelper.deepEqual(expected, step.getDocument()));
    }

    private Document xPath2Document(String xPath) throws JaxenException {
        Element root = (Element) (MCRNodeBuilder.build(xPath, null, null, null));
        return new Document(root);
    }
}
