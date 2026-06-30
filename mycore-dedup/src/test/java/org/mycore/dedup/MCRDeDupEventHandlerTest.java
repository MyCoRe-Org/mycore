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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.DeDup.CriterionBuilder.test.dummy.Class",
        classNameOf = MCRDeDupTestCriterionBuilder.class)
})
public class MCRDeDupEventHandlerTest {

    private final MCRDeDupEventHandler handler = new MCRDeDupEventHandler();

    private static MCRObject object(int number) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(MCRObjectID.formatID("mcr", "test", number)));
        return object;
    }

    private static MCREvent event(MCREvent.EventType type, MCRObject object) {
        MCREvent event = new MCREvent(MCREvent.ObjectType.OBJECT, type);
        event.put(MCREvent.OBJECT_KEY, object);
        return event;
    }

    @Test
    public void createStoresKeysAndDeleteRemovesThem() {
        MCRObject object1 = object(1);
        MCRObject object2 = object(2);

        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object1));
        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object2));

        MCRDeDupKeyManager manager = MCRDeDupKeyManager.obtainInstance();
        assertEquals(1, manager.findAllDuplicates().size(),
            "the event handler should have stored the criteria of both objects");

        handler.doHandleEvent(event(MCREvent.EventType.DELETE, object1));

        assertTrue(manager.findAllDuplicates().isEmpty(),
            "deleting an object should remove its deduplication keys");
    }

    @Test
    public void updateReplacesKeys() {
        MCRObject object1 = object(1);
        MCRObject object2 = object(2);

        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object1));
        handler.doHandleEvent(event(MCREvent.EventType.CREATE, object2));
        assertEquals(1, MCRDeDupKeyManager.obtainInstance().findAllDuplicates().size());

        // re-processing the same object must not create duplicate key rows
        handler.doHandleEvent(event(MCREvent.EventType.UPDATE, object1));

        assertEquals(1, MCRDeDupKeyManager.obtainInstance().findAllDuplicates().size(),
            "updating must replace the keys, not add additional ones");
    }
}
