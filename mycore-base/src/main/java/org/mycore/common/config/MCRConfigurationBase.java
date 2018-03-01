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

package org.mycore.common.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPropertiesResolver;

public final class MCRConfigurationBase {
    static final Pattern PROPERTY_SPLITTER = Pattern.compile(",");

    /**
     * The properties instance that stores the values that have been read from every configuration file. These
     * properties are unresolved
     */
    private static MCRProperties baseProperties = new MCRProperties();

    /**
     * The same as baseProperties but all %properties% are resolved.
     */
    private static MCRProperties resolvedProperties = new MCRProperties();

    /**
     * List of deprecated properties with their new name
     */
    private static MCRProperties deprecatedProperties = new MCRProperties();

    private static File lastModifiedFile;

    static {
        try {
            loadDeprecatedProperties();
            createLastModifiedFile();
        } catch (IOException e) {
            throw new MCRConfigurationException("Could not initialize MyCoRe configuration", e);
        }
    }

    private MCRConfigurationBase() {
    }

    /**
     * returns the last point in time when the MyCoRe system was last modified. This method can help you to validate
     * caches not under your controll, e.g. client caches.
     *
     * @see System#currentTimeMillis()
     */
    public static final long getSystemLastModified() {
        return lastModifiedFile.lastModified();
    }

    /**
     * signalize that the system state has changed. Call this method when ever you changed the persistency layer.
     */
    public static final void systemModified() {
        if (!lastModifiedFile.exists()) {
            try {
                createLastModifiedFile();
            } catch (IOException ioException) {
                throw new MCRException("Could not change modify date of file " + lastModifiedFile.getAbsolutePath(),
                    ioException);
            }
        } else if (!lastModifiedFile.setLastModified(System.currentTimeMillis())) {
            // a problem occurs, when a linux user other than the file owner
            // tries to change the last modified date
            // @see Java Bug:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4466073
            // fixable in Java7 with setTimes() method of new file system API
            // workaround for now: try to recreate the file
            // @author Robert Stephan
            FileOutputStream fout = null;
            try {
                try {
                    fout = new FileOutputStream(lastModifiedFile);
                    fout.write(new byte[0]);
                    lastModifiedFile.setWritable(true, false);
                } finally {
                    if (fout != null) {
                        fout.close();
                    }
                }
            } catch (IOException e) {
                throw new MCRException("Could not change modify date of file " + lastModifiedFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Creates a new .systemTime file in MCR.datadir.
     */
    private static synchronized void createLastModifiedFile() throws IOException {
        final String dataDirKey = "MCR.datadir";
        if (getResolvedProperties().containsKey(dataDirKey)) {
            Optional<File> dataDir = getString(dataDirKey)
                .map(File::new);
            if (dataDir.filter(File::exists)
                .filter(File::isDirectory).isPresent()) {
                lastModifiedFile = dataDir.map(p -> new File(p, ".systemTime")).get();
            } else {
                dataDir.ifPresent(d -> System.err.println("WARNING: MCR.dataDir does not exist: " + d.getAbsolutePath()));
            }
        }
        if (lastModifiedFile == null) {
            try {
                lastModifiedFile = File.createTempFile("MyCoRe", ".systemTime");
                lastModifiedFile.deleteOnExit();
            } catch (IOException e) {
                throw new MCRException("Could not create temporary file, please set property MCR.datadir");
            }
        }
        if (!lastModifiedFile.exists()) {
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(lastModifiedFile);
                fout.write(new byte[0]);
            } finally {
                if (fout != null) {
                    fout.close();
                }
            }
            //allow other users to change this file
            lastModifiedFile.setWritable(true, false);
        }
    }

    private static void debug() {
        Properties tmp = null;
        String comments = "Active mycore properties";
        File resolvedPropertiesFile = MCRConfigurationDir.getConfigFile("mycore.resolved.properties");
        if (resolvedPropertiesFile != null) {
            tmp = MCRConfiguration.sortProperties(getResolvedProperties());
            try (FileOutputStream fout = new FileOutputStream(resolvedPropertiesFile)) {
                tmp.store(fout, comments + "\nDo NOT edit this file!");
            } catch (IOException e) {
                LogManager.getLogger()
                    .warn("Could not store resolved properties to {}", resolvedPropertiesFile.getAbsolutePath(),
                        e);
            }
        }

        Logger logger = LogManager.getLogger();
        if (logger.isDebugEnabled()) {
            try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
                tmp = tmp == null ? MCRConfiguration.sortProperties(getResolvedProperties()) : tmp;
                tmp.store(out, comments);
                out.flush();
                sw.flush();
                logger.debug(sw.toString());
            } catch (IOException e) {
                logger.debug("Error while debugging mycore properties.", e);
            }
        }
    }

    /**
     * Substitute all %properties%.
     */
    protected static synchronized void resolveProperties() {
        MCRProperties tmpProperties = MCRProperties.copy(getBaseProperties());
        MCRPropertiesResolver resolver = new MCRPropertiesResolver(tmpProperties);
        resolvedProperties = MCRProperties.copy(resolver.resolveAll(tmpProperties));
    }

    private static void checkForDeprecatedProperties(Map<String, String> props) {
        Map<String, String> depUsedProps = props.entrySet().stream()
            .filter(e -> getDeprecatedProperties().containsKey(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> getDeprecatedProperties().getAsMap().get(e.getKey())));
        if (!depUsedProps.isEmpty()) {
            throw new MCRConfigurationException(
                depUsedProps.entrySet().stream().map(e -> e.getKey() + " ==> " + e.getValue())
                    .collect(Collectors.joining("\n",
                        "Found deprecated properties that are defined but will NOT BE USED. Please use the replacements:\n",
                        "\n")));
        }
    }

    private static void checkForDeprecatedProperty(String name) throws MCRConfigurationException {
        if (getDeprecatedProperties().containsKey(name)) {
            throw new MCRConfigurationException("Cannot set deprecated property " + name + ". Please use "
                + getDeprecatedProperties().getProperty(name) + " instead.");
        }
    }

    protected static MCRProperties getResolvedProperties() {
        return resolvedProperties;
    }

    protected static MCRProperties getBaseProperties() {
        return baseProperties;
    }

    protected static MCRProperties getDeprecatedProperties() {
        return deprecatedProperties;
    }

    /**
     * Returns the configuration property with the specified name as a String.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String
     * @throws MCRConfigurationException
     *             if the properties are not initialized
     */
    public static Optional<String> getString(String name) {
        if (Objects.requireNonNull(name, "MyCoRe property name must not be null.").trim().isEmpty()) {
            throw new MCRConfigurationException("MyCoRe property name must not be empty.");
        }
        if (name.trim() != name) {
            throw new MCRConfigurationException(
                "MyCoRe property name must not contain trailing or leading whitespaces: '" + name + "'");
        }
        if (getBaseProperties().isEmpty()) {
            throw new MCRConfigurationException("MCRConfiguration is still not initialized");
        }
        return getStringUnchecked(name);
    }

    static Optional<String> getStringUnchecked(String name) {
        checkForDeprecatedProperty(name);
        return Optional.ofNullable(getResolvedProperties().getProperty(name, null));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * String</CODE> value. If the parameter <CODE>value</CODE> is <CODE>
     * null</CODE>, the property will be deleted.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property, possibly <CODE>
     *            null</CODE>
     */
    static void set(String name, String value) {
        checkForDeprecatedProperty(name);
        if (value == null) {
            getBaseProperties().remove(name);
        } else {
            getBaseProperties().setProperty(name, value);
        }
        resolveProperties();
    }

    public static synchronized void initialize(Map<String, String> props, boolean clear) {
        checkForDeprecatedProperties(props);
        if (clear) {
            getBaseProperties().clear();
        } else {
            getBaseProperties().entrySet()
                .removeIf(e -> props.containsKey(e.getKey()) && props.get(e.getKey()) == null);
        }
        getBaseProperties().putAll(
            props.entrySet()
                .stream()
                .filter(e -> e.getKey() != null)
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        resolveProperties();
        debug();
    }

    /**
     * Loads file deprecated.properties that can be used to rename old properties. The file contains a list of renamed
     * properties: OldPropertyName=NewPropertyName. The old property is automatically replaced with the new name, so
     * that existing mycore.properties files must not be migrated immediately.
     */
    private static synchronized void loadDeprecatedProperties() throws IOException {
        try (InputStream in = MCRConfigurationBase.class.getResourceAsStream("/deprecated.properties")){
            if (in == null) {
                return;
            }
            deprecatedProperties.load(in);
        }
    }
}
