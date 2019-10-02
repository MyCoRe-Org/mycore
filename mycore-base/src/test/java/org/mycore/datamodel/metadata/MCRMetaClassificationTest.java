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

import static org.junit.Assert.assertEquals;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaClassification.
 * 
 * @author Jens Kupferschmidt
 *
 */
public class MCRMetaClassificationTest extends MCRTestCase {

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsClone() {
        MCRMetaClassification classification = new MCRMetaClassification("subtag", 0, "my_type", "my_classification",
            "my_category");

        Element langtext_xml = classification.createXML();
        MCRMetaClassification langtext_read = new MCRMetaClassification();
        langtext_read.setFromDOM(langtext_xml);
        assertEquals("read objects from XML should be equal", classification, langtext_read);

        MCRMetaClassification langtext_clone = langtext_read.clone();
        assertEquals("cloned object should be equal with original", langtext_read, langtext_clone);
    }
}
