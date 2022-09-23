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

package org.mycore.datamodel.objectinfo;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.util.List;

/**
 * Allows to query objects using {@link MCRObjectQuery}.
 */
public interface MCRObjectQueryResolver {

    /**
     * Gets all object info which match the restrictions of the query
     * @param objectQuery the query
     * @return the ids of the objects
     */
    List<MCRObjectID> getIds(MCRObjectQuery objectQuery);

    /**
     * Gets all the object info which match the restrictions of the query
     * @param objectQuery the query
     * @return the ids and dates of the object info
     */
    List<MCRObjectIDDate> getIdDates(MCRObjectQuery objectQuery);

    /**
     * Gets all the object info which match the restrictions of the query
     * @param objectQuery the query
     * @return the info
     */
    List<MCRObjectInfo> getInfos(MCRObjectQuery objectQuery);

    /**
     * Gets the count of object info which match the restrictions of query ignoring limit and offset
     * @param objectQuery the query
     * @return the count of the object info
     */
    int count(MCRObjectQuery objectQuery);

    static MCRObjectQueryResolver getInstance() {
        return InstanceHolder.RESOLVER;
    }

    class InstanceHolder {
        private static final String QUERY_RESOLVER_CLASS_PROPERTY = "MCR.Object.QueryResolver.Class";

        private static final MCRObjectQueryResolver RESOLVER =
                MCRConfiguration2.<MCRObjectQueryResolver>getSingleInstanceOf(QUERY_RESOLVER_CLASS_PROPERTY)
                .orElseThrow(() -> MCRConfiguration2.createConfigurationException(QUERY_RESOLVER_CLASS_PROPERTY));
    }
}
