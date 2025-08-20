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

package org.mycore.mods.csl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.xml.sax.SAXException;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
class MCRListModsItemDataProviderTest extends MCRMODSCSLTest {

    @Test
    public void testSorting() throws IOException, JDOMException, SAXException {
        List<MCRContent> testContent = new ArrayList<>(getTestContent());
        MCRListModsItemDataProvider mcrListModsItemDataProvider = new MCRListModsItemDataProvider();

        for (int i = 0; i < 10; i++) {
            Collections.shuffle(testContent);
            Element root = new Element("root");
            for (MCRContent c : testContent) {
                root.addContent(c.asXML().getRootElement().clone());
            }
            mcrListModsItemDataProvider.addContent(new MCRJDOMContent(root));

            List<String> ids = mcrListModsItemDataProvider.getIds().stream().toList();
            assertEquals(testContent.size(), ids.size(), "The number of ids should match the number of input");
            for (int j = 0; j < ids.size(); j++) {
                String id = ids.get(j);
                String idFromContent = testContent.get(j).asXML().getRootElement().getAttributeValue("ID");
                assertEquals(idFromContent, id, "The order of output should match input");
            }
            mcrListModsItemDataProvider.reset();
        }
    }

}
