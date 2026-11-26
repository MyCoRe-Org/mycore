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

package org.mycore.ocfl.metadata.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.jdom2.Document;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.common.MCRTrackingObjectIDGenerator;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.ocfl.MCROCFLTestCaseHelper;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.test.MCROCFLSetupExtension;
import org.mycore.ocfl.test.MCRPermutationExtension;
import org.mycore.ocfl.test.PermutedParam;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import io.ocfl.api.OcflObjectUpdater;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.VersionInfo;

/**
 * Migrates the metadata of the test repository into a second repository.
 */
@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRPermutationExtension.class, MCROCFLSetupExtension.class })
@MCROCFLSetupExtension.LoadDefaultDerivate(false)
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.JPA.MappingFileNames", string = "META-INF/mycore-base-mappings.xml"),
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
        @MCRTestProperty(key = "MCR.OCFL.Repository.Migration.Class",
            string = "org.mycore.ocfl.repository.MCROCFLHashRepositoryProvider"),
        @MCRTestProperty(key = "MCR.OCFL.Repository.Migration.RepositoryRoot",
            string = "%MCR.datadir%/ocfl-migration-root"),
        @MCRTestProperty(key = "MCR.OCFL.Repository.Migration.WorkDir",
            string = "%MCR.datadir%/ocfl-migration-temp")
    })
public class MCROCFLMigrationTest {

    private static final String OBJECT_ID = "junit_object_00000001";

    private static final String DERIVATE_ID = MCROCFLTestCaseHelper.DERIVATE_1;

    private static final String MIGRATION_REPOSITORY = "Migration";

    protected MCROCFLRepository repository;

    @PermutedParam
    private boolean remote;

    /**
     * An old store with history allowed to reuse the id of a deleted object, so the history can contain a delete
     * followed by a create. The migration writes the create on top of the delete version and keeps the history.
     */
    @TestTemplate
    public void migrateRecreatedIds() {
        MCROCFLRepository target = MCROCFLRepositoryProvider.getRepository(MIGRATION_REPOSITORY);
        // the migration repository is not reset by the setup extension
        target.listObjectIds().toList().forEach(target::purgeObject);
        String objectXml = toString(MCROCFLTestCaseHelper.createObject(OBJECT_ID).createXML());
        String derivateXml = toString(MCROCFLTestCaseHelper.createDerivate(OBJECT_ID, DERIVATE_ID).createXML());
        String ocflObjectId = MCROCFLObjectIDPrefixHelper.MCROBJECT + OBJECT_ID;
        String ocflDerivateId = MCROCFLObjectIDPrefixHelper.MCRDERIVATE + DERIVATE_ID;
        writeRecreatedHistory(ocflObjectId, "metadata/" + OBJECT_ID + ".xml", objectXml);
        writeRecreatedHistory(ocflDerivateId, "metadata/" + DERIVATE_ID + ".xml", derivateXml);
        // the migration iterates over the ids known to the id generator
        if (MCRMetadataManager.getMCRObjectIDGenerator() instanceof MCRTrackingObjectIDGenerator generator) {
            generator.recordID(MCRObjectID.getInstance(OBJECT_ID));
            generator.recordID(MCRObjectID.getInstance(DERIVATE_ID));
        }

        MCROCFLMigration migration = new MCROCFLMigration(MIGRATION_REPOSITORY);
        migration.start();

        assertEquals(List.of(), migration.getInvalidState(), "no object should end in an invalid state");
        assertTrue(migration.getSuccess().containsAll(List.of(OBJECT_ID, DERIVATE_ID)),
            "object and derivate should be migrated: " + migration.getSuccess());

        MCROCFLXMLMetadataManager targetManager = new MCROCFLXMLMetadataManager();
        targetManager.setRepositoryKey(MIGRATION_REPOSITORY);
        for (String id : List.of(ocflObjectId, ocflDerivateId)) {
            assertEquals(3, target.describeObject(id).getVersionMap().size(), "the whole history of " + id
                + " should be migrated");
        }
        for (String id : List.of(OBJECT_ID, DERIVATE_ID)) {
            MCRObjectID mcrid = MCRObjectID.getInstance(id);
            assertTrue(targetManager.exists(mcrid), id + " should exist after the migration");
        }
    }

    private void writeRecreatedHistory(String ocflObjectId, String path, String xml) {
        write(ocflObjectId, "Created", updater -> updater.writeFile(toStream(xml), path));
        write(ocflObjectId, "Deleted", updater -> updater.removeFile(path));
        write(ocflObjectId, "Created", updater -> updater.writeFile(toStream(xml), path));
    }

    private void write(String ocflObjectId, String message,
        Consumer<OcflObjectUpdater> change) {
        repository.updateObject(ObjectVersionId.head(ocflObjectId),
            new VersionInfo().setMessage(message).setUser("junit", null), change);
    }

    private static ByteArrayInputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static String toString(Document document) {
        return new XMLOutputter().outputString(document);
    }

}
