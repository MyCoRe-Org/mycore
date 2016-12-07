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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.xml.transform.TransformerException;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformerTest extends MCRTestCase {

    private MCREditorSession buildEditorSession(String editedXMLFile) {
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        if (editedXMLFile != null) {
            parameters.put("input", new String[] { editedXMLFile });
        }
        MCRParameterCollector collector = new MCRParameterCollector(false);
        collector.setParameter("input", editedXMLFile);
        MCREditorSession editorSession = new MCREditorSession(parameters, collector);
        editorSession.setID("1");
        return editorSession;
    }

    private MCREditorSession testTransformation(String inputFile, String editedXMLFile, String expectedOutputFile)
        throws TransformerException, IOException, JDOMException, SAXException, JaxenException {
        MCREditorSession editorSession = buildEditorSession(editedXMLFile);
        testTransformation(inputFile, editedXMLFile, editorSession, expectedOutputFile);
        return editorSession;
    }

    private void testTransformation(String inputFile, String editedXMLFile, MCREditorSession session,
        String expectedOutputFile) throws TransformerException, IOException, JDOMException, SAXException,
        JaxenException {
        MCRParameterCollector pc = new MCRParameterCollector(false);
        if (editedXMLFile != null) {
            pc.setParameter("input", editedXMLFile);
        }

        MCRContent input = MCRSourceContent.getInstance("resource:" + inputFile);
        MCRContent transformed = new MCRXEditorTransformer(session, pc).transform(input);

        Document expected = MCRSourceContent.getInstance("resource:" + expectedOutputFile).asXML();
        MCRBinding binding = new MCRBinding("//input[@type='hidden'][@name='_xed_session']/@value", true,
            new MCRBinding(expected));
        binding.setValue(session.getID() + "-" + session.getChangeTracker().getChangeCounter());

        String msg = "Transformed output is different to " + expectedOutputFile;
        boolean isEqual = MCRXMLHelper.deepEqual(expected, transformed.asXML());
        if (!isEqual)
            System.out.println(transformed.asString());
        assertTrue(msg, isEqual);
    }

    @Test
    public void testBasicInputComponents() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        testTransformation("testBasicInputComponents-editor.xml", null, "testBasicInputComponents-transformed1.xml");
        testTransformation("testBasicInputComponents-editor.xml", "testBasicInputComponents-source.xml",
            "testBasicInputComponents-transformed2.xml");
    }

    @Test
    public void testIncludes() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        testTransformation("testIncludes-editor.xml", null, "testIncludes-transformed.xml");
    }

    @Test
    public void testRepeats() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        testTransformation("testRepeats-editor.xml", "testBasicInputComponents-source.xml",
            "testRepeats-transformed.xml");
    }

    @Test
    public void testXPathSubstitution() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        MCRSessionMgr.getCurrentSession().put("SomeUser", "John Doe");
        testTransformation("testXPathSubstitution-editor.xml", "testBasicInputComponents-source.xml",
            "testXPathSubstitution-transformed.xml");
    }

    @Test
    public void testNamespaces() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        testTransformation("testNamespaces-editor.xml", "testNamespaces-source.xml", "testNamespaces-transformed.xml");
    }

    @Test
    public void testConditions() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        MCRSessionMgr.getCurrentSession().put("switch", "on");
        MCRSessionMgr.getCurrentSession().put("case", "2");
        testTransformation("testConditions-editor.xml", "testBasicInputComponents-source.xml",
            "testConditions-transformed.xml");
    }

    @Test
    public void testI18N() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException,
        JaxenException {
        MCRSessionMgr.getCurrentSession().setCurrentLanguage("en");
        testTransformation("testI18N-editor.xml", "testBasicInputComponents-source.xml", "testI18N-transformed-en.xml");
        MCRSessionMgr.getCurrentSession().setCurrentLanguage("de");
        testTransformation("testI18N-editor.xml", "testBasicInputComponents-source.xml", "testI18N-transformed-de.xml");
    }

    @Test
    public void testLoadResources() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        MCRSessionMgr.getCurrentSession().put("genre", "article");
        MCRSessionMgr.getCurrentSession().put("host", "journal");
        testTransformation("testLoadResources-editor.xml", null, "testLoadResources-transformed.xml");
    }

    @Test
    public void testValidation() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        MCREditorSession session = testTransformation("testValidation-editor.xml",
            "testBasicInputComponents-source.xml", "testValidation-transformed1.xml");
        assertTrue(session.getValidator().isValid());
        session = testTransformation("testValidation-editor.xml", null, "testValidation-transformed2.xml");
        assertFalse(session.getValidator().isValid());
        testTransformation("testValidation-editor.xml", null, session, "testValidation-transformed3.xml");
    }

    @Test
    public void testDefaultValue() throws IOException, URISyntaxException, TransformerException, JDOMException,
        SAXException, JaxenException {
        MCREditorSession session = testTransformation("testDefaultValue-editor.xml", null,
            "testDefaultValue-transformed1.xml");
        assertEquals("true", session.getEditedXML().getRootElement().getAttributeValue("publish"));
        session = testTransformation("testDefaultValue-editor.xml", "testDefaultValue-input.xml",
            "testDefaultValue-transformed2.xml");
        assertEquals("false", session.getEditedXML().getRootElement().getAttributeValue("publish"));
    }
}
