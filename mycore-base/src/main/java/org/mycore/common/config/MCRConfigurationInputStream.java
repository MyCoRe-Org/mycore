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

package org.mycore.common.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.SequencedMap;

import org.apache.logging.log4j.LogManager;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRURLContent;

/**
 * A InputStream from (preferably) property files. All available InputStreams are combined in this order:
 * <ol>
 * <li>mycore-base</li>
 * <li>other mycore-components</li>
 * <li>application modules</li>
 * <li>installation specific files</li>
 * </ol>
 *
 * @author Thomas Scheffler (yagee)
 * @author Robert Stephan
 * @since 2013.12
 */
@SuppressWarnings({"PMD.MCR.ResourceResolver", "PMD.UnusedAssignment"})
public class MCRConfigurationInputStream extends InputStream {

    private static final String MYCORE_PROPERTIES = "mycore.properties";

    // latin1 for properties
    private static final byte[] LINE_BREAK = System.lineSeparator().getBytes(StandardCharsets.ISO_8859_1);

    InputStream in;

    private Enumeration<? extends InputStream> e;

    private boolean empty;

    /**
     * Combined Stream of all config files named <code>filename</code> available via
     * {@link MCRRuntimeComponentDetector#getAllComponents()}.
     *
     * @param filename
     *            , e.g. mycore.properties or messages_de.properties
     */
    public MCRConfigurationInputStream(String filename) throws IOException {
        this(filename, null);
    }

    private MCRConfigurationInputStream(String filename, InputStream initStream) throws IOException {
        super();
        this.empty = true;
        this.e = getPropertyInputStreams(filename, initStream);
        if (e.hasMoreElements()) {
            nextStream();
        }
    }

        /**
         * {@link InputStream} that includes all properties from {@link MCRRuntimeComponentDetector#getAllComponents()}
         * and <strong>mycore.properties</strong>. Use system property <code>MCR.Configuration.File</code> to configure
         * alternative property file.
         *
         * @since 2014.04
         */
    public static MCRConfigurationInputStream ofMyCoReProperties() throws IOException {
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        InputStream initStream = null;
        if (configurationDirectory != null) {
            LogManager.getLogger().info("Current configuration directory: {}",
                configurationDirectory::getAbsolutePath);
            // set MCR.basedir, is normally overwritten later
            if (!configurationDirectory.isDirectory()) {
                LogManager.getLogger().warn("Current configuration directory does not exist: {}",
                    configurationDirectory::getAbsolutePath);
            }
            initStream = getBaseDirInputStream(configurationDirectory);
        }
        return new MCRConfigurationInputStream(MYCORE_PROPERTIES, initStream);
    }

    public boolean isEmpty() {
        return empty;
    }

    private Enumeration<? extends InputStream> getPropertyInputStreams(String filename, InputStream initStream)
        throws IOException {
        List<InputStream> cList = new ArrayList<>();
        if (initStream != null) {
            empty = false;
            cList.add(initStream);
        }
        for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
            InputStream is = component.getConfigFileStream(filename);
            if (is != null) {
                empty = false;
                String comment = "\n\n#\n#\n# Component: " + component.getName() + "\n#\n#\n";
                cList.add(new ByteArrayInputStream(comment.getBytes(StandardCharsets.ISO_8859_1)));
                cList.add(is);
                //workaround if last property is not terminated with line break
                cList.add(new ByteArrayInputStream(LINE_BREAK));
            } else {
                cList.add(new ByteArrayInputStream(
                    ("# Unable to find " + filename + " in " + component.getResourceBase() + "\n")
                        .getBytes(StandardCharsets.ISO_8859_1)));
            }
        }
        InputStream propertyStream = getConfigFileStream(filename);
        if (propertyStream != null) {
            empty = false;
            cList.add(propertyStream);
            cList.add(new ByteArrayInputStream(LINE_BREAK));
        }
        File localProperties = MCRConfigurationDir.getConfigFile(filename);
        if (localProperties != null && localProperties.canRead()) {
            empty = false;
            LogManager.getLogger().info("Loading additional properties from {}", localProperties::getAbsolutePath);
            cList.add(Files.newInputStream(localProperties.toPath()));
            cList.add(new ByteArrayInputStream(LINE_BREAK));
        }
        return Collections.enumeration(cList);
    }

    private static ByteArrayInputStream getBaseDirInputStream(File configurationDirectory) throws IOException {
        Properties dataProp = new Properties();
        //On Windows we require forward slashes
        dataProp.setProperty("MCR.basedir", configurationDirectory.getAbsolutePath().replace('\\', '/'));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        dataProp.store(out, null);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static InputStream getConfigFileStream(String filename) throws IOException {
        File cfgFile = new File(filename);
        MCRContent input = null;
        if (cfgFile.canRead()) {
            input = new MCRFileContent(cfgFile);
        } else {
            URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
            if (url != null) {
                input = new MCRURLContent(url);
            }
        }
        return input == null ? null : input.getInputStream();
    }

    /**
     * Returns the contents of the given config file from all available sources.
     *
     * @param filename
     *            the name of the config file
     * @return a map with the component name as key and the file content as value
     *  found in MyCoRe components and modules, respecting the proper loading order
     * @throws IOException
     *             if an I/O error occurs
     */
    public static SequencedMap<String, byte[]> getConfigFileContents(String filename) throws IOException {
        SequencedMap<String, byte[]> map = new LinkedHashMap<>();
        for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
            try (InputStream is = component.getConfigFileStream(filename)) {
                if (is != null) {
                    map.put(component.getName(), is.readAllBytes());
                }
            }
        }
        // load config file from classpath
        try (InputStream configStream = getConfigFileStream(filename)) {
            if (configStream != null) {
                LogManager.getLogger().debug("Loaded config file from classpath: {}", filename);
                map.put("classpath_" + filename, configStream.readAllBytes());
            }
        }

        //load config file from app config dir
        File localConfigFile = MCRConfigurationDir.getConfigFile(filename);
        if (localConfigFile != null && localConfigFile.canRead()) {
            LogManager.getLogger().debug("Loaded config file from config dir: {}", filename);
            try (InputStream fileInputStream = Files.newInputStream(localConfigFile.toPath())) {
                map.put("configdir_" + filename, fileInputStream.readAllBytes());
            }
        }
        return map;
    }

    /**
     * Continues reading in the next stream if an EOF is reached.
     */
    final void nextStream() throws IOException {
        if (in != null) {
            in.close();
        }

        if (e.hasMoreElements()) {
            in = e.nextElement();
            Objects.requireNonNull(in);

        } else {
            in = null;
        }

    }

    @Override
    public int available() throws IOException {
        if (in == null) {
            return 0; // no way to signal EOF from available()
        }
        return in.available();
    }

    @Override
    public int read() throws IOException {
        if (in == null) {
            return -1;
        }
        int c = in.read();
        if (c == -1) {
            nextStream();
            return read();
        }
        return c;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (in == null) {
            return -1;
        }

        int n = in.read(b, off, len);
        if (n <= 0) {
            nextStream();
            return read(b, off, len);
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        do {
            nextStream();
        } while (in != null);
    }
}
