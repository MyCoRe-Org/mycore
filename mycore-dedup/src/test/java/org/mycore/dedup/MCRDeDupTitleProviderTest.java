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

package org.mycore.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.withtitle", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.notitle", string = "true"),
    @MCRTestProperty(key = "MCR.DeDup.TitleResolver.withtitle.Class",
        classNameOf = MCRDeDupLabelTitleResolver.class)
})
public class MCRDeDupTitleProviderTest {

    private final MCRDeDupTitleProvider provider = MCRDeDupTitleProvider.obtainInstance();

    private static MCRObject object(String type, String label) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(MCRObjectID.formatID("mcr", type, 1)));
        object.setLabel(label);
        return object;
    }

    @Test
    public void usesResolverConfiguredForTheType() {
        assertEquals(Optional.of("A title"), provider.resolveTitle(object("withtitle", "A title")));
    }

    @Test
    public void typeWithoutConfiguredResolverYieldsNoTitle() {
        assertTrue(provider.getTitleResolver("notitle").isEmpty());
        assertTrue(provider.resolveTitle(object("notitle", "A title")).isEmpty());
    }
}
