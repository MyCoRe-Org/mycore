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

package org.mycore.pi.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRPIXPathPredicateTest {

    private static final String KEY_CREATION_PREDICATE = "MCR.PI.Service.Mock.CreationPredicate";

    private static final String KEY_CREATION_PREDICATE_XPATH = KEY_CREATION_PREDICATE + ".XPath";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIXPathPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_XPATH, string = "/mycoreobject/metadata/test")
    })
    void testMetadataRootElement() {
        assertTrue(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIXPathPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_XPATH, string = "/mycoreobject/metadata/foo")
    })
    void testMetadataWrongRootElement() {
        assertFalse(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIXPathPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_XPATH, string = "/mycoreobject/metadata/test/foo")
    })
    void testMetadataNestedElement() {
        assertTrue(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIXPathPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_XPATH, string = "false()")
    })
    void testFasle() {
        assertFalse(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = KEY_CREATION_PREDICATE, classNameOf = MCRPIXPathPredicate.class),
        @MCRTestProperty(key = KEY_CREATION_PREDICATE_XPATH, string = "true()")
    })
    void testTrue() {
        assertTrue(MCRPIJobService.getPredicateInstance(KEY_CREATION_PREDICATE).test(getTestObject()));
    }

    private static MCRObject getTestObject() {

        Element testElement = new Element("test");
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("foo").setText("result1"));
        testElement.addContent(new Element("bar").setText("result2"));

        Element metadataElement = new Element("metadata");
        metadataElement.addContent(testElement);

        MCRObject mcrObject = new MCRObject();
        mcrObject.setSchema("test");
        mcrObject.setId(MCRObjectID.getInstance("mcr_test_00000001"));
        mcrObject.getMetadata().setFromDOM(metadataElement);

        return mcrObject;

    }

}
