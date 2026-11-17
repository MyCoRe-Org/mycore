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
public class MCRDNBURNTest {

    public static final String NAMESPACE_PREFIX = "urn:nbn:de:";

    public static final String SUB_NAMESPACE = "gbv:xyz:";

    public static final String NAMESPACE = NAMESPACE_PREFIX + SUB_NAMESPACE;

    @Test
    public void testUrnWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);

        assertEquals(NAMESPACE + "foo-bar", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);

        assertEquals(NAMESPACE + "foo-bar-2", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumGranularWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.toGranular("idx", 42, 100, false, "");

        assertEquals(NAMESPACE + "foo-bar-idx-042", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-idx-042", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumGranularWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.toGranular("idx", 42, 100, true, "-");

        assertEquals(NAMESPACE + "foo-bar-idx-042-3", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-idx-042-3", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumGranularWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.toGranular("idx", 42, 100, false, "");

        assertEquals(NAMESPACE + "foo-bar-2-idx-042", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2-idx-042", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumGranularWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.toGranular("idx", 42, 100, true, "-");

        assertEquals(NAMESPACE + "foo-bar-2-idx-042-5", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2-idx-042-5", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumSuffixWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.withSuffix("-sfx", false, "");

        assertEquals(NAMESPACE + "foo-bar-sfx", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-sfx", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumSuffixWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.withSuffix("-sfx", true, "-");

        assertEquals(NAMESPACE + "foo-bar-sfx-4", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-sfx-4", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumSuffixWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.withSuffix("-sfx", false, "");

        assertEquals(NAMESPACE + "foo-bar-2-sfx", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2-sfx", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumSuffixWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.withSuffix("-sfx", true, "-");

        assertEquals(NAMESPACE + "foo-bar-2-sfx-8", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE, mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2-sfx-8", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumNamespaceSuffixWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.withNamespaceSuffix("sfx:", false, "");

        assertEquals(NAMESPACE + "sfx:foo-bar", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE + "sfx:", mcrdnburn.getSubNamespace());
        assertEquals("foo-bar", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithoutChecksumNamespaceSuffixWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar", false);
        mcrdnburn = mcrdnburn.withNamespaceSuffix("sfx:", true, "-");

        assertEquals(NAMESPACE + "sfx:foo-bar-9", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE + "sfx:", mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-9", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumNamespaceSuffixWithoutChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.withNamespaceSuffix("sfx:", false, "");

        assertEquals(NAMESPACE + "sfx:foo-bar-2", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE + "sfx:", mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2", mcrdnburn.getNamespaceSpecificString());

    }

    @Test
    public void testUrnWithChecksumNamespaceSuffixWithChecksum() {

        MCRDNBURN mcrdnburn = new MCRDNBURN(SUB_NAMESPACE, "foo-bar-", true);
        mcrdnburn = mcrdnburn.withNamespaceSuffix("sfx:", true, "-");

        assertEquals(NAMESPACE + "sfx:foo-bar-2-7", mcrdnburn.asString());
        assertEquals(NAMESPACE_PREFIX, mcrdnburn.getPREFIX());
        assertEquals(SUB_NAMESPACE + "sfx:", mcrdnburn.getSubNamespace());
        assertEquals("foo-bar-2-7", mcrdnburn.getNamespaceSpecificString());

    }

}
