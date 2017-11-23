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

package org.mycore.services.packaging;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.mycore.common.config.MCRConfiguration;

public class MCRPackerMock extends MCRPacker {

    public static final String TEST_VALUE = "testValue";

    public static final String TEST_CONFIGURATION_KEY = "testConfiguration";

    public static final String TEST_PARAMETER_KEY = "testParameter";

    public static final String FINISHED_PROPERTY = MCRPackerMock.class + ".finished";

    public static final String SETUP_CHECKED_PROPERTY = MCRPackerMock.class + ".checked";

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
