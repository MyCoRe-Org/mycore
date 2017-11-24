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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRConfiguration;

public class MCRTestCase {

    String oldProperties;

    @ClassRule
    public static TemporaryFolder junitFolder = new TemporaryFolder();

    protected MCRConfiguration config;

    @BeforeClass
    public static void initBaseDir() throws IOException {
        MCRTestCaseHelper.beforeClass(junitFolder);
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
        MCRTestCaseHelper.before(getTestProperties());
        config = MCRConfiguration.instance();
    }

    @After
    public void tearDown() throws Exception {
        MCRTestCaseHelper.after();
    }

    protected Map<String, String> getTestProperties() {
        return new HashMap<>();
    }

    /**
     * Creates a temporary properties file if the system variable MCR.Configuration.File is not set.
     *
     * @throws IOException
     *             Thrown if the creation of the temporary properties file failed.
     * @author Marcel Heusinger &lt;marcel.heusinger[at]uni-due.de&gt;
     */
    protected void initProperties() throws IOException {
        String currentComponent = getCurrentComponentName();
        System.setProperty("MCRRuntimeComponentDetector.underTesting", currentComponent);
    }

    protected static String getCurrentComponentName() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir).getFileName().toString();
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
     * Retrieve the resource file<br> Example: /Classname/recource.file
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
