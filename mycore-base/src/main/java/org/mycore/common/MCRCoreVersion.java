/*
 * $RCSfile$
 * $Revision: 1 $ $Date: 17.07.2009 $
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

package org.mycore.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfigurationDir;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCoreVersion {
    private static Properties prop = loadVersionProperties();

    public static final String VERSION = prop.getProperty("mycore.version");

    public static final int REVISION = getRevisionFromProperty();

    public static final String COMPLETE = VERSION + " r" + REVISION;

    public static String getVersion() {
        return VERSION;
    }

    private static Properties loadVersionProperties() {
        Properties props = new Properties();
        URL propURL = MCRCoreVersion.class.getResource("/org/mycore/version.properties");
        try {
            InputStream propStream = propURL.openStream();
            try {
                props.load(propStream);
            } finally {
                propStream.close();
            }
        } catch (IOException e) {
            throw new MCRException("Error while initializing MCRCoreVersion.", e);
        }
        return props;
    }

    public static int getRevision() {
        return REVISION;
    }

    public static String getCompleteVersion() {
        return COMPLETE;
    }

    public static void main(String arg[]) {
        System.out.printf(Locale.ROOT, "MyCoRe\tver: %s\trev: %d\n", VERSION, REVISION);
        System.out.printf(Locale.ROOT, "Config directory: %s\n", MCRConfigurationDir.getConfigurationDirectory());
    }

    private static int getRevisionFromProperty() {
        try {
            return Integer.parseInt(prop.getProperty("revision.number"));
        } catch (NumberFormatException e) {
            Logger.getLogger(MCRCoreVersion.class).error(
                "Error parsing revisionnumber: " + prop.getProperty("revision.number"));
            return -1;
        }
    }
}
