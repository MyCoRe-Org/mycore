/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.common;

import org.mycore.common.config.MCRConfiguration2;

import java.util.List;

/**
 * Allows to query objects using {@link MCRObjectQuery}.
 */
public interface MCRObjectQueryResolver {

    List<String> getIds(MCRObjectQuery objectQuery);

    List<MCRObjectIDDate> getIdDates(MCRObjectQuery objectQuery);

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
