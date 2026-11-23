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

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRDNBURNParserTest {

    public static final String NAMESPACE_PREFIX = "urn:nbn:de:";

    public static final String SUB_NAMESPACE = "gbv:xyz:";

    public static final String NAMESPACE = NAMESPACE_PREFIX + SUB_NAMESPACE;

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testWithoutChecksum() {

        String identifier = NAMESPACE + "foo-bar";
        MCRDNBURN mcrdnburn = new MCRDNBURNParser().parse(identifier).get();

        assertEquals(identifier, mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testWithChecksum() {

        String identifier = NAMESPACE + "foo-bar-2";
        MCRDNBURN mcrdnburn = new MCRDNBURNParser().parse(identifier).get();

        assertEquals(identifier, mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testWithCalculatedChecksum() {

        String identifier = NAMESPACE + "foo-bar-" + MCRDNBURNParser.APPEND_CHECKSUM_MARKER;
        MCRDNBURN mcrdnburn = new MCRDNBURNParser().parse(identifier).get();

        assertEquals(identifier.replace(MCRDNBURNParser.APPEND_CHECKSUM_MARKER, "2"), mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2", mcrdnburn.getNamespaceSpecificString());

    }

}
