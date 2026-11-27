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

package org.mycore.mcr.neo4j.frontend.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.neo4j.utils.MCRNeo4JManager;

public class MCRNeo4JCommandsTest extends MCRStoreTestCase {

    private static final String OBJECT_ID = "test_document_00000001";

    private static final String DERIVATE_ID = "test_derivate_00000001";

    private static final List<MCRObjectID> SYNCHRONIZED_IDS = new ArrayList<>();

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> properties = super.getTestProperties();
        properties.put("MCR.Metadata.Type.document", "true");
        properties.put("MCR.Metadata.Type.derivate", "true");
        properties.put("MCR.Neo4J.Manager.Class", RecordingNeo4JManager.class.getName());
        return properties;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        SYNCHRONIZED_IDS.clear();
        store(OBJECT_ID);
        store(DERIVATE_ID);
    }

    @Test
    public void synchronizeAllSkipsDerivates() {
        MCRNeo4JCommands.synchronizeAll();

        assertEquals(List.of(MCRObjectID.getInstance(OBJECT_ID)), SYNCHRONIZED_IDS);
    }

    @Test
    public void synchronizeForTypeSkipsDerivates() {
        MCRNeo4JCommands.synchronizeForType("derivate");

        assertTrue(SYNCHRONIZED_IDS.isEmpty());
    }

    @Test
    public void synchronizeForBaseSkipsDerivates() {
        MCRNeo4JCommands.synchronizeForBase("test_derivate");

        assertTrue(SYNCHRONIZED_IDS.isEmpty());
    }

    @Test
    public void synchronizeForMCRIDSkipsDerivate() {
        MCRNeo4JCommands.synchronizeForMCRID(DERIVATE_ID);

        assertTrue(SYNCHRONIZED_IDS.isEmpty());
    }

    @Test
    public void synchronizeForMCRIDSynchronizesObject() {
        MCRNeo4JCommands.synchronizeForMCRID(OBJECT_ID);

        assertEquals(List.of(MCRObjectID.getInstance(OBJECT_ID)), SYNCHRONIZED_IDS);
    }

    private void store(String id) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/MCRNeo4JCommandsTest/" + id + ".xml")) {
            Document xml = new SAXBuilder().build(is);
            getStore().create(MCRObjectID.getInstance(id), xml, new Date());
        }
    }

    public static class RecordingNeo4JManager implements MCRNeo4JManager {

        @Override
        public void addNodeByMCRObject(MCRObject mcrObject) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateNodeByMCRObject(MCRObject mcrObject) {
            SYNCHRONIZED_IDS.add(mcrObject.getId());
        }

        @Override
        public void deleteNodeByMCRObjectID(String id) {
            throw new UnsupportedOperationException();
        }
    }
}
