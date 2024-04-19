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
public class MCRXEditorTransformerTest extends MCRTestCase {

    @Test
    public void testBasicInputComponents() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testBasicInputComponents");
    }

    @Test
    public void testBasicFieldMapping() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testBasicFieldMapping");
    }

    @Test
    public void testCheckboxesAndRadios() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testCheckboxesAndRadios");
    }

    @Test
    public void testSelect() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testSelect");
    }

    @Test
    public void testSelectMultiple() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testSelectMultiple");
    }

    @Test
    public void testDefaultValue() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testDefaultValue");
    }

    @Test
    public void testConditions() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testConditions");
    }

    @Test
    public void testI18N() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testI18N");
    }

    @Test
    public void testNamespaces() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testNamespaces");
    }

    @Test
    public void testSource() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testSource");
    }

    @Test
    public void testLoadResources() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testLoadResources");
    }

    @Test
    public void testIncludes() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testIncludes");
    }

    @Test
    public void testPreload() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testPreload");
    }

    @Test
    public void testRepeater() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testRepeater");
    }

    @Test
    public void testRepeaterControls() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testRepeaterControls");
    }

    @Test
    public void testXPathSubstitution() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testXPathSubstitution");
    }

    @Test
    public void testValidation() throws IOException, JDOMException, SAXException, JaxenException {
        new MCRXEditorTestRunner("testValidation");
    }
}
