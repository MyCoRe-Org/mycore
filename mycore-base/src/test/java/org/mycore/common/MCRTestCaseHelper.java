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

package org.mycore.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;
import org.mycore.common.config.MCRRuntimeComponentDetector;

public class MCRTestCaseHelper {

    public static void beforeClass(TemporaryFolder junitFolder) throws IOException {
        if (System.getProperties().getProperty("MCR.Home") == null) {
            File baseDir = junitFolder.newFolder("mcrhome");
            System.out.println("Setting MCR.Home=" + baseDir.getAbsolutePath());
            System.getProperties().setProperty("MCR.Home", baseDir.getAbsolutePath());
        }
        if (System.getProperties().getProperty("MCR.AppName") == null) {
            String currentComponentName = getCurrentComponentName();
            System.out.println("Setting MCR.AppName=" + currentComponentName);
            System.getProperties().setProperty("MCR.AppName", getCurrentComponentName());
        }
        File configDir = new File(System.getProperties().getProperty("MCR.Home"),
            System.getProperties().getProperty("MCR.AppName"));
        System.out.println("Creating config directory: " + configDir);
        configDir.mkdirs();
    }

    public static void before(Map<String, String> testProperties) {
        String mcrComp = MCRRuntimeComponentDetector.getMyCoReComponents().stream().map(MCRComponent::toString).collect(
            Collectors.joining(", "));
        String appMod = MCRRuntimeComponentDetector.getApplicationModules()
            .stream()
            .map(MCRComponent::toString)
            .collect(Collectors.joining(", "));
        System.out.printf("MyCoRe components detected: %s\nApplications modules detected: %s\n",
            mcrComp.isEmpty() ? "'none'" : mcrComp, appMod.isEmpty() ? "'none'" : appMod);
        MCRConfiguration config = MCRConfiguration.instance();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        HashMap<String, String> baseProperties = new HashMap<>(configurationLoader.load());
        baseProperties.putAll(testProperties);
        config.initialize(baseProperties, true);
    }

    public static void after() {
        MCRConfiguration.instance().initialize(Collections.emptyMap(), true);
        MCRSessionMgr.releaseCurrentSession();
    }

    public static String getCurrentComponentName() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir).getFileName().toString();
    }

}
