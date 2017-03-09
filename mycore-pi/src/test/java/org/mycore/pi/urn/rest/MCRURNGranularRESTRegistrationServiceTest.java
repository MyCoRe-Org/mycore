package org.mycore.pi.urn.rest;

import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.pi.MCRPIUtils;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by chi on 09.03.17.
 * @author Huu Chi Vu
 */
public class MCRURNGranularRESTRegistrationServiceTest extends MCRStoreTestCase {
    @Test
    public void fullRegister() throws Exception {
        MCRURNGranularRESTRegistrationService testService = new MCRURNGranularRESTRegistrationService("TestService");
        MCRDerivate derivate = new MCRDerivate();
        derivate.setId(MCRPIUtils.getNextFreeID());
        testService.fullRegister(derivate, "");

//        fail("Please test!");
    }

    @Test
    public void validateAlreadyInscribed() throws Exception {

    }

    @Test
    public void registerIdentifier() throws Exception {

    }

    @Test
    public void delete() throws Exception {

    }

    @Test
    public void update() throws Exception {

    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.PI.Generator.testGenerator.Namespace", "frontend-");
        return testProperties;
    }
}