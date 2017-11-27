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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.oai.pmh.Set;

/**
 * Uses  <code>{OAIPrefix}.Sets.{SetID}.SetSolrField</code> (defaults to 'category.top') property to map
 * from SOLR field of the result document to the OAI set
 * @author Thomas Scheffler (yagee)
 * @see MCRClassificationAndSetMapper
 */
class MCROAIClassificationSetResolver extends MCROAISetResolver<String, SolrDocument> {

    Map<String, SolrDocument> setMap;

    private String classField;

    private String classPrefix;

    @Override
    public void init(String configPrefix, String setId, Map<String, MCRSet> setMap, Collection<SolrDocument> result,
        Function<SolrDocument, String> identifier) {
        super.init(configPrefix, setId, setMap, result, identifier);
        this.setMap = result.stream().collect(Collectors.toMap(getIdentifier(), d -> d));
        String classId = MCRClassificationAndSetMapper.mapSetToClassification(configPrefix, setId);
        classField = MCRConfiguration.instance().getString(getConfigPrefix() + "Sets." + setId + "SetSolrField",
            "category.top");
        classPrefix = classId + ":";
    }

    @Override
    public Collection<Set> getSets(String key) {
        SolrDocument solrDocument = setMap.get(key);
        if (solrDocument == null) {
            throw new MCRException("Unknown key: " + key);
        }
        return Optional.ofNullable(solrDocument.getFieldValues(classField))
            .orElseGet(Collections::emptySet)
            .stream()
            .map(String.class::cast)
            .filter(s -> s.startsWith(classPrefix))
            .map(s -> s.substring(classPrefix.length()))
            .map(s -> getSetId() + ":" + s)
            .map(getSetMap()::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    }

}
