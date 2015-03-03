package org.mycore.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRConfiguration;
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
        if (System.getProperties().getProperty("MCR.Home") == null) {
            File baseDir = junitFolder.newFolder("mcrhome");
            System.out.println("Setting MCR.Home=" + baseDir.getAbsolutePath());
            System.getProperties().setProperty("MCR.Home", baseDir.getAbsolutePath());
        }
        if (System.getProperties().getProperty("MCR.AppName") == null) {
            String  currentComponentName = getCurrentComponentName();
            System.out.println("Setting MCR.AppName="+ currentComponentName);
            System.getProperties().setProperty("MCR.AppName", getCurrentComponentName());
        }
        File configDir=new File(System.getProperties().getProperty("MCR.Home"), System.getProperties().getProperty("MCR.AppName"));
        System.out.println("Creating config directory: "+ configDir);
        configDir.mkdirs();
    }

    /**
     * initializes MCRConfiguration with an empty property file. This can be used to test MyCoRe classes without any
     * propties set, using default. You may want to set Properties per TestCase with the set() method of
     * <code>MCRConfiguration</code>
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
     * Creates a temporary properties file if the system variable MCR.Configuration.File is not set.
     * 
     * @throws IOException
     *             Thrown if the creation of the temporary properties file failed.
     * @author Marcel Heusinger <marcel.heusinger[at]uni-due.de>
     */
    protected void initProperties() throws IOException {
        String currentComponent = getCurrentComponentName();
        System.setProperty("MCRRuntimeComponentDetector.underTesting", currentComponent);
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

    private static String getCurrentComponentName() {
        String userDir = System.getProperty("user.dir");
        String currentComponent = Paths.get(userDir).getFileName().toString();
        return currentComponent;
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
     * Retrieve the resource file</br> Example: /Classname/recource.file
     * 
     * @param fileName
     * @return the resource file as InputStream
     */
    protected InputStream getResourceAsStream(String fileName) {
        String fileLocation = buildFileLocation(fileName);
        System.out.println("File location: " + fileLocation);
        return Class.class.getResourceAsStream(fileLocation);
    }

    /**
     * Retrieve the resource file as URI. Example: /Classname/recource.file
     * 
     * @param fileName
     * @return the resource file as URL
     */
    protected URL getResourceAsURL(String fileName) {
        String fileLocation = buildFileLocation(fileName);
        System.out.println("File location: " + fileLocation);
        return Class.class.getResource(fileLocation);
    }

    private String buildFileLocation(String fileName) {
        return MessageFormat.format("/{0}/{1}", this.getClass().getSimpleName(), fileName);
    }

}
