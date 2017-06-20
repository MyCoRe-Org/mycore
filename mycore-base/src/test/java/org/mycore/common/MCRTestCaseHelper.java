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
        MCRConfiguration.instance().initialize(Collections.<String, String> emptyMap(), true);
        MCRSessionMgr.releaseCurrentSession();
    }

    public static String getCurrentComponentName() {
        String userDir = System.getProperty("user.dir");
        String currentComponent = Paths.get(userDir).getFileName().toString();
        return currentComponent;
    }

}
