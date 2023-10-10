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

package org.mycore.mcr.neo4j.datamodel.metadata.neo4jutil;

/**
 * Utility Class providing constants to Neo4J Implementation
 * @author Andreas Kluge (ai112vezo)
 */
public class MCRNeo4JConstants {

    public static final String NEO4J_CONFIG_PREFIX = "MCR.Neo4J.";

    public static final String DEFAULT_NEO4J_SERVER_URL = NEO4J_CONFIG_PREFIX + "ServerURL";

    public static final String NEO4J_PARAMETER_SEPARATOR = "_-_";

    public static final String NEO4J_CLASSID_CATEGID_SEPARATOR = "__";

    private MCRNeo4JConstants() {
        throw new IllegalStateException("Utility class");
    }

}
