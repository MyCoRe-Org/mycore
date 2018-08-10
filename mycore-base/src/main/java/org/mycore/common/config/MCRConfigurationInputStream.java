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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
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
 * @since 2013.12
 */
public class MCRConfigurationInputStream extends InputStream {

    private static final String MYCORE_PROPERTIES = "mycore.properties";

    // latin1 for properties
    private static final byte[] lbr = System.getProperty("line.separator").getBytes(StandardCharsets.ISO_8859_1);

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
     * {@link InputStream} that includes all properties from {@link MCRRuntimeComponentDetector#getAllComponents()} and
     * <strong>mycore.properties</strong>. Use system property <code>MCR.Configuration.File</code> to configure
     * alternative property file.
     * 
     * @since 2014.04
     */
    public static MCRConfigurationInputStream getMyCoRePropertiesInstance() throws IOException {
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        InputStream initStream = null;
        if (configurationDirectory != null) {
            LogManager.getLogger().info("Current configuration directory: {}",
                    configurationDirectory.getAbsolutePath());
            // set MCR.basedir, is normally overwritten later
            if (configurationDirectory.isDirectory()) {
                initStream = getBaseDirInputStream(configurationDirectory);
            }
        }
        return new MCRConfigurationInputStream(MYCORE_PROPERTIES, initStream);
    }

    public boolean isEmpty() {
        return empty;
    }

    private Enumeration<? extends InputStream> getPropertyInputStreams(String filename, InputStream initStream)
        throws IOException {
        LinkedList<InputStream> cList = new LinkedList<>();
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
                cList.add(new ByteArrayInputStream(lbr));
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
            cList.add(new ByteArrayInputStream(lbr));
        }
        File localProperties = MCRConfigurationDir.getConfigFile(filename);
        if (localProperties != null && localProperties.canRead()) {
            empty = false;
            LogManager.getLogger().info("Loading additional properties from {}", localProperties.getAbsolutePath());
            cList.add(new FileInputStream(localProperties));
            cList.add(new ByteArrayInputStream(lbr));
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
            URL url = MCRConfigurationInputStream.class.getClassLoader().getResource(filename);
            if (url != null) {
                input = new MCRURLContent(url);
            }
        }
        return input == null ? null : input.getInputStream();
    }

    /**
     * return an enumeration of input streams of configuration files
     * found in MyCoRe components and modules, respecting the proper loading order 
     * 
     * @author Robert Stephan
     */
    public static List<byte[]> getConfigFileContents(String filename) throws IOException {
        ArrayList<byte[]> cList = new ArrayList<>();
        for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
            try (InputStream is = component.getConfigFileStream(filename)) {
                if (is != null) {
                    cList.add(IOUtils.toByteArray(is));
                }
            }
        }
        // load config file from classpath
        try (InputStream configStream = getConfigFileStream(filename)) {
            if (configStream != null) {
                LogManager.getLogger().debug("Loaded config file from classpath: " + filename);
                cList.add(IOUtils.toByteArray(configStream));
            }
        }
        
        //load config file from app config dir
        File localConfigFile = MCRConfigurationDir.getConfigFile(filename);
        if (localConfigFile != null && localConfigFile.canRead()) {
            LogManager.getLogger().debug("Loaded config file from config dir: " + filename);
            try (FileInputStream fis = new FileInputStream(localConfigFile)) {
                cList.add(IOUtils.toByteArray(fis));
            }
        }
        return cList;
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
            if (in == null) {
                throw new NullPointerException();
            }
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
    public int read(byte b[], int off, int len) throws IOException {
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
