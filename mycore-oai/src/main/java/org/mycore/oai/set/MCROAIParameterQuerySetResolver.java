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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.mycore.oai.pmh.Set;

/**
 * If <code>{OAIPrefix}.Sets.{SetID}.Query</code> is in form <code>{searchField}':{setSpec}', uses {searchField}
 * to map from SOLR result document to OAI set.
 * @author Thomas Scheffler (yagee)
 */
class MCROAIParameterQuerySetResolver extends MCROAISetResolver<String, SolrDocument> {

    private String queryField;

    private Map<String, SolrDocument> resultMap;

    public MCROAIParameterQuerySetResolver(String queryField) {
        super();
        this.queryField = queryField;
    }

    @Override
    public void init(String configPrefix, String setId, Map<String, MCRSet> setMap, Collection<SolrDocument> result,
        Function<SolrDocument, String> identifier) {
        super.init(configPrefix, setId, setMap, result, identifier);
        resultMap = getResult().stream().collect(Collectors.toMap(getIdentifier(), d -> d));
    }

    @Override
    public Collection<Set> getSets(String key) {
        return Optional.ofNullable(resultMap.get(key)
            .getFieldValues(queryField))
            .orElseGet(Collections::emptySet)
            .stream()
            .map(getSetMap()::get)
            .collect(Collectors.toSet());
    }

}
