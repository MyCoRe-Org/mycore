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

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Retrieves a publication as MODS XML from a given URI and
 * enriches publication data using external data sources.
 * <p>
 * There may be different configurations for the enrichment process.
 * Syntax:
 * enrich:[ConfigID]:[URIReturningExistingMODS]
 * <p>
 * To start with just an identifier, use the "buildxml" resolver, e.g.
 * enrich:import:buildxml:_rootName_=mods:mods&amp;mods:identifier=10.123/456&amp;mods:identifier/@type=doi
 * This first builds an empty MODS document with just a DOI identifier,
 * then enriches it using the "import" configuration of MCREnricher.
 * <p>
 * For further details,
 * @see MCREnricher
 *
 * @author Frank Lützenkirchen
 */
public class MCREnrichmentResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) {

        String hrefSub = href.substring(href.indexOf(':') + 1);
        String configID = hrefSub.substring(0, hrefSub.indexOf(':'));

        hrefSub = hrefSub.substring(hrefSub.indexOf(':') + 1);
        Element mods = MCRURIResolver.obtainInstance().resolve(hrefSub);

        enrichPublication(mods, configID);
        return new JDOMSource(mods);
    }

    public void enrichPublication(Element mods, String configID) {
        MCREnricher enricher = new MCREnricher(configID);
        enricher.enrich(mods);
    }
}
