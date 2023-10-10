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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.config.MCRConfigurationException;

@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRTestCase {

    @ClassRule
    public static TemporaryFolder junitFolder = new TemporaryFolder();

    @Rule
    public MCRTestAnnotationWatcher<MCRTestConfiguration> configurationTestWatcher
        = new MCRTestAnnotationWatcher<>(MCRTestConfiguration.class);

    @BeforeClass
    public static void initBaseDir() throws IOException {
        MCRTestCaseHelper.beforeClass(junitFolder);
    }

    /**
     * Initializes {@link org.mycore.common.config.MCRConfiguration2} with an empty property file.
     * This can be used to test MyCoRe classes without any properties set, using default. You may set specific
     * properties for a test case by, in this order of precedence:
     * <ol>
     *     <li>
     *         Annotating the test class (or its superclasses) with {{@link MCRTestConfiguration}}.
     *     </li>
     *     <li>
     *         Overwriting  {@link MCRTestCase#getTestProperties()} in the test class.
     *     </li>
     *     <li>
     *         Annotating the test method with {{@link MCRTestConfiguration}}.
     *     </li>
     *     <li>
     *         Calling {@link org.mycore.common.config.MCRConfiguration2#set(String, String)} inside of the test method.
     *     </li>
     * </ol>
     */
    @Before
    public void setUp() throws Exception {
        initSystemProperties();
        Map<String, String> testProperties = getCombinedTestProperties();
        MCRTestCaseHelper.before(testProperties);
    }

    private Map<String, String> getCombinedTestProperties() {
        Map<String, String> testProperties = new HashMap<>();
        getClassLevelTestConfigurations().descendingIterator().forEachRemaining(
            testConfiguration -> extendTestProperties(testProperties, testConfiguration));
        testProperties.putAll(getTestProperties());
        getMethodLevelTestConfiguration().ifPresent(
            testConfiguration -> extendTestProperties(testProperties, testConfiguration));
        return testProperties;
    }

    private Deque<MCRTestConfiguration> getClassLevelTestConfigurations() {
        Deque<MCRTestConfiguration> testConfigurations = new ArrayDeque<>();
        for (Class<?> t = this.getClass(); t != Object.class; t = t.getSuperclass()) {
            Optional.ofNullable(t.getAnnotation(MCRTestConfiguration.class))
                .ifPresent(testConfigurations::add);
        }
        return testConfigurations;
    }

    protected Map<String, String> getTestProperties() {
        return new HashMap<>();
    }

    private Optional<MCRTestConfiguration> getMethodLevelTestConfiguration() {
        return configurationTestWatcher.getAnnotation();
    }

    private void extendTestProperties(Map<String, String> testProperties, MCRTestConfiguration testConfiguration) {
        Arrays.stream(testConfiguration.properties())
            .forEach(testProperty -> extendTestProperties(testProperties, testProperty));
    }

    private static void extendTestProperties(Map<String, String> testProperties, MCRTestProperty testProperty) {

        String stringValue = testProperty.string();
        boolean nonDefaultString = !Objects.equals(stringValue, "");

        Class<?> classNameOfValue = testProperty.classNameOf();
        boolean nonDefaultClassNameOf = Void.class != classNameOfValue;

        if (nonDefaultString && nonDefaultClassNameOf) {
            throw new MCRConfigurationException("MCRTestProperty can have either a string- or a classNameOf-value");
        } else if (nonDefaultString) {
            testProperties.put(testProperty.key(), stringValue);
        } else if (nonDefaultClassNameOf) {
            testProperties.put(testProperty.key(), classNameOfValue.getName());
        } else {
            throw new MCRConfigurationException("MCRTestProperty must have either a string- or a classNameOf-value");
        }

    }

    @After
    public void tearDown() throws Exception {
        MCRTestCaseHelper.after();
    }

    /**
     * Creates a temporary properties file if the system variable MCR.Configuration.File is not set.
     *
     * @author Marcel Heusinger &lt;marcel.heusinger[at]uni-due.de&gt;
     */
    protected void initSystemProperties() {
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
        return getClass().getResource(fileLocation);
    }

    private String buildFileLocation(String fileName) {
        return new MessageFormat("/{0}/{1}", Locale.ROOT).format(
            new Object[] { this.getClass().getSimpleName(), fileName });
    }

}
