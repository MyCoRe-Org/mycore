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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.backend.MCRDeDupNoDuplicate;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRDeDupNoDuplicateFlagEventHandlerTest {

    private final MCRDeDupNoDuplicateFlagEventHandler handler = new MCRDeDupNoDuplicateFlagEventHandler();

    private static String flagType() {
        return MCRConfiguration2.getStringOrThrow(MCRDeDupNoDuplicateFlagEventHandler.FLAG_TYPE_PROPERTY);
    }

    private static MCRObjectID id(int number) {
        return MCRObjectID.getInstance(MCRObjectID.formatID("mcr", "test", number));
    }

    private static MCRObject object(int number, String... flags) {
        MCRObject object = new MCRObject();
        object.setId(id(number));
        for (String flag : flags) {
            object.getService().addFlag(flagType(), flag);
        }
        return object;
    }

    private static MCREvent event(MCREvent.EventType type, MCRObject object) {
        MCREvent event = new MCREvent(MCREvent.ObjectType.OBJECT, type);
        event.put(MCREvent.OBJECT_KEY, object);
        return event;
    }

    private static Set<String> markedPartnersOf(MCRObjectID objectId) {
        return MCRDeDupKeyManager.obtainInstance().listNoDuplicates().stream()
            .map(marking -> partnerOf(marking, objectId.toString()))
            .collect(Collectors.toSet());
    }

    private static String partnerOf(MCRDeDupNoDuplicate marking, String objectId) {
        return objectId.equals(marking.getObjectId1()) ? marking.getObjectId2() : marking.getObjectId1();
    }

    @Test
    public void createConvertsFlagsIntoMarkings() {
        MCRObject object = object(1, id(2).toString(), id(3).toString());

        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object));

        assertEquals(Set.of(id(2).toString(), id(3).toString()), markedPartnersOf(id(1)),
            "every flag should have been turned into a no-duplicate marking");
        assertTrue(object.getService().getFlags(flagType()).isEmpty(),
            "the flags must be removed before the object metadata is stored");
    }

    @Test
    public void updateConvertsFlagsIntoMarkings() {
        MCRObject object = object(1, id(2).toString());

        handler.doHandleEvent(event(MCREvent.EventType.UPDATE, object));

        assertEquals(Set.of(id(2).toString()), markedPartnersOf(id(1)));
        assertTrue(object.getService().getFlags(flagType()).isEmpty());
    }

    @Test
    public void repeatedFlagsCreateOneMarking() {
        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object(1, id(2).toString(), id(2).toString())));
        handler.doHandleEvent(event(MCREvent.EventType.UPDATE, object(1, id(2).toString())));

        assertEquals(1, MCRDeDupKeyManager.obtainInstance().listNoDuplicates().size(),
            "the same pair must not be marked more than once");
    }

    @Test
    public void invalidAndSelfReferencingFlagsAreIgnored() {
        MCRObject object = object(1, "not an object id", id(1).toString());

        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object));

        assertTrue(MCRDeDupKeyManager.obtainInstance().listNoDuplicates().isEmpty(),
            "neither an invalid id nor a self reference may become a marking");
        assertTrue(object.getService().getFlags(flagType()).isEmpty(),
            "unusable flags must be removed as well");
    }

    @Test
    public void objectWithoutFlagsCreatesNoMarking() {
        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object(1)));

        assertEquals(List.of(), MCRDeDupKeyManager.obtainInstance().listNoDuplicates());
    }
}
