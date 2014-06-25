package org.mycore.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationLoader;
import org.mycore.common.config.MCRConfigurationLoaderFactory;

public class MCRTestCase {
    protected static final String MCR_CONFIGURATION_FILE = "MCR.Configuration.File";

    File properties = null;

    String oldProperties;

    @ClassRule
    public static TemporaryFolder junitFolder = new TemporaryFolder();

    protected MCRConfiguration config;

    @BeforeClass
    public static void initBaseDir() throws IOException {
        if (System.getProperties().getProperty("MCR.basedir") == null) {
            File baseDir = junitFolder.newFolder("baseDir");
            File dataDir = new File(baseDir, "data");
            dataDir.mkdir();
            System.out.println("Setting MCR.basedir=" + baseDir.getAbsolutePath());
            System.getProperties().setProperty("MCR.basedir", baseDir.getAbsolutePath());
        }
    }

    /**
     * initializes MCRConfiguration with an empty property file.
     * 
     * This can be used to test MyCoRe classes without any propties set, using
     * default. You may want to set Properties per TestCase with the
     * set() method of <code>MCRConfiguration</code>
     * 
     * @see MCRConfiguration#set(String, String)
     */
    @Before
    public void setUp() throws Exception {
        initProperties();
        config = MCRConfiguration.instance();
        MCRConfigurationLoader configurationLoader = MCRConfigurationLoaderFactory.getConfigurationLoader();
        HashMap<String, String> testProperties = new HashMap<>(configurationLoader.load());
        testProperties.putAll(getTestProperties());
        config.initialize(testProperties, true);
    }

    @After
    public void tearDown() throws Exception {
        if (properties != null) {
            properties.delete();
            properties = null;
        }
        if (oldProperties == null) {
            System.getProperties().remove(MCR_CONFIGURATION_FILE);
        } else {
            System.setProperty(MCR_CONFIGURATION_FILE, oldProperties);
        }
        MCRConfiguration.instance().initialize(Collections.<String, String> emptyMap(), true);
        System.getProperties().remove(MCRConfigurationDir.DISABLE_CONFIG_DIR_PROPERTY);
    }

    protected Map<String, String> getTestProperties() {
        HashMap<String, String> props = new HashMap<>();
        if (isDebugEnabled()) {
            props.put("log4j.rootLogger", "DEBUG, stdout");
        } else {
            props.put("log4j.rootLogger", "INFO, stdout");
        }
        props.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        props.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.stdout.layout.ConversionPattern", "%-5p %c{1} %m%n");
        return props;
    }

    /**
     * Creates a temporary properties file if the system variable MCR.Configuration.File
     * is not set.
     * @throws IOException Thrown if the creation of the temporary properties file failed.
     * @author Marcel Heusinger <marcel.heusinger[at]uni-due.de>
     */
    protected void initProperties() throws IOException {
        System.setProperty(MCRConfigurationDir.DISABLE_CONFIG_DIR_PROPERTY, "");
        oldProperties = System.getProperties().getProperty(MCR_CONFIGURATION_FILE);
        if (oldProperties == null && getClass().getClassLoader().getResource("mycore.properties") != null) {
            return;
        }
        properties = getPropertiesFile();
        if (System.getProperties().getProperty(MCR_CONFIGURATION_FILE) == null) {
            System.getProperties().setProperty(MCR_CONFIGURATION_FILE, properties.getAbsolutePath());
        }//if
        else if (properties.getAbsoluteFile().equals(System.getProperties().getProperty(MCR_CONFIGURATION_FILE))) {
            if (!properties.exists()) {
                throw new FileNotFoundException("File does not exist: " + properties.getAbsolutePath());
            }
        }
    }

    private File getPropertiesFile() throws IOException {
        File newFile = junitFolder.newFile("mycore.properties");
        System.out.println("Create new file: " + newFile);
        return newFile;
    }

    protected boolean isDebugEnabled() {
        return false;
    }

    /**
     * Waits 1,1 seconds and does nothing
     */
    protected void bzzz() {
        synchronized (this) {
            try {
                wait(1100);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Retrieve the resource file</br>
     * Example: /Classname/recource.file
     * 
     * @param fileName
     * @return 
     *        the resource file as InputStream
     */
    protected InputStream getResourceAsStream(String fileName) {
        String fileLocation = buildFileLocation(fileName);
        System.out.println("File location: " + fileLocation);
        return Class.class.getResourceAsStream(fileLocation);
    }

    private String buildFileLocation(String fileName) {
        String pathseparator = File.separator;
        return pathseparator + this.getClass().getSimpleName() + pathseparator + fileName;
    }

}
