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

package org.mycore.ocfl;

import java.util.Map;

import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRJPATestCase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.repository.MCROCFLRepository;

public abstract class MCROCFLMetadataTestCase extends MCRJPATestCase {

    protected MCROCFLRepository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.repository = MCROCFLTestCaseHelper.setUp(false);
    }

    @Override
    public void tearDown() throws Exception {
        MCROCFLTestCaseHelper.tearDown(this.repository);
        MCREventManager.getInstance().clear();
        super.tearDown();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        // set jpa mappings, only use mycore-base
        testProperties.put("MCR.JPA.MappingFileNames", "META-INF/mycore-base-mappings.xml");
        // use ocfl metadata
        testProperties.put("MCR.Metadata.Manager.Repository","Test");
        testProperties.put("MCR.Metadata.Manager.Class","org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager");
        testProperties.put("MCR.Metadata.Type.object", "true");
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        // set free access
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Access.Strategy.Class", AlwaysTrueStrategy.class.getName());

        return testProperties;
    }

    public MCRObject createObject(String objectId) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(objectId));
        object.setSchema("noSchema");
        return object;
    }

    public MCRDerivate createDerivate(String objectId, String derivateId) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRObjectID.getInstance(derivateId));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS("internal", null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID("linkmeta", 0);
        mcrMetaLinkID.setReference(objectId, null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }

}
