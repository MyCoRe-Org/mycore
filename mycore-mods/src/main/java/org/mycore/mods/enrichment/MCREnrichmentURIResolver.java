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
import org.mycore.common.xsl.uriresolver.MCRURIResolver;

/**
 * {@link URIResolver} that resolves a URI returning a MODS element and enriches it using
 * external data sources via a configured {@link MCREnricher}.
 */
public class MCREnrichmentURIResolver implements URIResolver {

    /**
     * Resolves the target URI, enriches the returned MODS element using the specified
     * enrichment configuration, and returns the result as an XML source.
     * <p>To start with just an identifier, combine with the {@code buildxml} resolver to
     * construct a minimal MODS document before enrichment.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{configId}:{anyMCRUri}
     * </pre>
     * <p>Example request:
     * <pre>
     *   enrich:import:mcrobject:mcr_document_00000001
     *   enrich:import:buildxml:_rootName_=mods:mods&amp;mods:identifier=10.123/456&amp;mods:identifier/@type=doi
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
     *     <mods:identifier type="doi">10.123/456</mods:identifier>
     *     <mods:titleInfo>
     *       <mods:title>Enriched Title</mods:title>
     *     </mods:titleInfo>
     *     ...
     *   </mods:mods>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the enriched MODS element
     */
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
