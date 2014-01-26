/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 3, 2013 $
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

import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;

/**
 * This class abstracts different MyCoRe component types.
 * As every component (mycore component, application module) holds it configuration in different places, you can use this class to
 * get uniform access to these configuration resources. 
 * 
 * As this class is immutable it could be used as key in a {@link Map}
 * @author Thomas Scheffler (yagee)
 * @since 2013.12
 * @see MCRRuntimeComponentDetector
 */
public class MCRComponent implements Comparable<MCRComponent> {

    private static final ResourceBundle.Control CONTROL_HELPER = new ResourceBundle.Control() {
    };

    private enum Type {
        base, complete, component, module
    }

    private Type type;

    private String name;

    private File jarFile;

    private String sortCriteria;

    private String artifactId;

    public MCRComponent(String artifactId) {
        this(artifactId, null);
    }

    public MCRComponent(String artifactId, File jarFile) {
        if (artifactId.endsWith("complete")) {
            type = Type.complete;
            setName(artifactId.replaceAll("-?[^-]*complete", ""));
        } else if (artifactId.startsWith("mycore-")) {
            if (artifactId.endsWith("base")) {
                type = Type.base;
                setName("base");
            } else {
                type = Type.component;
                setName(artifactId.substring("mycore-".length()));
            }
        } else {
            type = Type.module;
            setName(artifactId.replaceAll("-?module", ""));
        }
        this.jarFile = jarFile;
        switch (type) {
            case complete:
                this.sortCriteria = 0 + getName();
                break;
            case base:
                this.sortCriteria = 1 + getName();
                break;
            case component:
                this.sortCriteria = 2 + getName();
                break;
            case module:
                this.sortCriteria = 3 + getName();
                break;
            default:
                throw new MCRException("Do not support MCRComponenty of type: " + type);
        }
        this.artifactId = artifactId;
        logdebug(artifactId + " is of type " + type + " and named " + getName() + ": " + jarFile);
    }

    private static void logdebug(String msg) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRComponent.class).debug(msg);
        }
    }

    private static void loginfo(String msg) {
        if (MCRConfiguration.isLog4JEnabled()) {
            Logger.getLogger(MCRComponent.class).info(msg);
        } else {
            System.out.printf("INFO: %s\n", msg);
        }
    }

    /**
     * returns InputStream of mycore.properties of this component.
     * @return null if component does not contain mycore.properties
     */
    public InputStream getPropertyStream() {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        return this.getClass().getClassLoader().getResourceAsStream(resourceBase + "mycore.properties");
    }

    /**
     * returns InputStream of messages*.properties of this component for the specified locale.
     * @return null if component does not contain specified resource bundle
     */
    public InputStream getMessagesInputStream(Locale locale) {
        String resourceBase = getResourceBase();
        if (resourceBase == null) {
            return null;
        }
        String bundleName = resourceBase + CONTROL_HELPER.toBundleName("messages", locale) + ".properties";
        loginfo("Loading bundle: " + bundleName);
        return this.getClass().getClassLoader().getResourceAsStream(bundleName);
    }

    /**
     * Returns resource base path to this components config resources.
     */
    public String getResourceBase() {
        switch (type) {
            case base:
                return "config/";
            case component:
                return "components/" + getName() + "/config/";
            case module:
                return "config/" + getName() + "/";
            default:
                logdebug(getName() + ": there is no resource base for type " + type);
                break;
        }
        return null;
    }

    /**
     * Returns true, if this component is part of MyCoRe
     */
    public boolean isMyCoReComponent() {
        return type == Type.base || type == Type.component;
    }

    /**
     * Returns true, if this component is application module 
     */
    public boolean isAppModule() {
        return type == Type.module;
    }

    /**
     * Returns true, if this component is a mycore complete package and thus does not support auto-resolving of config resources.
     */
    public boolean isCompletePackage() {
        return type == Type.complete;
    }

    /**
     * A short name for this component.
     * E.g. mycore-base would return "base" here.
     */
    public String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
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
        if (!(obj instanceof MCRComponent)) {
            return false;
        }
        MCRComponent other = (MCRComponent) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case base:
            case component:
                sb.append("mcr:");
                break;
            case module:
                sb.append("app:");
                break;
            default:
                //complete
                break;
        }
        sb.append(artifactId);
        return sb.toString();
    }
}
