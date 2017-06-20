/*
 * $Revision$ $Date$
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

package org.mycore.mods.enrichment;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Frank L\u00FCtzenkirchen
 */
class MCRDataSourceFactory {

    private static MCRDataSourceFactory INSTANCE = new MCRDataSourceFactory();

    public static MCRDataSourceFactory instance() {
        return INSTANCE;
    }

    private MCRCache<String, MCRDataSource> dataSources = new MCRCache<String, MCRDataSource>(30, "data sources");

    private MCRDataSourceFactory() {
    }

    private MCRDataSource buildDataSource(String sourceID) {
        MCRConfiguration config = MCRConfiguration.instance();
        MCRDataSource dataSource = new MCRDataSource(sourceID);

        String[] identifierTypes = config
            .getString("MCR.MODS.EnrichmentResolver.DataSource." + sourceID + ".IdentifierTypes").split("\\s");
        for (String typeID : identifierTypes) {
            String prefix = "MCR.MODS.EnrichmentResolver.DataSource." + sourceID + "." + typeID + ".";
            String uri = config.getString(prefix + "URI");

            MCRIdentifierType idType = MCRIdentifierTypeFactory.instance().getType(typeID);
            MCRIdentifierResolver resolver = new MCRIdentifierResolver(idType, uri);
            dataSource.addResolver(resolver);
        }
        return dataSource;
    }

    public MCRDataSource getDataSource(String sourceID) {
        MCRDataSource dataSource = dataSources.get(sourceID);
        if (dataSource == null) {
            dataSource = buildDataSource(sourceID);
            dataSources.put(sourceID, dataSource);
        }
        return dataSource;
    }
}
