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

package org.mycore.pi.doi;

import javax.xml.validation.Schema;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRDOIServiceTest extends MCRTestCase {

    @Test
    public void testSchemaV3() {
        Assert.assertNotNull(loadSchema("xsd/datacite/v3/metadata.xsd"));
        Assert.assertNotNull(loadSchema(MCRDOIService.DATACITE_SCHEMA_V3));
    }

    @Test
    public void testSchemaV4() {
        Assert.assertNotNull(loadSchema("xsd/datacite/v4/metadata.xsd"));
        Assert.assertNotNull(loadSchema(MCRDOIService.DATACITE_SCHEMA_V4));
    }

    @Test
    public void testSchemaV41() {
        Assert.assertNotNull(loadSchema("xsd/datacite/v4.1/metadata.xsd"));
        Assert.assertNotNull(loadSchema(MCRDOIService.DATACITE_SCHEMA_V41));
    }

    @Test
    public void testSchemaV43() {
        Assert.assertNotNull(loadSchema("xsd/datacite/v4.3/metadata.xsd"));
        Assert.assertNotNull(loadSchema(MCRDOIService.DATACITE_SCHEMA_V43));
    }

    @Test
    public void testCrossrefSchema() {
        Assert.assertNotNull(loadSchema("xsd/crossref/4.4.1/crossref4.4.1.xsd"));
        Assert.assertNotNull(loadSchema(MCRCrossrefService.DEFAULT_SCHEMA));
    }

    private Schema loadSchema(String schemaURL) {
        return MCRDOIBaseService.resolveSchema(schemaURL);
    }

}
