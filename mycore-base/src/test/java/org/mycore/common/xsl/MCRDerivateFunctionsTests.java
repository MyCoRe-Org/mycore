/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.common.xsl.uriresolver.MCRMockResolver;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.URIResolver.ModuleResolver.mcrfile.Class", classNameOf = MCRMockResolver.class)
})
public class MCRDerivateFunctionsTests {

    private static final String DERIVATE_ID = "mcr_derivate_00000001";

    @BeforeEach
    public void setUp() {
        MCRMockResolver.clearCalls();
        MCRMockResolver.setResultSource(null);
        MCRURIResolver.obtainInstance().reinitialize();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = { "dir/file.xml", "/dir/file.xml" })
    @DisplayName("mcrderivate:get-file returns the file")
    public void testGetFile(String path) throws TransformerException {
        MCRMockResolver.setResultSource(new JDOMSource(new Document(new Element("file"))));

        Element result = getFile(path);

        assertEquals("file", result.getChildren().getFirst().getName());
        assertEquals("mcrfile:" + DERIVATE_ID + "/dir/file.xml", MCRMockResolver.getCalls().getFirst().getHref());
    }

    @Test
    @DisplayName("mcrderivate:get-file returns nothing for a missing file")
    public void testGetMissingFile() throws TransformerException {
        assertTrue(getFile("missing.xml").getChildren().isEmpty());
    }

    private static Element getFile(String path) throws TransformerException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("derivate-id", DERIVATE_ID);
        parameters.put("path", path);
        return MCRTestCaseXSLTUtil.transform("/xslt/functions/derivate-test.xsl", parameters).getRootElement();
    }
}
