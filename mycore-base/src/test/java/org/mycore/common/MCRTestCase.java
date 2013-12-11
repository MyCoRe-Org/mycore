package org.mycore.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

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
     * getProperties() method of <code>MCRConfiguration</code>
     * 
     * @see MCRConfiguration#getProperties()
     */
    @Before
    public void setUp() throws Exception {
        initProperties();
        config = MCRConfiguration.instance();
        config.reload(true);
        boolean setProperty = false;
        if (isDebugEnabled()) {
            setProperty = setProperty("log4j.rootLogger", "DEBUG, stdout", false) || setProperty;
        } else {
            setProperty = setProperty("log4j.rootLogger", "INFO, stdout", false) || setProperty;
        }
        setProperty = setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender", false) || setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout", false)
            || setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5p %c{1} %m%n", true)
            || setProperty;
        if (setProperty) {
            config.configureLogging();
        }
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
    }

    /**
     * Creates a temporary properties file if the system variable MCR.Configuration.File
     * is not set.
     * @throws IOException Thrown if the creation of the temporary properties file failed.
     * @author Marcel Heusinger <marcel.heusinger[at]uni-due.de>
     */
    protected void initProperties() throws IOException {
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

    protected boolean setProperty(String key, String value, boolean overwrite) {
        String propValue = config.getProperties().getProperty(key);
        if (propValue == null || overwrite) {
            config.getProperties().setProperty(key, value);
            return true;
        }
        return false;
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
