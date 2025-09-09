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
package org.mycore.datamodel.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRURLContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler
 *
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.DefaultLang", string = "de")
})
public class MCRObjectMetadataTest {
    private static final String TEST_OBJECT_RESOURCE_NAME = "/mcr_test_01.xml";

    private MCRObjectMetadata testMetadata;

    @BeforeEach
    public void setUp() throws Exception {
        Document testObjectDocument = loadResourceDocument(TEST_OBJECT_RESOURCE_NAME);
        testMetadata = new MCRObjectMetadata();
        testMetadata.setFromDOM(testObjectDocument.getRootElement().getChild(MCRObjectMetadata.XML_NAME));
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#size()}.
     */
    @Test
    public void size() {
        assertEquals(1, testMetadata.size(), "Expected just one metadata entry");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(int)}.
     */
    @Test
    public void getMetadataTagName() {
        assertEquals("def.textfield", testMetadata.getMetadataElement(0)
            .getTag(), "Metadata tag is not 'def.textfield'");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getHeritableMetadata()}.
     */
    @Test
    public void getHeritableMetadata() {
        assertEquals(1, testMetadata.getHeritableMetadata().size(), "Did not find any heritable metadata");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeInheritedMetadata()}.
     */
    @Test
    public void removeInheritedMetadata() {
        testMetadata.removeInheritedMetadata();
        assertEquals(1, testMetadata.size(), "Did not expect removal of any metadata");
        testMetadata.setMetadataElement(getInheritedMetadata());
        testMetadata.removeInheritedMetadata();
        assertEquals(2, testMetadata.size(), "Did not expect removal of any metadata element");
        MCRMetaElement defJunit = testMetadata.getMetadataElement("def.junit");
        assertEquals(1, defJunit.size(), "Not all inherited metadata was removed");
        defJunit = getInheritedMetadata();
        for (MCRMetaInterface i : defJunit) {
            if (i.getInherited() == 0) {
                i.setInherited(1);
            }
        }
        testMetadata.setMetadataElement(defJunit);
        testMetadata.removeInheritedMetadata();
        assertEquals(1, testMetadata.size(), "Did expect removal of \"def.junit\" metadata element");
    }

    private MCRMetaElement getInheritedMetadata() {
        MCRMetaElement defJunit = new MCRMetaElement(MCRMetaLangText.class, "def.junit", true, false, null);
        MCRMetaLangText test1 = new MCRMetaLangText("junit", "de", null, 0, null, "Test 1");
        MCRMetaLangText test2 = new MCRMetaLangText("junit", "de", null, 1, null, "Test 2");
        MCRMetaLangText test3 = new MCRMetaLangText("junit", "de", null, 1, null, "Test 3");
        defJunit.addMetaObject(test1);
        defJunit.addMetaObject(test2);
        defJunit.addMetaObject(test3);
        return defJunit;
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#appendMetadata(org.mycore.datamodel.metadata.MCRObjectMetadata)}.
     */
    @Test
    public void appendMetadata() {
        MCRObjectMetadata meta2 = getDateObjectMetadata();
        testMetadata.appendMetadata(meta2);
        assertEquals(2, testMetadata.size(), "Expected 2 metadates");
    }

    private MCRObjectMetadata getDateObjectMetadata() {
        MCRObjectMetadata meta2 = new MCRObjectMetadata();
        MCRMetaISO8601Date date = new MCRMetaISO8601Date("datefield", "test", 0);
        date.setDate(new Date());
        MCRMetaElement el2 = new MCRMetaElement();
        el2.addMetaObject(date);
        el2.setClass(MCRMetaISO8601Date.class);
        el2.setHeritable(true);
        el2.setTag(date.datapart);
        meta2.setMetadataElement(el2);
        return meta2;
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(java.lang.String)}.
     */
    @Test
    public void getMetadataElementString() {
        assertEquals(testMetadata.getMetadataElement(0), testMetadata.getMetadataElement("def.textfield"),
            "did not get correct MCRMetaElement instance");
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#getMetadataElement(int)}.
     */
    @Test
    public void getMetadataElementInt() {
        assertEquals(testMetadata.getMetadataElement("def.textfield"), testMetadata.getMetadataElement(0),
            "did not get correct MCRMetaElement instance");
    }

    /**
     * Test method for org.mycore.datamodel.metadata.MCRObjectMetadata#setMetadataElement(org.mycore.datamodel.metadata.MCRMetaElement, java.lang.String) (not implemented yet).
     */
    @Test
    @Disabled("not implemented")
    public void setMetadataElement() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeMetadataElement(java.lang.String)}.
     */
    @Test
    @Disabled("not implemented")
    public void removeMetadataElementString() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#removeMetadataElement(int)}.
     */
    @Test
    @Disabled("not implemented")
    public void removeMetadataElementInt() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#createXML()}.
     */
    @Test
    @Disabled("not implemented")
    public void createXML() {
        fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link org.mycore.datamodel.metadata.MCRObjectMetadata#isValid()}.
     */
    @Test
    @Disabled("not implemented")
    public void isValid() {
        fail("Not yet implemented"); // TODO
    }

    private static Document loadResourceDocument(String resource) throws MCRException, IOException, JDOMException {
        URL mcrTestUrl = MCRObjectMetadataTest.class.getResource(resource);
        return MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(mcrTestUrl));
    }

}
