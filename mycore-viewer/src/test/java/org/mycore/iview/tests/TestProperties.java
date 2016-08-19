package org.mycore.iview.tests;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class TestProperties extends Properties {

    private static final long serialVersionUID = -1135672087633884258L;
    private static final Logger LOGGER = Logger.getLogger(TestProperties.class);
    public static final String TEST_PROPERTIES = "test.properties";

    private TestProperties(){
        LOGGER.info("Load TestProperties");
        try {
            load(TestProperties.class.getClassLoader().getResourceAsStream(TEST_PROPERTIES));
        } catch (IOException e) {
            LOGGER.error("Error while loading test properties!");
        }
    }

    private static TestProperties singleton = null;

    public static synchronized TestProperties getInstance() {
        if(TestProperties.singleton == null){
            TestProperties.singleton = new TestProperties();
        }

        return TestProperties.singleton;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return System.getProperty(key, super.getProperty(key, defaultValue));
    }

    @Override
    public String getProperty(String key) {
        return System.getProperty(key, super.getProperty(key));
    }
}
