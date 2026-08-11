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

package org.mycore.datamodel.metadata.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRObjectDateNormalizerTest {

    @Test
    public void testNormalize() throws Exception {
        MCRObject mcrObject = new MCRObject();
        MCRObjectDateNormalizer normalizer = new MCRObjectDateNormalizer();
        normalizer.normalize(mcrObject);

        assertNotNull(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE));
        assertNotNull(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE));

        // test with existing dates
        Date date = new Date();

        date.setTime(0);
        mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_CREATEDATE, date);
        mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE, date);
        normalizer.normalize(mcrObject);

        assertEquals(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE), date);
        assertEquals(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE), date);
    }

}
