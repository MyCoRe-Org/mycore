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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRObjectIDPoolTest {

    @Test
    public void getInstance() {
        Duration maxGCTime = Duration.ofSeconds(30);
        runGarbageCollection(new ArrayDeque<>(Arrays.asList(false, false, true))::poll, maxGCTime);
        long before = MCRObjectIDPool.getSize();
        int intPart = Year.now().getValue();
        String id = MCRObjectID.formatID("MyCoRe_test", intPart);
        MCRObjectID mcrId = MCRObjectIDPool.getMCRObjectID(id);
        WeakReference<MCRObjectID> objRef = new WeakReference<>(mcrId);
        assertEquals(before + 1, MCRObjectIDPool.getSize(), "ObjectIDPool size is different");
        mcrId = null;
        runGarbageCollection(() -> objRef.get() == null, maxGCTime);
        id = MCRObjectID.formatID("MyCoRe_test", intPart);
        assertNull(MCRObjectIDPool.getIfPresent(id), "ObjectIDPool should not contain ID anymore.");
        assertEquals(before, MCRObjectIDPool.getSize(), "ObjectIDPool size is different");
    }

    private void runGarbageCollection(Supplier<Boolean> test, Duration maxTime) {
        LocalDateTime start = LocalDateTime.now();
        int runs = 0;
        boolean succeed = test.get();
        while (!maxTime.minus(Duration.between(start, LocalDateTime.now())).isNegative()) {
            if (succeed) {
                break;
            }
            runs++;
            System.gc();
            succeed = test.get();
        }
        if (!succeed) {
            LogManager.getLogger().warn("Maximum wait time for garbage collector of {} exceeded.", maxTime);
        }
        LogManager.getLogger().info("Garbage collector ran {} times.", runs);
    }

}
