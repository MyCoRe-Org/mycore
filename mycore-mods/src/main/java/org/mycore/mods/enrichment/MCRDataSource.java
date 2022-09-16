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

package org.mycore.mods.enrichment;

import java.util.ArrayList;
import java.util.List;

/**
 * A data source is able to return publication data in MODS format
 * for a given publication identifier. A data source may support more than
 * one identifier, e.g. DOI and PubMed ID for PubMed as a data source:
 *
 * MCR.MODS.EnrichmentResolver.DataSource.PubMed.IdentifierTypes=doi pubmed
 *
 * For each supported identifier type, the data source has a resolver that
 * returns MODS data for that identifier type.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRDataSource {

    private String sourceID;

    private boolean stopOnFirstResult = true;

    private List<MCRIdentifierResolver> resolvers = new ArrayList<>();

    MCRDataSource(String sourceID, boolean stopOnFirstResult) {
        this.sourceID = sourceID;
        this.stopOnFirstResult = stopOnFirstResult;
    }

    boolean shouldStopOnFirstResult() {
        return stopOnFirstResult;
    }

    void addResolver(MCRIdentifierResolver resolver) {
        resolvers.add(resolver);
    }

    /** Returns all resolvers to get publication data for a given identifier */
    List<MCRIdentifierResolver> getResolvers() {
        return resolvers;
    }

    String getID() {
        return sourceID;
    }

    public String toString() {
        return "data source " + sourceID;
    }
}
