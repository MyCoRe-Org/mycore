/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mcr.acl.accesskey;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRAccessKeyTestCase extends MCRStoreTestCase {

    private static final String ACCESS_KEY_STRATEGY_PROP = "MCR.ACL.AccessKey.Strategy";

    protected static final String ALLOWED_OBJECT_TYPES_PROP = ACCESS_KEY_STRATEGY_PROP + ".AllowedObjectTypes";

    protected static final String ALLOWED_SESSION_PERMISSION_TYPES_PROP = ACCESS_KEY_STRATEGY_PROP + ".AllowedSessionPermissionTypes";

    private static final String OBJECT_ID = "mcr_object_00000001";

    private static final String DERIVATE_ID = "mcr_derivate_00000001";

    private MCRObject object;

    private MCRDerivate derivate;

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties
            .put("MCR.Persistence.LinkTable.Store.Class", "org.mycore.backend.hibernate.MCRHIBLinkTableStore");
        testProperties.put("MCR.Access.Class", MCRAccessBaseImpl.class.getName());
        testProperties.put("MCR.Metadata.Type.document", "true");
        testProperties.put("MCR.Metadata.Type.object", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.Type.derivate", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.ObjectID.NumberPattern", "00000000");
        return testProperties;
    }

    protected void setUpInstanceDefaultProperties() {
        MCRConfiguration2.set(ALLOWED_OBJECT_TYPES_PROP, "object,derivate");
        MCRConfiguration2.set(ALLOWED_SESSION_PERMISSION_TYPES_PROP, "read,writedb");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        object = new MCRObject();
        object.setSchema("noSchema");
        final MCRObjectID objectId = MCRObjectID.getInstance(OBJECT_ID);
        object.setId(objectId);
        MCRMetadataManager.create(object);

        derivate = new MCRDerivate();
        derivate.setSchema("datamodel-derivate.xsd");
        final MCRObjectID derivateId = MCRObjectID.getInstance(DERIVATE_ID);
        derivate.setId(derivateId);
        final MCRMetaIFS ifs = new MCRMetaIFS("internal", null);
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID metaLinkID = new MCRMetaLinkID("internal", 0);
        metaLinkID.setReference(objectId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(metaLinkID);
        MCRMetadataManager.create(derivate);
        setUpInstanceDefaultProperties();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }

    public MCRObject getObject() {
        return object;
    }

    public MCRDerivate getDerivate() {
        return derivate;
    }
}
