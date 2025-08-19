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

package org.mycore.pi.urn.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.backend.hibernate.MCRHIBLinkTableStore;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRLinkTableEventHandler;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRUUIDURNGenerator;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

/**
 * Created by chi on 09.03.17.
 * @author Huu Chi Vu
 */
@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.datadir", string = "%MCR.basedir%/data"),
    @MCRTestProperty(key = "MCR.Persistence.LinkTable.Store.Class", classNameOf = MCRHIBLinkTableStore.class),
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Access.Strategy.Class",
        classNameOf = MCRURNGranularRESTServiceTest.AlwaysTrueStrategy.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true"),
    @MCRTestProperty(key = "MCR.Metadata.Type.derivate", string = "true"),
    @MCRTestProperty(key = "MCR.IFS2.Store.mycore_derivate.Class", classNameOf = MCRMetadataStore.class),
    @MCRTestProperty(key = "MCR.IFS2.Store.mycore_derivate.SlotLayout", string = "4-2-2"),
    @MCRTestProperty(key = "MCR.EventHandler.MCRDerivate.020.Class", classNameOf = MCRXMLMetadataEventHandler.class),
    @MCRTestProperty(key = "MCR.EventHandler.MCRDerivate.030.Class", classNameOf = MCRLinkTableEventHandler.class),
    @MCRTestProperty(key = "MCR.PI.Generator.UUID", classNameOf = MCRUUIDURNGenerator.class),
    @MCRTestProperty(key = "MCR.PI.Generator.UUID.Namespace", string = "frontend-"),
    @MCRTestProperty(key = "MCR.PI.DNB.Credentials.Login", string = "test"),
    @MCRTestProperty(key = "MCR.PI.DNB.Credentials.Password", string = "test")
})
public class MCRURNGranularRESTServiceTest {
    private static final Logger LOGGER = LogManager.getLogger();

    private int numOfDerivFiles = 15;

    private MCRObject object;

    private MCRDerivate derivate;

    @BeforeEach
    public void setUp(MCRMetadataExtension.BaseDirs baseDirs) throws Exception {
        MCRConfiguration2.set("MCR.IFS.ContentStore.IFS2.BaseDir", baseDirs.storeBaseDir().toString());
        MCREventManager.getInstance().clear().addEventHandler(MCREvent.ObjectType.OBJECT,
            new MCRXMLMetadataEventHandler());
    }

    @Test
    public void fullRegister(MCRMetadataExtension.BaseDirs baseDirs) throws Exception {
        LOGGER.info("Store BaseDir {}", baseDirs.storeBaseDir());
        LOGGER.info("Store SVN Base {}", baseDirs.svnBaseDir());
        object = createObject();
        derivate = createDerivate(object.getId());
        MCRMetadataManager.create(object);
        MCRMetadataManager.create(derivate);

        List<MCRPath> fileList = new ArrayList<>();

        for (int j = 0; j < numOfDerivFiles; j++) {
            String fileName = UUID.randomUUID() + "_" + String.format(Locale.getDefault(), "%02d", j);
            MCRPath path = MCRPath.getPath(derivate.getId().toString(), fileName);
            fileList.add(path);
            if (!Files.exists(path)) {
                Files.writeString(path, "test_file");
                derivate.getDerivate().getOrCreateFileMetadata(path);
            }
        }

        Function<MCRDerivate, Stream<MCRPath>> foo = deriv -> fileList.stream();
        String serviceID = "TestService";
        MCRURNGranularRESTService testService = new MCRURNGranularRESTService(foo);
        testService.init("MCR.PI.Service.TestService");
        testService.setProperties(getTestServiceProperties());
        testService.register(derivate, "", true);

        timerTask();

        List<MCRPIRegistrationInfo> registeredURNs = MCREntityManagerProvider
            .getEntityManagerFactory()
            .createEntityManager()
            .createNamedQuery("Get.PI.Created", MCRPIRegistrationInfo.class)
            .setParameter("mcrId", derivate.getId().toString())
            .setParameter("type", MCRDNBURN.TYPE)
            .setParameter("service", serviceID)
            .getResultList();

        registeredURNs.stream()
            .forEach(pi -> LOGGER.info("URN: {}", pi));
        assertEquals(numOfDerivFiles + 1, registeredURNs.size(), "Wrong number of registered URNs: ");
    }

    public void timerTask() {
        System.out.println("Start: " + new Date());

        new MCRURNGranularRESTRegistrationCronjob().runJob();

        System.out.println("End: " + new Date());
    }

    protected Map<String, String> getTestServiceProperties() {
        HashMap<String, String> serviceProps = new HashMap<>();

        serviceProps.put("Generator", "UUID");
        serviceProps.put("supportDfgViewerURN", Boolean.TRUE.toString());

        return serviceProps;
    }

    public static MCRDerivate createDerivate(MCRObjectID objectHrefId) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("mycore_derivate"));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath("");
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID();
        mcrMetaLinkID.setReference(objectHrefId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    public static MCRObject createObject() {
        MCRObject object = new MCRObject();
        object.setId(MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId("mycore_object"));
        object.setSchema("noSchema");
        return object;
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Force garbage collection 
        // (may close some open Windows file handles when calling finalize() and avoid AccessDeniedExceptions)
        System.gc();
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }
}
