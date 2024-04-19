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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCRXEditorTestRunner {

    private Element xEditor;

    public MCRXEditorTestRunner(String testFile) throws IOException, JDOMException, SAXException, JaxenException {
        Element test = MCRURIResolver.instance().resolve("resource:" + testFile + ".xml");
        System.out.println("Running test file " + testFile + " ...");
        runTest(test);
    }

    private void runTest(Element test) throws IOException, JDOMException, SAXException, JaxenException {
        System.out.println("========== " + test.getAttributeValue("label") + " ==========");

        Map<String, String> params = new HashMap<String, String>();
        MCRParameterCollector pc = null;
        MCREditorSession session = null;
        MCRContent transformed = null;

        for (Element e : test.getChildren()) {
            switch (e.getName()) {
            case "xed":
                xEditor = e.getChildren().get(0).detach();
                break;
            case "test":
                runTest(e);
                break;
            case "source":
                prepareSourceXML(e);
                break;
            case "param":
                params.put(e.getAttributeValue("name"), e.getText());
                break;
            case "lang":
                MCRSessionMgr.getCurrentSession().setCurrentLanguage(e.getTextTrim());
                break;
            case "transform":
                pc = new MCRParameterCollector();
                session = buildEditorSession(pc, params);
            case "transformAgain":
                MCRXEditorTransformer transformer = new MCRXEditorTransformer(session, pc);
                transformed = transformer.transform(new MCRJDOMContent(xEditor.clone()));
                break;
            case "html":
                testResultingHTML(e, transformed);
                break;
            case "xml":
                testResultingXML(session, e);
                break;
            case "isValid":
                Boolean expected = Boolean.valueOf(e.getTextTrim());
                Boolean isValid = session.getValidator().isValid();
                assertEquals(expected, isValid);
                break;
            case "submit":
                Map<String, String[]> submittedValues = new HashMap<>();
                e.getChildren("param").forEach(param -> {
                    String name = param.getAttributeValue("name");
                    int numValues = param.getChildren().size();
                    String[] values = new String[numValues];
                    for (int i = 0; i < numValues; i++) {
                        values[i] = param.getChildren().get(i).getText();
                    }
                    submittedValues.put(name, values);
                });
                session.getSubmission().setSubmittedValues(submittedValues);
                break;
            default:
                throw new MCRException("Unknown test element: " + e.getQualifiedName());
            }
        }
    }

    private MCREditorSession buildEditorSession(MCRParameterCollector pc, Map<String, String> params) {
        pc.setParameters(params);

        Map<String, String[]> requestParameters = new HashMap<String, String[]>();
        params.entrySet().forEach(me -> {
            requestParameters.put(me.getKey(), new String[] { me.getValue() });
        });

        MCREditorSession session = new MCREditorSession(requestParameters, pc);
        session.setID("1");
        return session;
    }

    private void prepareSourceXML(Element source) {
        String id = source.getAttributeValue("id");
        Element root = source.getChildren().get(0).detach();
        MCRSessionMgr.getCurrentSession().put(id, root);
    }

    private void testResultingXML(MCREditorSession session, Element xml) throws IOException {
        Element expectedXML = xml.getChildren().get(0).detach();
        Element editedXML = session.getEditedXML().getDocument().getRootElement().clone();
        MCRTestResult tr = new MCRTestResult(editedXML, expectedXML);
        tr.assertTrue("Resulting XML is not as expected");
    }

    private void testResultingHTML(Element html, MCRContent transformed)
        throws JDOMException, IOException, SAXException {
        Element expectedResult = html.getChildren().get(0).detach();
        Element transformedResult = transformed.asXML().detachRootElement();
        MCRTestResult tr = new MCRTestResult(transformedResult, expectedResult);
        tr.assertTrue("Resulting HTML is not as expected");
    }

    class MCRTestResult {

        boolean deepEquals;

        String output;

        MCRTestResult(Element actualXML, Element expectedXML) throws IOException {
            this.deepEquals = MCRXMLHelper.deepEqual(actualXML, expectedXML);
            if (!this.deepEquals) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n---------- expected: ----------\n");
                sb.append(new MCRJDOMContent(expectedXML).asString());
                sb.append("\n---------- actual result: ----------\n");
                sb.append(new MCRJDOMContent(actualXML).asString());
                this.output = sb.toString();
            }
        }

        void assertTrue(String message) {
            org.junit.Assert.assertTrue(message + ":" + this.output, this.deepEquals);
        }
    }
}
