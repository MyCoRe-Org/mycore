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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.*;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaBoolean.
 * 
 * @author Jens Kupferschmidt
 *
 */
public class MCRMetaBooleanTest extends MCRTestCase {

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsClone() {
        MCRMetaBoolean bool = new MCRMetaBoolean("subtag", "my_type", 0, true);

        Element boolean_xml = bool.createXML();
        MCRMetaBoolean boolean_read = new MCRMetaBoolean();
        boolean_read.setFromDOM(boolean_xml);
        assertEquals("read objects from XML should be equal", bool, boolean_read);

        MCRMetaBoolean boolean_clone = boolean_read.clone();
        assertEquals("cloned object should be equal with original", boolean_read, boolean_clone);
    }
}
