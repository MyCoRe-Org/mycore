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

package org.mycore.datamodel.common;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRFileBaseCacheObjectIDGeneratorTest extends MCRTestCase {

    public static final int GENERATOR_COUNT = 10;
    public static final int TEST_IDS = 100;

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void getNextFreeId() throws IOException {
        Files.createDirectories(MCRFileBaseCacheObjectIDGenerator.getDataDirPath());

        var generatorList = new ArrayList<MCRFileBaseCacheObjectIDGenerator>();
        for (int i = 0; i < GENERATOR_COUNT; i++) {
            generatorList.add(new MCRFileBaseCacheObjectIDGenerator());
        }

        // need thread safe list of generated ids
        var generatedIds = Collections.synchronizedList(new ArrayList<MCRObjectID>());
        IntStream.range(0, TEST_IDS)
            .parallel()
            .forEach(i -> {
                LOGGER.info("Generating ID {}", i);
                var generator = generatorList.get(i % GENERATOR_COUNT);
                MCRObjectID id = generator.getNextFreeId("junit", "test");
                generatedIds.add(id);
            });

        // check if all ids are unique
        assertEquals(TEST_IDS, generatedIds.size());
        assertEquals(TEST_IDS, generatedIds.stream().distinct().count());

        // check if there is no space in the ids
        var sortedIds = new ArrayList<>(generatedIds);
        Collections.sort(sortedIds);
        for (int i = 0; i < sortedIds.size() - 1; i++) {
            assertEquals(i + 1, sortedIds.get(i).getNumberAsInteger());
        }

    }

}
