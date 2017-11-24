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
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.input.TeeInputStream;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.MCRURLContent;

import com.ibm.icu.util.StringTokenizer;

/**
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRDefaultConfigurationLoader implements MCRConfigurationLoader {

    MCRProperties properties;

    public MCRDefaultConfigurationLoader() {
        properties = new MCRProperties();
        try (InputStream in = getConfigInputStream()) {
            loadFromContent(new MCRStreamContent(in));
        } catch (IOException e) {
            throw new MCRConfigurationException("Could not load MyCoRe properties.", e);
        }
    }

    private InputStream getConfigInputStream() throws IOException {
        MCRConfigurationInputStream configurationInputStream = MCRConfigurationInputStream
            .getMyCoRePropertiesInstance();
        File configFile = MCRConfigurationDir.getConfigFile("mycore.active.properties");
        if (configFile != null) {
            FileOutputStream fout = new FileOutputStream(configFile);
            return new TeeInputStream(configurationInputStream, fout, true);
        }
        return configurationInputStream;
    }

    @Override
    public Map<String, String> load() {
        return properties.getAsMap();
    }

    /**
     * Loads configuration properties from a specified properties file and adds
     * them to the properties currently set. This method scans the <CODE>
     * CLASSPATH</CODE> for the properties file, it may be a plain file, but
     * may also be located in a zip or jar file. If the properties file contains
     * a property called <CODE>MCR.Configuration.Include</CODE>, the files
     * specified in that property will also be read. Multiple include files have
     * to be separated by spaces or colons.
     * 
     * @param filename
     *            the properties file to be loaded
     * @throws MCRConfigurationException
     *             if the file can not be loaded
     */
    private void loadFromFile(String filename) {
        File mycoreProperties = new File(filename);
        MCRContent input = null;
        try {
            if (mycoreProperties.canRead()) {
                input = new MCRFileContent(mycoreProperties);
            } else {
                URL url = this.getClass().getResource("/" + filename);
                if (url == null) {
                    throw new MCRConfigurationException("Could not find file or resource:" + filename);
                }
                input = new MCRURLContent(url);
            }
            loadFromContent(input);
        } catch (IOException e) {
            String name = input == null ? filename : input.getSystemId();
            throw new MCRConfigurationException("Could not load configuration from: " + name, e);
        }
    }

    private void loadFromContent(MCRContent input) throws IOException {
        try (InputStream in = input.getInputStream()) {
            properties.load(in);
        }
        String include = properties.getProperty("MCR.Configuration.Include", null);

        if (include != null) {
            StringTokenizer st = new StringTokenizer(include, ", ");
            properties.remove("MCR.Configuration.Include");
            while (st.hasMoreTokens()) {
                loadFromFile(st.nextToken());
            }
        }
    }

}
