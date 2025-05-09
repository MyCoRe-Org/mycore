/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.iview.tests;

import java.io.IOException;
import java.io.Serial;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestProperties extends Properties {

    public static final String TEST_PROPERTIES = "test.properties";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    private static TestProperties singleton;

    private TestProperties() {
        LOGGER.info("Load TestProperties");
        try {
            load(TestProperties.class.getClassLoader().getResourceAsStream(TEST_PROPERTIES));
        } catch (IOException e) {
            LOGGER.error("Error while loading test properties!");
        }
    }

    public static synchronized TestProperties getInstance() {
        if (singleton == null) {
            singleton = new TestProperties();
        }

        return singleton;
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
