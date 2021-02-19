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

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCREntityResolver;
import org.xml.sax.SAXException;

public class MCRDOIServiceTest extends MCRTestCase {

    @Test
    public void testSchemaV3() {
        try {
            loadSchema("xsd/datacite/v3/metadata.xsd");
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V3);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testSchemaV4() {
        try {
            loadSchema("xsd/datacite/v4/metadata.xsd");
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V4);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testSchemaV41() {
        try {
            loadSchema("xsd/datacite/v4.1/metadata.xsd");
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V41);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testSchemaV43() {
        try {
            loadSchema("xsd/datacite/v4.3/metadata.xsd");
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V43);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testCrossrefSchema() {
        try {
            loadSchema("xsd/crossref/4.4.1/crossref4.4.1.xsd");
            loadSchema(MCRCrossrefService.DEFAULT_SCHEMA);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    private void loadSchema(String schemaURL) throws SAXException {
        MCRDOIBaseService.resolveSchema(schemaURL);
    }

}
