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

package org.mycore.mods.enrichment;

import org.mycore.common.MCRCache;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Builds and caches all data sources as configured in mycore.properties.
 * <p>
 * For each data source, the types of identifiers must be configured:
 * MCR.MODS.EnrichmentResolver.DataSource.[ID].IdentifierTypes=[TYPE] [TYPE] [TYPE]
 * e.g.
 * MCR.MODS.EnrichmentResolver.DataSource.PubMed.IdentifierTypes=doi pubmed
 * <p>
 * Per data source, for each identifier type, there must be a pattern configured
 * that defines the URI to get the data for this type of identifier
 *
 * @see MCRIdentifierResolver
 * 
 * As a global parameter, or per data source, it can be configured whether 
 * the data source will stop after the first successful call, or retrieve all data
 * for all identifiers.
 * 
 * @see MCRDataSource
 *
 * @author Frank Lützenkirchen
 */
final class MCRDataSourceFactory {

    private static final String CONFIG_PREFIX = "MCR.MODS.EnrichmentResolver.";

    private final MCRCache<String, MCRDataSource> dataSources = new MCRCache<>(30, "data sources");

    private final Boolean defaultStopOnFirstResult;

    private MCRDataSourceFactory() {
        String cfgProperty = CONFIG_PREFIX + "DefaultStopOnFirstResult";
        defaultStopOnFirstResult = MCRConfiguration2.getBoolean(cfgProperty).orElse(Boolean.TRUE);
    }

    /**
     * @deprecated Use {@link #getInstance()} instead
     */
    @Deprecated
    static MCRDataSourceFactory instance() {
        return getInstance();
    }

    static MCRDataSourceFactory getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    private MCRDataSource buildDataSource(String sourceID) {
        String configPrefix = CONFIG_PREFIX + "DataSource." + sourceID + ".";
        String modeProperty = configPrefix + "StopOnFirstResult";
        boolean stopOnFirstResult = MCRConfiguration2.getBoolean(modeProperty).orElse(defaultStopOnFirstResult);

        MCRDataSource dataSource = new MCRDataSource(sourceID, stopOnFirstResult);

        String typesProperty = configPrefix + "IdentifierTypes";
        String[] identifierTypes = MCRConfiguration2.getStringOrThrow(typesProperty).split("\\s");
        for (String typeID : identifierTypes) {
            String property = configPrefix + typeID + ".URI";
            String uri = MCRConfiguration2.getStringOrThrow(property);

            MCRIdentifierType idType = MCRIdentifierTypeFactory.getInstance().getType(typeID);
            MCRIdentifierResolver resolver = new MCRIdentifierResolver(dataSource, idType, uri);
            dataSource.addResolver(resolver);
        }
        return dataSource;
    }

    MCRDataSource getDataSource(String sourceID) {
        MCRDataSource dataSource = dataSources.get(sourceID);
        if (dataSource == null) {
            dataSource = buildDataSource(sourceID);
            dataSources.put(sourceID, dataSource);
        }
        return dataSource;
    }

    private static final class LazyInstanceHolder {
        public static final MCRDataSourceFactory SINGLETON_INSTANCE = new MCRDataSourceFactory();
    }

}
