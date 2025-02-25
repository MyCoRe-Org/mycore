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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;

/**
 * This class abstracts different MyCoRe component types.
 * As every component (mycore component, application module) holds it configuration in different places,
 * you can use this class to get uniform access to these configuration resources.
 * <p>
 * As this class is immutable it could be used as key in a {@link Map}
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 * @see MCRRuntimeComponentDetector
 */
public class MCRComponent implements Comparable<MCRComponent> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ATT_PRIORITY = "Priority";

    private static final NumberFormat PRIORITY_FORMAT = getPriorityFormat();

    private static final String DEFAULT_PRIORITY = "99";

    private final Type type;

    private final String name;

    private final File jarFile;

    private final int priority;

    private final String sortCriteria;

    private final String artifactId;

    private final Manifest manifest;

    private enum Type {
        BASE, COMPONENT, MODULE
    }

    private static NumberFormat getPriorityFormat() {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        format.setGroupingUsed(false);
        format.setMinimumIntegerDigits(3);
        return format;
    }

    public MCRComponent(String artifactId, Manifest manifest) {
        this(artifactId, manifest, null);
    }

    public MCRComponent(String artifactId, Manifest manifest, File jarFile) {
        if (artifactId.startsWith("mycore-")) {
            if (artifactId.endsWith("base")) {
                this.type = Type.BASE;
                this.name = "base";
            } else {
                this.type = Type.COMPONENT;
                this.name = artifactId.substring("mycore-".length());
            }
        } else {
            this.type = Type.MODULE;
            this.name = artifactId.replaceAll("[_-]?module", "");
        }
        this.jarFile = jarFile;
        this.artifactId = artifactId;
        this.manifest = manifest;
        this.priority = calculatePriority(artifactId, manifest, this.type);
        synchronized (PRIORITY_FORMAT) {
            this.sortCriteria = PRIORITY_FORMAT.format(this.priority) + this.name;
        }
        LOGGER.debug("{} is of type {} and named {}: {}", artifactId, this.type, this.name, jarFile);
    }

    private static int calculatePriority(String artifactId, Manifest manifest, Type type) {
        String priorityAtt = manifest.getMainAttributes().getValue(ATT_PRIORITY);
        if (priorityAtt == null) {
            priorityAtt = DEFAULT_PRIORITY;
            LOGGER.debug("{} has DEFAULT priority {}", artifactId, priorityAtt);
        } else {
            LOGGER.debug("{} has priority {}", artifactId, priorityAtt);
        }
        int priority = Integer.parseInt(priorityAtt);
        if (priority > 99 || priority < 0) {
            throw new MCRException(artifactId + " has unsupported priority: " + priority);
        }
        priority += switch (type) {
            case BASE -> 100;
            case COMPONENT -> 200;
            case MODULE -> 300;
        };
        return priority;
    }

    public InputStream getConfigFileStream(String filename) {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        String resourceName = resourceBase + filename;
        InputStream resourceStream = MCRClassTools.getClassLoader().getResourceAsStream(resourceName);
        if (resourceStream != null) {
            LOGGER.info("Reading config resource: {}", resourceName);
        }
        return resourceStream;
    }

    public URL getConfigURL(String filename) {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        String resourceName = resourceBase + filename;
        URL resourceURL = MCRClassTools.getClassLoader().getResource(resourceName);
        if (resourceURL != null) {
            LOGGER.info("Reading config resource: {}", resourceName);
        }
        return resourceURL;
    }

    /**
     * Returns resource base path to this components config resources.
     */
    public String getResourceBase() {
        return switch (type) {
            case BASE -> "config/";
            case COMPONENT -> "components/" + name + "/config/";
            case MODULE -> "config/" + name + "/";
            default -> {
                LOGGER.debug("{}: there is no resource base for type {}", name, type);
                yield null;
            }
        };
    }

    /**
     * Returns true, if this component is a MyCore base component
     */
    public boolean isMyCoReBaseComponent() {
        return type == Type.BASE;
    }

    /**
     * Returns true, if this component is a MyCoRe component
     */
    public boolean isMyCoReComponent() {
        return type == Type.BASE || type == Type.COMPONENT;
    }

    /**
     * Returns true, if this component is an application module
     */
    public boolean isAppModule() {
        return type == Type.MODULE;
    }

    /**
     * A short name for this component.
     * E.g. mycore-base would return "base" here.
     */
    public String getName() {
        return name;
    }

    /**
     * The unshortened name for this component.
     */
    public String getFullName() {
        return artifactId;
    }

    /**
     * Returns the jar file or <code>null</code> if nothing was set.
     *
     * @return the jar file
     */
    public File getJarFile() {
        return jarFile;
    }

    /**
     * Returns the priority.
     *
     * @return the priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Returns the mainfest main attribute value for given attribute name.
     *
     * @param name the attribute name
     * @return the attribute value
     */
    public String getManifestMainAttribute(String name) {
        return manifest.getMainAttributes().getValue(name);
    }

    /**
     * Compares this component to other component.
     * Basic order is:
     * <ol>
     *  <li>complete</li>
     *  <li>base</li>
     *  <li>component</li>
     *  <li>module</li>
     * </ol>
     * If more than one component is in one of these groups, they are sorted alphabetically via {@link #getName()}.
     */
    @Override
    public int compareTo(MCRComponent o) {
        return this.sortCriteria.compareTo(o.sortCriteria);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MCRComponent other)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return type == other.type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case BASE, COMPONENT -> sb.append("mcr:");
            case MODULE -> sb.append("app:");
            default -> {
            }
        }
        sb.append(artifactId);
        return sb.toString();
    }

}
