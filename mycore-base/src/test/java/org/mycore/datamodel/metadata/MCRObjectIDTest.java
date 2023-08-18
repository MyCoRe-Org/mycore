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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;

public class MCRObjectIDTest extends MCRStoreTestCase {

    private static final String BASE_ID = "MyCoRe_test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void setNextFreeIdString() {
        MCRObjectID id1 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals("First id should be int 1", 1, id1.getNumberAsInteger());
        MCRObjectID id2 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 2", 2, id2.getNumberAsInteger());
        getStore().create(id2, new Document(new Element("test")), new Date());
        MCRObjectID id3 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 3", 3, id3.getNumberAsInteger());
    }

    @Test
    public void validateID() {
        assertTrue("The mcrid 'JUnit_test_123' is valid", MCRObjectID.isValid("JUnit_test_123"));
        assertFalse("The mcrid 'JUnit_xxx_123' is invalid (unknown type)", MCRObjectID.isValid("JUnit_xxx_123"));
        assertFalse("The mcrid 'JUnit_test__123' is invalid (to many underscores)",
            MCRObjectID.isValid("JUnit_test__123"));
        assertFalse("The mcrid 'JUnit_test_123 ' is invalid (space at end)", MCRObjectID.isValid("JUnit_test_123 "));
        assertFalse("The mcrid 'JUnit_test_-123' is invalid (negative number)", MCRObjectID.isValid("JUnit_test_-123"));
        assertFalse(
            "The mcrid 'aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffff_test_123' is invalid (length)",
            MCRObjectID.isValid("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffff_test_123"));
    }

    @Test
    public void compareTo() {
        Set<MCRObjectID> testIds = IntStream.range(0, 17)
            .mapToObj(i -> MCRObjectID.getInstance(MCRObjectID.formatID("MyCoRe", "test", i)))
            .flatMap(o -> Stream.of(o, MCRObjectID.getInstance(
                MCRObjectID.formatID(o.getProjectId(), "junit", o.getNumberAsInteger()))))
            .flatMap(o -> Stream.concat(Stream.of(o),
                Stream.of("junit", "mcr", "JUnit")
                    .map(projectId -> MCRObjectID
                        .getInstance(
                            MCRObjectID.formatID(
                                projectId,
                                o.getTypeId(),
                                o.getNumberAsInteger())))))
            .collect(Collectors.toSet());
        ArrayList<MCRObjectID> first = new ArrayList<>(testIds);
        ArrayList<MCRObjectID> test = new ArrayList<>(testIds);
        first.sort(Comparator.comparing(MCRObjectID::toString));
        test.sort(MCRObjectID::compareTo);
        assertArrayEquals("Order should be the same.", first.toArray(), test.toArray());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.Type.junit", Boolean.TRUE.toString());
        return testProperties;
    }

}
