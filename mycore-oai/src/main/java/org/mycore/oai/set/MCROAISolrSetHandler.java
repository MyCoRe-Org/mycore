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

package org.mycore.oai.set;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;

public abstract class MCROAISolrSetHandler implements MCROAISetHandler<SolrQuery, SolrDocument, String> {

    private String configPrefix;

    private String handlerPrefix;

    private HashMap<String, MCRSet> setMap;

    @Override
    public void init(String configPrefix, String handlerPrefix) {
        this.configPrefix = configPrefix;
        this.handlerPrefix = handlerPrefix;
        this.setMap = new HashMap<>();
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public String getHandlerPrefix() {
        return handlerPrefix;
    }

    public Collection<String> getFieldNames() {
        return Collections.emptySet();
    }

    public MCROAISetResolver<String, SolrDocument> getSetResolver(Collection<SolrDocument> result) {
        MCROAISetResolver<String, SolrDocument> resolver = new MCROAISetResolver<>();
        resolver.init(configPrefix, handlerPrefix, getSetMap(), result, MCROAISolrSetHandler::getIdentifier);
        return resolver;
    }

    protected static String getIdentifier(SolrDocument doc) {
        return doc.getFieldValue("id").toString();
    }

    @Override
    public Map<String, MCRSet> getSetMap() {
        return setMap;
    }

}
