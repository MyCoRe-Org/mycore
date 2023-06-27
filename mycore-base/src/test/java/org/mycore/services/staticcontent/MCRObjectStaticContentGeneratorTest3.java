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

package org.mycore.services.staticcontent;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectStaticContentGeneratorTest3 extends MCRTestCase {

    @Test
    public void getSlotDirPath3() {
        final MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(
            null, Paths.get("/"));
        MCRConfiguration2.set("MCR.Metadata.ObjectID.NumberPattern", "0000000");
        MCRObjectID derivate = MCRObjectID.getInstance("mcr_derivate_0000001");
        Assert.assertEquals("Paths should match", Paths.get("/000/000/1"), generator.getSlotDirPath(derivate));
    }
}
