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
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaLangText.
 * 
 * @author Jens Kupferschmidt
 *
 */
public class MCRMetaLangTextTest extends MCRTestCase {

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsClone() {
        MCRMetaLangText langtext = new MCRMetaLangText("subtag", "de", "my_type", 0, "plain", "mein text");

        Element langtext_xml = langtext.createXML();
        MCRMetaLangText langtext_read = new MCRMetaLangText();
        langtext_read.setFromDOM(langtext_xml);
        assertEquals("read objects from XML should be equal", langtext, langtext_read);

        MCRMetaLangText langtext_clone = langtext_read.clone();
        assertEquals("cloned object should be equal with original", langtext_read, langtext_clone);
    }
}
