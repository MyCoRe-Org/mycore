/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.strategies.MCRAccessCheckStrategy;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
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

/**
 * Created by chi on 09.03.17.
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTServiceTest extends MCRStoreTestCase {
    private static Logger LOGGER = LogManager.getLogger();

    private int numOfDerivFiles = 15;

    private MCRObject object;

    private MCRDerivate derivate;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MCREventManager.instance().clear().addEventHandler(MCREvent.ObjectType.OBJECT,
            new MCRXMLMetadataEventHandler());
    }

    @Test
    public void fullRegister() throws Exception {
        LOGGER.info("Store BaseDir {}", getStoreBaseDir());
        LOGGER.info("Store SVN Base {}", getSvnBaseDir());
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
                Files.createFile(path);
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
        Assert.assertEquals("Wrong number of registered URNs: ", numOfDerivFiles + 1, registeredURNs.size());
    }

    public void timerTask() throws Exception {
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

        testProperties.put("MCR.IFS2.Store.mycore_derivate.Class", "org.mycore.datamodel.ifs2.MCRMetadataStore");
        //testProperties.put("MCR.IFS2.Store.mycore_derivate.BaseDir","/foo");
        testProperties.put("MCR.IFS2.Store.mycore_derivate.SlotLayout", "4-2-2");
        testProperties.put("MCR.EventHandler.MCRDerivate.020.Class",
            "org.mycore.datamodel.common.MCRXMLMetadataEventHandler");
        testProperties.put("MCR.EventHandler.MCRDerivate.030.Class",
            "org.mycore.datamodel.common.MCRLinkTableEventHandler");

        testProperties.put("MCR.IFS.ContentStore.IFS2.BaseDir", getStoreBaseDir().toString());
        testProperties.put("MCR.PI.Generator.UUID", MCRUUIDURNGenerator.class.getName());
        testProperties.put("MCR.PI.Generator.UUID.Namespace", "frontend-");
        testProperties.put("MCR.PI.DNB.Credentials.Login", "test");
        testProperties.put("MCR.PI.DNB.Credentials.Password", "test");

        return testProperties;
    }

    public static MCRDerivate createDerivate(MCRObjectID objectHrefId) {
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRObjectID.getNextFreeId("mycore_derivate"));
        derivate.setSchema("datamodel-derivate.xsd");
        MCRMetaIFS ifs = new MCRMetaIFS();
        ifs.setSubTag("internal");
        ifs.setSourcePath(MCRPath.getPath(derivate.getId().toString(), "/").toAbsolutePath().toString());
        derivate.getDerivate().setInternals(ifs);
        MCRMetaLinkID mcrMetaLinkID = new MCRMetaLinkID();
        mcrMetaLinkID.setReference(objectHrefId.toString(), null, null);
        derivate.getDerivate().setLinkMeta(mcrMetaLinkID);
        return derivate;
    }

    public static MCRObject createObject() {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getNextFreeId("mycore_object"));
        object.setSchema("noSchema");
        return object;
    }

    @After
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(object);
        super.tearDown();
    }

    public static class AlwaysTrueStrategy implements MCRAccessCheckStrategy {

        @Override
        public boolean checkPermission(String id, String permission) {
            return true;
        }

    }
}
