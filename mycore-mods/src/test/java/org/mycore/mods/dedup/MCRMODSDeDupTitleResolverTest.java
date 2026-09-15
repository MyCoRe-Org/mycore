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

package org.mycore.mods.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRMODSDeDupTitleResolverTest {

    private final MCRMODSDeDupTitleResolver resolver = new MCRMODSDeDupTitleResolver();

    private static Element titleInfo(String title, String subTitle) {
        Element titleInfo = new Element("titleInfo", MCRConstants.MODS_NAMESPACE);
        if (title != null) {
            titleInfo.addContent(new Element("title", MCRConstants.MODS_NAMESPACE).setText(title));
        }
        if (subTitle != null) {
            titleInfo.addContent(new Element("subTitle", MCRConstants.MODS_NAMESPACE).setText(subTitle));
        }
        return titleInfo;
    }

    private static MCRObject object(Element... modsChildren) {
        Element mods = new Element("mods", MCRConstants.MODS_NAMESPACE);
        for (Element child : modsChildren) {
            mods.addContent(child);
        }
        return MCRMODSWrapper.wrapMODSDocument(mods, "junit");
    }

    @Test
    public void resolvesCombinedTitle() {
        assertEquals(Optional.of("Main title : A subtitle"),
            resolver.resolveTitle(object(titleInfo("Main title", "A subtitle"))));
    }

    @Test
    public void resolvesTitleWithoutSubtitle() {
        assertEquals(Optional.of("Main title"),
            resolver.resolveTitle(object(titleInfo("Main title", null))));
    }

    @Test
    public void emptyWhenNoTitleInfo() {
        assertTrue(resolver.resolveTitle(object()).isEmpty());
    }
}
