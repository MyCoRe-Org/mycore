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

package org.mycore.mcr.acl.accesskey;

import static org.mycore.datamodel.metadata.MCRObjectDerivate.ELEMENT_INTERNAL;
import static org.mycore.datamodel.metadata.MCRObjectDerivate.ELEMENT_LINKMETA;

import java.util.Map;

import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRAccessKeyTestCase extends MCRStoreTestCase {

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.datadir", "%MCR.basedir%/data");
        testProperties
            .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Access.Strategy.Class", AlwaysTrueStrategy.class.getName());
        testProperties.put("MCR.Metadata.Type.object", "true");
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        return testProperties;
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
        final MCRMetaIFS ifs = new MCRMetaIFS(ELEMENT_INTERNAL, null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID metaLinkID = new MCRMetaLinkID(ELEMENT_LINKMETA, 0);
        metaLinkID.setReference(objectHrefId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(metaLinkID);
        return derivate;
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }
}
