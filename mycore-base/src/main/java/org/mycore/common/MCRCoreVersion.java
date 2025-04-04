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

package org.mycore.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.mycore.common.config.MCRConfigurationDir;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRCoreVersion {

    private static final Properties PROPERTIES = loadVersionProperties();

    public static final String VERSION = PROPERTIES.getProperty("git.build.version");

    public static final String BRANCH = PROPERTIES.getProperty("git.branch");

    public static final String REVISION = PROPERTIES.getProperty("git.commit.id.full");

    public static final String DESCRIBE = PROPERTIES.getProperty("git.commit.id.describe");

    public static final String ABBREV = PROPERTIES.getProperty("git.commit.id.abbrev");

    public static final String COMPLETE = VERSION + " " + BRANCH + ":" + DESCRIBE;

    public static String getVersion() {
        return VERSION;
    }

    @SuppressWarnings("PMD.MCR.ResourceResolver")
    private static Properties loadVersionProperties() {
        Properties props = new Properties();
        URL gitPropURL = MCRCoreVersion.class.getResource("/org/mycore/git.properties");
        try (InputStream gitPropStream = getInputStream(gitPropURL)) {
            props.load(gitPropStream);
        } catch (IOException e) {
            throw new MCRException("Error while initializing MCRCoreVersion.", e);
        }
        return props;
    }

    private static InputStream getInputStream(URL gitPropURL) throws IOException {
        if (gitPropURL == null) {
            return new InputStream() {
                @Override
                public int read() {
                    return -1;
                }
            };
        }
        return gitPropURL.openStream();
    }

    public static String getBranch() {
        return BRANCH;
    }

    public static String getRevision() {
        return REVISION;
    }

    public static String getGitDescribe() {
        return DESCRIBE;
    }

    public static String getCompleteVersion() {
        return COMPLETE;
    }

    public static String getAbbrev() {
        return ABBREV;
    }

    public static Map<String, String> getVersionProperties() {
        return PROPERTIES.entrySet()
            .stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
    }

    @SuppressWarnings("PMD.SystemPrintln")
    public static void main(String[] arg) throws IOException {
        System.out.printf(Locale.ROOT, "MyCoRe\tver: %s\tbranch: %s\tcommit: %s%n", VERSION, BRANCH, DESCRIBE);
        System.out.printf(Locale.ROOT, "Config directory: %s%n", MCRConfigurationDir.getConfigurationDirectory());
        PROPERTIES.store(System.out, "Values of '/org/mycore/version.properties' resource");
    }

}
