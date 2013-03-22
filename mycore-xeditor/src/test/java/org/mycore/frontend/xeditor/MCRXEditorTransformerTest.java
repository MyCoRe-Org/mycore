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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

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

    private void testTransformation(String inputFile, String editedXMLFile, String expectedOutputFile, boolean write)
            throws TransformerException, IOException, JDOMException, SAXException {
        MCRParameterCollector parameters = new MCRParameterCollector();
        if (editedXMLFile != null)
            parameters.setParameter("input", editedXMLFile);

        MCREditorSession editorSession = new MCREditorSession(parameters);
        editorSession.setID("1");
        
        MCRContent input = MCRSourceContent.getInstance("resource:" + inputFile);
        MCRContent transformed = new MCRXEditorTransformer(editorSession).transform(input);

        if (write) {
            File targetFile = File.createTempFile(expectedOutputFile.split("\\.")[0], expectedOutputFile.split("\\.")[1]);
            transformed.sendTo(targetFile);
            System.out.println("Output written to " + targetFile.getAbsolutePath());
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
        MCRSessionMgr.getCurrentSession().put("XSL.User", "John Doe");
        testTransformation("testXPathSubstitution-editor.xml", "testBasicInputComponents-source.xml", "testXPathSubstitution-transformed.xml", false);
    }
}
