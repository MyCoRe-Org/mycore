package org.mycore.services.packaging;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.mycore.common.config.MCRConfiguration;

public class MCRPackerMock extends MCRPacker {

    public static final String TEST_VALUE = "testValue";

    public static final String TEST_CONFIGURATION_KEY = "testConfiguration";

    public static final String TEST_PARAMETER_KEY = "testParameter";

    public static final String FINISHED_PROPERTY = MCRPackerMock.class.toString() + ".finished";

    public static final String SETUP_CHECKED_PROPERTY = MCRPackerMock.class.toString() + ".checked";

    @Override
    public void checkSetup() {
        MCRConfiguration.instance().set(SETUP_CHECKED_PROPERTY, true);
    }

    @Override
    public void pack() throws ExecutionException {
        Map<String, String> configuration = getConfiguration();

        if (!configuration.containsKey(TEST_CONFIGURATION_KEY)
            && configuration.get("testConfiguration").equals(TEST_VALUE)) {
            throw new ExecutionException(new Exception(TEST_CONFIGURATION_KEY + " invalid!"));

        }

        if (!getParameters().containsKey(TEST_PARAMETER_KEY) && configuration.get("testParameter").equals(TEST_VALUE)) {
            throw new ExecutionException(new Exception(TEST_PARAMETER_KEY + " is invalid!"));
        }

        MCRConfiguration.instance().set(FINISHED_PROPERTY, true);
    }

    @Override
    public void rollback() {

    }

}
