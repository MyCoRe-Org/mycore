package org.mycore.pi.urn.rest;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Test;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.*;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.MockMetadataManager;
import org.mycore.pi.urn.MCRUUIDURNGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by chi on 09.03.17.
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationServiceTest extends MCRStoreTestCase {
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

        Function<MCRDerivate, Stream<MCRPath>> foo = deriv -> IntStream
                .iterate(0, i -> i + 1)
                .mapToObj(i -> {
                    return "/foo/" + UUID.randomUUID().toString() + "_" + String.format(Locale.getDefault(), "%02d", i);
                })
                .map(f -> MCRPath.getPath(derivate.getId().toString(), f))
                .limit(15);
        MCRURNGranularRESTRegistrationService testService = new MCRURNGranularRESTRegistrationService("TestService",
                                                                                                      foo);
        testService.fullRegister(derivate, "");
        timerTask();
    }

    private static final String countRegistered = "select count(u) from MCRPI u "
            + "where u.type = :type "
            + "and u.registered is not null";

    public void timerTask() throws Exception {
        System.out.println("Start: " + new Date());

        MCRURNGranularRESTRegistrationStarter starter = new MCRURNGranularRESTRegistrationStarter(2, TimeUnit.SECONDS);
        starter.startUp(null);
        Thread.sleep(6000);

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
        public boolean exists(Path path, LinkOption... options) {return true;}
    }

    public class MockFrontendUtil extends MockUp<MCRFrontendUtil> {
        @Mock
        public void prepareBaseURLs(String baseURL) {
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
}