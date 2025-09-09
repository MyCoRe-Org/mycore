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

import java.util.List;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRMetaEnrichedLinkIDTest {

    protected static final String TEST_ELEMENT_NAME = "atest";

    protected static final String TEST2_ELEMENT_NAME = "test";

    @Test
    public void testOrdering() {
        final MCREditableMetaEnrichedLinkID mcrMetaEnrichedLinkID = new MCREditableMetaEnrichedLinkID();

        mcrMetaEnrichedLinkID.setReference("mir_derivate_00000001", null, "");
        mcrMetaEnrichedLinkID.setSubTag("derobject");
        mcrMetaEnrichedLinkID.setMainDoc("main");
        mcrMetaEnrichedLinkID.setOrder(1);
        mcrMetaEnrichedLinkID.getContentList().add(new Element(TEST_ELEMENT_NAME));
        mcrMetaEnrichedLinkID.getContentList().add(new Element(TEST2_ELEMENT_NAME));

        final Element xml = mcrMetaEnrichedLinkID.createXML();

        final List<Element> children = xml.getChildren();

        assertEquals(MCRMetaEnrichedLinkID.ORDER_ELEMENT_NAME, children.get(0).getName(),
            "First Element should be order");
        assertEquals(MCRMetaEnrichedLinkID.MAIN_DOC_ELEMENT_NAME, children.get(1).getName(),
            "Second Element should be maindoc");
        assertEquals(TEST_ELEMENT_NAME, children.get(2).getName(),
            "Third Element should be " + TEST_ELEMENT_NAME);

    }

}
