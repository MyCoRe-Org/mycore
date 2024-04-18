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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.common.xsl.MCRParameterCollector;
import org.xml.sax.SAXException;

/**
 * @author Frank Lützenkirchen
 */
public class MCRXEditorTransformerTest extends MCRTestCase {

    @Test
    public void testBasicInputComponents() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testBasicInputComponents");
    }

    @Test
    public void testBasicFieldMapping() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testBasicFieldMapping");
    }

    @Test
    public void testCheckboxesAndRadios() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testCheckboxesAndRadios");
    }

    @Test
    public void testSelect() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testSelect");
    }

    @Test
    public void testSelectMultiple() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testSelectMultiple");
    }

    @Test
    public void testDefaultValue() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testDefaultValue");
    }

    @Test
    public void testConditions() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testConditions");
    }

    @Test
    public void testI18N() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testI18N");
    }

    @Test
    public void testNamespaces() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testNamespaces");
    }

    @Test
    public void testSource() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testSource");
    }

    @Test
    public void testLoadResources() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testLoadResources");
    }

    @Test
    public void testIncludes() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testIncludes");
    }

    @Test
    public void testPreload() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testPreload");
    }

    @Test
    public void testRepeater() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testRepeater");
    }

    @Test
    public void testRepeaterControls() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testRepeaterControls");
    }

    @Test
    public void testXPathSubstitution() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testXPathSubstitution");
    }

    @Test
    public void testValidation() throws IOException, JDOMException, SAXException, JaxenException {
        runTestFile("testValidation");
    }

    private void runTestFile(String testFile) throws IOException, JDOMException, SAXException, JaxenException {
        Element test = MCRURIResolver.instance().resolve("resource:" + testFile + ".xml");
        runTest(null, test);
    }

    private void runTest(Element xed, Element test) throws IOException, JDOMException, SAXException, JaxenException {
        System.out.println("========== " + test.getAttributeValue("label") + " ==========");

        Map<String, String> params = new HashMap<String, String>();
        MCRParameterCollector pc = null;
        MCREditorSession session = null;
        MCRContent transformed = null;

        for (Element e : test.getChildren()) {
            switch (e.getName()) {
            case "xed":
                xed = e.getChildren().get(0).detach();
                break;
            case "test":
                runTest(xed, e);
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
                transformed = transformer.transform(new MCRJDOMContent(xed.clone()));
                break;
            case "html":
                testResultingHTML(e, transformed);
                break;
            case "xml":
                testResultingXML(session, e);
                break;
            case "isValid":
                MCRFieldMapper.emptyNotResubmittedNodes(session.getEditedXML().getDocument());
                Boolean expected = Boolean.valueOf(e.getTextTrim());
                Boolean isValid = session.getValidator().isValid();
                assertEquals(expected, isValid);
                break;
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
        TestResult tr = compare(editedXML, expectedXML);
        assertTrue("resulting XML is not as expected:" + tr.message, tr.result);
    }

    private void testResultingHTML(Element html, MCRContent transformed)
        throws JDOMException, IOException, SAXException {
        Element expectedResult = html.getChildren().get(0).detach();
        Element transformedResult = transformed.asXML().detachRootElement();
        TestResult tr = compare(transformedResult, expectedResult);
        assertTrue("resulting HTML is not as expected:" + tr.message, tr.result);
    }

    private TestResult compare(Element result, Element expected) throws IOException {
        TestResult tr = new TestResult();
        tr.result = MCRXMLHelper.deepEqual(result, expected);
        if (!tr.result) {
            StringBuilder sb = new StringBuilder();
            sb.append("---------- expected: ----------\n");
            sb.append(new MCRJDOMContent(expected).asString());
            sb.append("---------- actual result: ----------\n");
            sb.append(new MCRJDOMContent(result).asString());
            tr.message = sb.toString();
        }
        return tr;
    }

    class TestResult {
        boolean result;
        String message;
    }
}
