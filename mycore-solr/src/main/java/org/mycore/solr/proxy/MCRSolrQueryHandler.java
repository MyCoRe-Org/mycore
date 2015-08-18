/*
 * $Id$
 * $Revision: 5697 $ $Date: May 24, 2013 $
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

package org.mycore.solr.proxy;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.solr.common.util.NamedList;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.solr.MCRSolrConstants;

/**
 * Holds information about the remote SOLR request handler. 
 * @author Thomas Scheffler (yagee)
 */
class MCRSolrQueryHandler {
    private static Pattern restrictedClassPattern = getRestrictedClassPattern();

    String clazz;

    String description;

    String path;

    boolean restricted;

    String version;

    public MCRSolrQueryHandler(String path, NamedList<Object> queryHandler) {
        if (path == null) {
            throw new NullPointerException(this.getClass().getSimpleName() + " requires path parameter.");
        }
        if (path.length() == 0 || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with '/': " + path);
        }
        this.path = path;
        if (queryHandler == null) {
            //probably no connection to SOLR server
            restricted = false;
            return;
        }
        for (Map.Entry<String, Object> entry : queryHandler) {
            switch (entry.getKey()) {
                case "class":
                    this.clazz = (String) entry.getValue();
                    break;
                case "version":
                    this.version = (String) entry.getValue();
                    break;
                case "description":
                    this.description = (String) entry.getValue();
                    break;
                default:
                    break;
            }
        }
        restricted = clazz == null || clazz.length() == 0 || restrictedClassPattern.matcher(clazz).find();
    }

    private static Pattern getRestrictedClassPattern() {
        final String agentRegEx = MCRConfiguration.instance().getString(
            MCRSolrConstants.CONFIG_PREFIX + "Proxy.ClassFilter");
        return Pattern.compile(agentRegEx);
    }

    public boolean isRestricted() {
        return restricted;
    }

    public String getClazz() {
        return clazz;
    }

    public String getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "MCRSolrQueryHandler [path=" + path + ", " + "restricted=" + restricted + ", "
            + (version != null ? "version=" + version + ", " : "")
            + (description != null ? "description=" + description + ", " : "")
            + (clazz != null ? "class=" + clazz : "") + "]";
    }

    public static Comparator<MCRSolrQueryHandler> getPathComparator() {
        return (o1, o2) -> o1.getPath().compareTo(o2.getPath());
    }

}
