package org.mycore.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MCRTestCase {
    File properties = null;

    static MCRConfiguration CONFIG = MCRConfiguration.instance();

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
        
    	initProperties ();
    	CONFIG = MCRConfiguration.instance();
        boolean setProperty = false;
        if (isDebugEnabled()) {
            setProperty = setProperty("log4j.rootLogger", "DEBUG, stdout", false) || setProperty;
        } else {
            setProperty = setProperty("log4j.rootLogger", "INFO, stdout", false) || setProperty;
        }
        setProperty = setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender", false) || setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout", false) || setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5p %m%n", false) || setProperty;
        if (setProperty) {
            CONFIG.configureLogging();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (properties != null) {
            properties.delete();
        }
    }

    /**
     * Creates a temporary properties file if the system variable MCR.Configuration.File
     * is not set.
     * @throws IOException Thrown if the creation of the temporary properties file failed.
     * @author Marcel Heusinger <marcel.heusinger[at]uni-due.de>
     */
    protected void initProperties () 
    	throws IOException {
    	if (System.getProperties().getProperty("MCR.Configuration.File") == null) {
            properties = File.createTempFile("test", ".properties");
            System.getProperties().setProperty("MCR.Configuration.File", properties.getAbsolutePath());
        }//if
    }//InitProperties

    protected static boolean setProperty(String key, String value, boolean overwrite) {
        String propValue = CONFIG.getProperties().getProperty(key);
        if (propValue == null || overwrite) {
            CONFIG.getProperties().setProperty(key, value);
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
    protected InputStream getResourceAsStream(String fileName){
        String fileLocation = buildFileLocation(fileName);
        System.out.println("File location: " + fileLocation);
        return Class.class.getResourceAsStream(fileLocation);
    }

    private String buildFileLocation(String fileName) {
        String pathseparator = File.separator;
        return pathseparator + this.getClass().getSimpleName() + pathseparator + fileName;
    }

	@Test
	public void testnothing() throws Exception {
		//required for JUnit4
	}
}