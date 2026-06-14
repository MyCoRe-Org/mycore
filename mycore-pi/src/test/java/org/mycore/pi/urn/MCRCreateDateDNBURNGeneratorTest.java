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

package org.mycore.pi.urn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
})
public class MCRCreateDateDNBURNGeneratorTest {

    public static final String DATE_FORMAT = "yyyyMMdd";

    public static final String NAMESPACE = "urn:nbn:de:gbv:xyz";

    @Test
    public void generate() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRCreateDateDNBURNGenerator generator = new MCRCreateDateDNBURNGenerator(DATE_FORMAT, NAMESPACE, "-", 3);
        String urn = generator.generate(object, "").asString();

        assertTrue(urn.startsWith(NAMESPACE));
        assertEquals('-', urn.charAt(NAMESPACE.length()));
        assertEquals('-', urn.charAt(urn.length() - 2));

        String value = urn.substring(NAMESPACE.length() + 1, urn.length() - 2);
        char checksum = Character.forDigit(new MCRDNBURN("gbv:xyz", "-" + value + "-").calculateChecksum(), 10);

        assertTrue(value.startsWith(formatDate(new Date()) + "-"));
        assertTrue(value.endsWith("-000"));
        assertEquals(checksum, urn.charAt(urn.length() - 1));

    }

    private String formatDate(Date date) {

        MCRISO8601Date isoDate = new MCRISO8601Date();
        isoDate.setDate(date);

        return isoDate.format(DATE_FORMAT, Locale.ROOT);

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRCreateDateDNBURNGenerator generator = new MCRCreateDateDNBURNGenerator(DATE_FORMAT, NAMESPACE, "", -1);
        String urn1 = generator.generate(object, "").asString();
        String urn2 = generator.generate(object, "").asString();
        String urn3 = generator.generate(object, "").asString();

        assertNotEquals(urn1, urn2);
        assertNotEquals(urn2, urn3);
        assertNotEquals(urn3, urn1);

        assertTrue(urn1.substring(0, urn1.length() - 1).endsWith("-0"));
        assertTrue(urn2.substring(0, urn2.length() - 1).endsWith("-1"));
        assertTrue(urn3.substring(0, urn3.length() - 1).endsWith("-2"));

    }

}
