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

package org.mycore.datamodel.metadata;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.junit", string = "true")
})
public class MCRObjectIDTest {

    private static final String BASE_ID = "MyCoRe_test";

    /**
     * Resets MCRObjectID number format via reflection
     */
    public static void resetObjectIDFormat() {
        try {
            Field fNumberformat = MCRObjectID.class.getDeclaredField("numberFormat");
            fNumberformat.setAccessible(true);
            Method mInitNumberformat = MCRObjectID.class.getDeclaredMethod("initNumberFormat");
            mInitNumberformat.setAccessible(true); //if security settings allow this
            Object oNumberFormat = mInitNumberformat.invoke(null);
            fNumberformat.set(null, oNumberFormat);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void setNextFreeIdString() {
        MCRObjectID id1 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals(1, id1.getNumberAsInteger(), "First id should be int 1");
        MCRObjectID id2 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals(2, id2.getNumberAsInteger(), "Second id should be int 2");
        MCRXMLMetadataManager.getInstance().create(id2, new Document(new Element("test")), new Date());
        MCRObjectID id3 = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(BASE_ID);
        assertEquals(3, id3.getNumberAsInteger(), "Second id should be int 3");
    }

    @Test
    public void validateID() {
        assertTrue(MCRObjectID.isValid("JUnit_test_123"), "The mcrid 'JUnit_test_123' is valid");
        assertFalse(MCRObjectID.isValid("JUnit_xxx_123"),
            "The mcrid 'JUnit_xxx_123' is invalid (unknown type)");
        assertFalse(MCRObjectID.isValid("JUnit_test__123"),
            "The mcrid 'JUnit_test__123' is invalid (to many underscores)");
        assertFalse(MCRObjectID.isValid("JUnit_test_123 "),
            "The mcrid 'JUnit_test_123 ' is invalid (space at end)");
        assertFalse(MCRObjectID.isValid("JUnit_test_-123"),
            "The mcrid 'JUnit_test_-123' is invalid (negative number)");
        assertFalse(
            MCRObjectID.isValid("aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffff_test_123"),
            "The mcrid 'aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffff_test_123' is invalid (length)");
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
            .sequential()
            .collect(Collectors.toSet());
        ArrayList<MCRObjectID> first = new ArrayList<>(testIds);
        ArrayList<MCRObjectID> test = new ArrayList<>(testIds);
        first.sort(Comparator.comparing(MCRObjectID::toString));
        test.sort(MCRObjectID::compareTo);
        assertArrayEquals(first.toArray(), test.toArray(), "Order should be the same.");
    }

}
