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

package org.mycore.common;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRLinkTableInterface;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
        @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
        @MCRTestProperty(key = "MCR.Access.Strategy.Class", classNameOf = MCRIFSTest.AlwaysTrueStrategy.class),
        @MCRTestProperty(key = "MCR.Persistence.LinkTable.Store.Class",
            classNameOf = MCRIFSTest.FakeLinkTableStore.class),
        @MCRTestProperty(key = "MCR.Metadata.Store.DefaultClass", classNameOf = MCRMetadataStore.class)
    })
public abstract class MCRIFSTest {

    @TempDir
    static Path tempDir;

    @BeforeEach
    public void prepareEventHandler() throws Exception {
        MCREventManager.getInstance().clear().addEventHandler(MCREvent.ObjectType.OBJECT,
            new MCRXMLMetadataEventHandler());
    }

    public static MCRObject createObject() {
        MCRObject object = new MCRObject();
        object.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("mycore_object"));
        object.setSchema("noSchema");
        return object;
    }

    public static MCRDerivate createDerivate(MCRObjectID objectHrefId) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("mycore_derivate"));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID();
        mcrMetaLinkID.setReference(objectHrefId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }

    public static class FakeLinkTableStore implements MCRLinkTableInterface {
        @Override
        public void create(String from, String to, String type, String attr) {

        }

        @Override
        public void delete(String from, String to, String type) {

        }

        @Override
        public int countTo(String fromtype, String to, String type, String restriction) {
            return 0;
        }

        @Override
        public Map<String, Number> getCountedMapOfMCRTO(String mcrtoPrefix) {
            return Map.of();
        }

        @Override
        public Collection<String> getSourcesOf(String to, String type) {
            return List.of();
        }

        @Override
        public Collection<String> getDestinationsOf(String from, String type) {
            return List.of();
        }
    }

}
