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

import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.datadir", string = "%MCR.basedir%/data"),
    @MCRTestProperty(key = "MCR.Persistence.LinkTable.Store.Class", classNameOf = MCRHIBLinkTableStore.class),
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Access.Strategy.Class", classNameOf = MCRAccessKeyTestCase.AlwaysTrueStrategy.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true")
})
public abstract class MCRAccessKeyTestCase {

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
