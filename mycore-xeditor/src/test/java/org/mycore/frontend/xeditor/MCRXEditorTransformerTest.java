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

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;

import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformerTest {

    private MCREditorSession buildEditorSession(String editedXMLFile) {
        HashMap<String, String[]> parameters = new HashMap<String, String[]>();
        if (editedXMLFile != null) {
            parameters.put("input", new String[] { editedXMLFile });
        }
        MCREditorSession editorSession = new MCREditorSession(parameters);
        editorSession.setID("1");
        return editorSession;
    }

    private MCREditorSession testTransformation(String inputFile, String editedXMLFile, String expectedOutputFile, boolean justShow)
            throws TransformerException, IOException, JDOMException, SAXException {
        MCREditorSession editorSession = buildEditorSession(editedXMLFile);
        testTransformation(inputFile, editedXMLFile, editorSession, expectedOutputFile, justShow);
        return editorSession;
    }

    private void testTransformation(String inputFile, String editedXMLFile, MCREditorSession session, String expectedOutputFile,
            boolean justShow) throws TransformerException, IOException, JDOMException, SAXException {
        MCRParameterCollector pc = new MCRParameterCollector(false);
        if (editedXMLFile != null) {
            pc.setParameter("input", editedXMLFile);
        }

        MCRContent input = MCRSourceContent.getInstance("resource:" + inputFile);
        MCRContent transformed = new MCRXEditorTransformer(session, pc).transform(input);

        if (justShow) {
            System.out.println(transformed.asString());
        } else {
            MCRContent output = MCRSourceContent.getInstance("resource:" + expectedOutputFile);
            String msg = "Transformed output is different to " + expectedOutputFile;
            assertTrue(msg, MCRXMLHelper.deepEqual(output.asXML(), transformed.asXML()));
        }
    }

    @Test
    public void testBasicInputComponents() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        testTransformation("testBasicInputComponents-editor.xml", null, "testBasicInputComponents-transformed1.xml", false);
        testTransformation("testBasicInputComponents-editor.xml", "testBasicInputComponents-source.xml",
                "testBasicInputComponents-transformed2.xml", false);
    }

    @Test
    public void testIncludes() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        testTransformation("testIncludes-editor.xml", null, "testIncludes-transformed.xml", false);
    }

    @Test
    public void testRepeats() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        testTransformation("testRepeats-editor.xml", "testBasicInputComponents-source.xml", "testRepeats-transformed.xml", false);
    }

    @Test
    public void testXPathSubstitution() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        MCRSessionMgr.getCurrentSession().put("SomeUser", "John Doe");
        testTransformation("testXPathSubstitution-editor.xml", "testBasicInputComponents-source.xml",
                "testXPathSubstitution-transformed.xml", false);
    }

    @Test
    public void testNamespaces() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        testTransformation("testNamespaces-editor.xml", "testNamespaces-source.xml", "testNamespaces-transformed.xml", false);
    }

    @Test
    public void testConditions() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        MCRSessionMgr.getCurrentSession().put("switch", "on");
        MCRSessionMgr.getCurrentSession().put("case", "2");
        testTransformation("testConditions-editor.xml", null, "testConditions-transformed.xml", false);
    }

    @Test
    public void testI18N() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException {
        MCRSessionMgr.getCurrentSession().setCurrentLanguage("en");
        testTransformation("testI18N-editor.xml", "testBasicInputComponents-source.xml", "testI18N-transformed-en.xml", false);
        MCRSessionMgr.getCurrentSession().setCurrentLanguage("de");
        testTransformation("testI18N-editor.xml", "testBasicInputComponents-source.xml", "testI18N-transformed-de.xml", false);
    }

    @Test
    public void testValidation() throws IOException, URISyntaxException, TransformerException, JDOMException, SAXException, ParseException {
        MCREditorSession session = testTransformation("testValidation-editor.xml", "testBasicInputComponents-source.xml",
                "testValidation-transformed1.xml", false);
        assertFalse(session.validate().failed());
        session = testTransformation("testValidation-editor.xml", null, "testValidation-transformed2.xml", false);
        assertTrue(session.validate().failed());
        testTransformation("testValidation-editor.xml", null, session, "testValidation-transformed3.xml", false);
    }
}
