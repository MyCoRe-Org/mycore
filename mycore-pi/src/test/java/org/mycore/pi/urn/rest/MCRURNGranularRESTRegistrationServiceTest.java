package org.mycore.pi.urn.rest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetaIFS;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.MockMetadataManager;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRUUIDURNGenerator;

import mockit.Mock;
import mockit.MockUp;

/**
 * Created by chi on 09.03.17.
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationServiceTest extends MCRStoreTestCase {
    private int numOfDerivFiles = 15;

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void fullRegister() throws Exception {
        new MockContentTypes();
        new MockFrontendUtil();
        new MockFiles();
        new MockAccessManager();
        new MockObjectDerivate();
        new MockDerivate();
        MockMetadataManager mockMetadataManager = new MockMetadataManager();

        MCRDerivate derivate = new MCRDerivate();
        MCRObjectID mcrObjectID = MCRPIUtils.getNextFreeID();
        derivate.setId(mcrObjectID);

        mockMetadataManager.put(mcrObjectID, derivate);

        Function<MCRDerivate, Stream<MCRPath>> foo = deriv -> {
            return IntStream
                .iterate(0, i -> i + 1)
                .mapToObj(i -> {
                    return "/foo/" + UUID.randomUUID().toString() + "_" + String
                        .format(Locale.getDefault(), "%02d", i);
                })
                .map(f -> MCRPath.getPath(derivate.getId().toString(), f))
                .limit(numOfDerivFiles);
        };
        String serviceID = "TestService";
        MCRURNGranularRESTRegistrationService testService = new MCRURNGranularRESTRegistrationService(serviceID,
            foo);
        testService.register(derivate, "", true);
        timerTask();

        List<MCRPIRegistrationInfo> registeredURNs = MCREntityManagerProvider
            .getEntityManagerFactory()
            .createEntityManager()
            .createNamedQuery("Get.PI.Created", MCRPIRegistrationInfo.class)
            .setParameter("mcrId", mcrObjectID.toString())
            .setParameter("type", MCRDNBURN.TYPE)
            .setParameter("service", serviceID)
            .getResultList();

        Assert.assertEquals("Wrong number of registered URNs: ", numOfDerivFiles, registeredURNs.size());
    }

    public void timerTask() throws Exception {
        System.out.println("Start: " + new Date());

        MCRURNGranularRESTRegistrationStarter starter = new MCRURNGranularRESTRegistrationStarter(2, TimeUnit.SECONDS);
        starter.startUp(null);
        Thread.sleep(12000);

        System.out.println("End: " + new Date());

    }

    public class MockObjectDerivate extends MockUp<MCRObjectDerivate> {
        @Mock
        public MCRFileMetadata getOrCreateFileMetadata(MCRPath file, String urn) {
            System.out.println("getOrCreateFileMetadata: " + file.toString() + " - " + urn);
            return new MCRFileMetadata(file.getOwnerRelativePath(), urn, null);
        }

        @Mock
        public MCRMetaIFS getInternals() {
            MCRMetaIFS mcrMetaIFS = new MCRMetaIFS();
            mcrMetaIFS.setMainDoc("mainDoc_" + UUID.randomUUID().toString());
            return mcrMetaIFS;
        }
    }

    public class MockDerivate extends MockUp<MCRDerivate> {
        @Mock
        public void validate() throws MCRException {
            // allways valid
        }
    }

    public class MockAccessManager extends MockUp<MCRAccessManager> {
        @Mock
        public boolean checkPermission(MCRObjectID id, String permission) {
            return true;
        }
    }

    public class MockFiles extends MockUp<Files> {
        @Mock
        public boolean exists(Path path, LinkOption... options) {
            return true;
        }
    }

    public class MockFrontendUtil extends MockUp<MCRFrontendUtil> {
        @Mock
        public void prepareBaseURLs(String baseURL) {
            System.out.println("prepare nothing");
        }

        @Mock
        private TreeSet<String> getTrustedProxies() {
            return new TreeSet<>();
        }

        @Mock
        public String getBaseURL() {
            return "http://localhost:8291/";
        }
    }

    public class MockContentTypes extends MockUp<MCRContentTypes> {
        @Mock
        public String probeContentType(Path path) throws IOException {
            return "";
        }
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Registration.TestService.Generator", "UUID");
        testProperties.put("MCR.PI.Registration.TestService.supportDfgViewerURN", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.UUID", MCRUUIDURNGenerator.class.getName());
        testProperties.put("MCR.PI.Generator.UUID.Namespace", "frontend-");
        testProperties.put("MCR.URN.DNB.Credentials.Login", "test");
        testProperties.put("MCR.URN.DNB.Credentials.Password", "test");
        return testProperties;
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
