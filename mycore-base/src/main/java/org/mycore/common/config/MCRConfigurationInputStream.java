/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 6, 2013 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.io.input.NullInputStream;
import org.apache.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRURLContent;

/**
 * {@link InputStream} that includes all properties from {@link MCRRuntimeComponentDetector#getAllComponents()} and <strong>mycore.properties</strong>.
 * 
 * Use system property <code>MCR.Configuration.File</code> to configure alternative property file.
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 */
public class MCRConfigurationInputStream extends SequenceInputStream {

    private static final String MYCORE_PROPERTIES = "mycore.properties";

    private static final byte[] lbr = "\n".getBytes();

    public MCRConfigurationInputStream() throws IOException {
        super(getInputStreams());
    }

    private static Enumeration<? extends InputStream> getInputStreams() throws IOException {
        LinkedList<InputStream> cList = new LinkedList<>();
        File configurationDirectory = MCRConfigurationDir.getConfigurationDirectory();
        if (configurationDirectory != null) {
            logInfo("Current configuration directory: " + configurationDirectory.getAbsolutePath());
            //set MCR.basedir, is normally overwritten later
            if (configurationDirectory.isDirectory()) {
                cList.add(getBaseDirInputStream(configurationDirectory));
            }
        }
        for (MCRComponent component : MCRRuntimeComponentDetector.getAllComponents()) {
            InputStream is = component.getPropertyStream();
            if (is != null) {
                String comment = "\n\n#\n#\n# Component: " + component.getName() + "\n#\n#\n";
                cList.add(new ByteArrayInputStream(comment.getBytes()));
                cList.add(is);
                //workaround if last property is not terminated with line break
                cList.add(new ByteArrayInputStream(lbr));
            }
        }
        InputStream propertyStream = getPropertyStream();
        if (propertyStream != null) {
            cList.add(propertyStream);
            cList.add(new ByteArrayInputStream(lbr));
        }
        File localProperties = MCRConfigurationDir.getConfigFile(MYCORE_PROPERTIES);
        if (localProperties != null && localProperties.canRead()) {
            logInfo("Loading additional properties from " + localProperties.getAbsolutePath());
            cList.add(new FileInputStream(localProperties));
            cList.add(new ByteArrayInputStream(lbr));
        }
        if (cList.isEmpty()) {
            cList.add(new NullInputStream(0));
        }
        return Collections.enumeration(cList);
    }

    private static ByteArrayInputStream getBaseDirInputStream(File configurationDirectory) throws IOException {
        Properties dataProp = new Properties();
        dataProp.setProperty("MCR.basedir", configurationDirectory.getAbsolutePath());
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        dataProp.store(out, null);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        return inputStream;
    }

    private static InputStream getPropertyStream() throws IOException {
        String filename = System.getProperty("MCR.Configuration.File", MYCORE_PROPERTIES);
        File mycoreProperties = new File(filename);
        MCRContent input = null;
        if (mycoreProperties.canRead()) {
            input = new MCRFileContent(mycoreProperties);
        } else {
            URL url = MCRConfigurationInputStream.class.getClassLoader().getResource(filename);
            if (url == null) {
                logWarn("Could not load: " + filename);
            } else {
                input = new MCRURLContent(url);
            }
        }
        return input == null ? null : input.getInputStream();
    }

    private static void logWarn(String msg) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRConfigurationInputStream.class).warn(msg);
        } else {
            System.err.printf("WARN: %s\n", msg);
        }
    }

    private static void logInfo(String msg) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRConfigurationInputStream.class).info(msg);
        } else {
            System.out.printf("INFO: %s\n", msg);
        }
    }

}
