package org.mycore.common;

import java.io.File;

import junit.framework.TestCase;

public class MCRTestCase extends TestCase {
    File properties=null;;
    static MCRConfiguration CONFIG; 

    /**
     * initializes MCRConfiguration with an empty property file.
     * 
     * This can be used to test MyCoRe classes without any propties set, using default.
     * You may want to set Properties per TestCase with the getProperties() method of <code>MCRConfiguration</code>
     * 
     * @see MCRConfiguration#getProperties()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if (System.getProperties().getProperty("MCR.configuration.file")==null){
            properties=File.createTempFile("test",".properties");
            System.getProperties().setProperty("MCR.configuration.file",properties.getAbsolutePath());
        }
        CONFIG=MCRConfiguration.instance();
        boolean setPropertie=false;
        setPropertie=setProperty("MCR.log4j.rootLogger","DEBUG, stdout",false)?true:setPropertie;
        setPropertie=setProperty("MCR.log4j.appender.stdout","org.apache.log4j.ConsoleAppender",false)?true:setPropertie;
        setPropertie=setProperty("MCR.log4j.appender.stdout.layout","org.apache.log4j.PatternLayout",false)?true:setPropertie;
        setPropertie=setProperty("MCR.log4j.appender.stdout.layout.ConversionPattern","%-5p %m%n",false)?true:setPropertie;
        if (setPropertie){
            CONFIG.configureLogging();
        }
   }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        if (properties!=null){
            properties.delete();
        }
    }
    
    protected boolean setProperty(String key, String value, boolean overwrite){
        String propValue=CONFIG.getProperties().getProperty(key);
        if ((propValue==null) || (overwrite==true)){
            CONFIG.getProperties().setProperty(key,value);
            return true;
        }
        return false;
    }
}