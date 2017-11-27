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

package org.mycore.oai;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.oai.pmh.Header;

/**
 * Solr implementation of a MCROAIResult.
 * 
 * @author Matthias Eichner
 */
public class MCROAISolrResult implements MCROAIResult {

    protected QueryResponse response;

    private Function<SolrDocument, Header> toHeader;

    public MCROAISolrResult(QueryResponse response, Function<SolrDocument, Header> toHeader) {
        this.response = response;
        this.toHeader = toHeader;
    }

    @Override
    public int getNumHits() {
        return (int) getResponse().getResults().getNumFound();
    }

    @Override
    public List<Header> list() {
        SolrDocumentList list = getResponse().getResults();
        return list.stream().map(toHeader).collect(Collectors.toList());
    }

    @Override
    public Optional<String> nextCursor() {
        return Optional.ofNullable(this.response.getNextCursorMark());
    }

    public QueryResponse getResponse() {
        return response;
    }

}
