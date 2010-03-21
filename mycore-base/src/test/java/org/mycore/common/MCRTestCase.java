package org.mycore.common;

import java.io.File;

import org.junit.After;
import org.junit.Before;

public class MCRTestCase {
    File properties = null;

    static MCRConfiguration CONFIG;

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
        if (System.getProperties().getProperty("MCR.Configuration.File") == null) {
            properties = File.createTempFile("test", ".properties");
            System.getProperties().setProperty("MCR.Configuration.File", properties.getAbsolutePath());
        }
        CONFIG = MCRConfiguration.instance();
        boolean setProperty = false;
        if (isDebugEnabled()) {
            setProperty = setProperty("log4j.rootLogger", "DEBUG, stdout", false) ? true : setProperty;
        } else {
            setProperty = setProperty("log4j.rootLogger", "INFO, stdout", false) ? true : setProperty;
        }
        setProperty = setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender", false) ? true : setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout", false) ? true : setProperty;
        setProperty = setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5p %m%n", false) ? true : setProperty;
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

    protected boolean setProperty(String key, String value, boolean overwrite) {
        String propValue = CONFIG.getProperties().getProperty(key);
        if (propValue == null || overwrite == true) {
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
}