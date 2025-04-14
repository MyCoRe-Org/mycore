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

package org.mycore.ocfl.niofs;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRPermutationExtension.class, MCROCFLSetupExtension.class })
@MCROCFLSetupExtension.LoadDefaultDerivate(false)
@MCRTestConfiguration(
    properties = {
        // set jpa mappings, only use mycore-base
        @MCRTestProperty(key = "MCR.JPA.MappingFileNames", string = "META-INF/mycore-base-mappings.xml"),
        // use ocfl metadata
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
        // set free access
        @MCRTestProperty(key = "MCR.Access.Class", string = "org.mycore.access.MCRAccessBaseImpl"),
        @MCRTestProperty(key = "MCR.Access.Strategy.Class",
            string = "org.mycore.ocfl.niofs.MCROCFLDerivateTest$AlwaysTrueStrategy")
    })
public class MCROCFLDerivateTest {

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    @PermutedParam
    private boolean purge;

    @TestTemplate
    public void create() throws Exception {
        MCRObject object = createObject("junit_object_00000001");
        MCRDerivate derivate = createDerivate(object.getId().toString(), "junit_derivate_00000001");
        MCRMetadataManager.create(object);

        MCRTransactionManager.requireTransactions(MCROCFLFileSystemTransaction.class);
        MCRMetadataManager.create(derivate);
        MCROCFLTestCaseHelper.loadDerivate(derivate.getId().toString());
        MCRTransactionManager.commitTransactions(MCROCFLFileSystemTransaction.class);
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
