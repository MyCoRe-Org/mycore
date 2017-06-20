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

import java.util.List;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXPathBuilder;
import org.mycore.mods.MCRMODSSorter;
import org.mycore.mods.merger.MCRMerger;
import org.mycore.mods.merger.MCRMergerFactory;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCREnrichmentResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger(MCREnrichmentResolver.class);

    private static final XPathExpression<Element> xPath2RelatedItems = XPathFactory.instance().compile(
        "mods:relatedItem[@type='host' or @type='series']", Filters.element(), null,
        MCRConstants.getStandardNamespaces());

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        href = href.substring(href.indexOf(":") + 1);
        String configID = href.substring(0, href.indexOf(':'));

        href = href.substring(href.indexOf(":") + 1);
        Element mods = MCRURIResolver.instance().resolve(href);

        enrichPublication(mods, configID);

        return new JDOMSource(mods);
    }

    public void enrichPublication(Element mods, String configID) {
        enrichPublicationLevel(mods, configID);
        List<Element> relatedItems = xPath2RelatedItems.evaluate(mods);
        for (Element relatedItem : relatedItems) {
            enrichPublicationLevel(relatedItem, configID);
        }

        debug(mods, "complete publication");
    }

    private void enrichPublicationLevel(Element mods, String configID) {
        LOGGER.debug("resolving via config " + configID + " : " + MCRXPathBuilder.buildXPath(mods));

        boolean withinGroup = false;
        boolean dataSourceCompleted = false;
        String dsConfig = MCRConfiguration.instance().getString("MCR.MODS.EnrichmentResolver.DataSources." + configID);

        for (StringTokenizer st = new StringTokenizer(dsConfig, " ()", true); st.hasMoreTokens();) {
            String token = st.nextToken();
            if (token.equals(" ")) {
                continue;
            } else if (token.equals("(")) {
                withinGroup = true;
                dataSourceCompleted = false;
            } else if (token.equals(")")) {
                withinGroup = false;
                dataSourceCompleted = false;
            } else if (withinGroup && dataSourceCompleted) {
                LOGGER.debug("Skipping data source " + token);
                continue;
            } else {
                String dataSourceID = token;
                MCRDataSource dataSource = MCRDataSourceFactory.instance().getDataSource(dataSourceID);

                dataSourceLoop: for (MCRIdentifierResolver resolver : dataSource.getResolvers()) {
                    MCRIdentifierType idType = resolver.getType();
                    List<Element> identifiersFound = idType.findIdentifiers(mods);
                    for (Element identifierElement : identifiersFound) {
                        String identifier = identifierElement.getTextTrim();

                        LOGGER.debug("resolving " + idType + " " + identifier + " from " + dataSource + "...");
                        Element resolved = resolver.resolve(identifier);

                        if (resolved == null) {
                            LOGGER.debug("no data returned from " + dataSource);
                        } else {
                            mergeResolvedIntoExistingData(mods, resolved);
                            dataSourceCompleted = true;
                            break dataSourceLoop;
                        }
                    }
                }
            }
        }
    }

    private void mergeResolvedIntoExistingData(Element mods, Element resolved) {
        LOGGER.debug("resolved publication data, merging into existing data...");
        debug(resolved, "resolved publication");

        if (mods.getName().equals("relatedItem")) {
            // resolved is always mods:mods, transform to mods:relatedItem to be mergeable
            resolved.setName("relatedItem");
            resolved.setAttribute(mods.getAttribute("type").clone());
        }

        MCRMerger a = MCRMergerFactory.buildFrom(mods);
        MCRMerger b = MCRMergerFactory.buildFrom(resolved);
        a.mergeFrom(b);

        MCRMODSSorter.sort(mods);
        debug(mods, "merged publication");
    }

    private void debug(Element mods, String headline) {
        if (LOGGER.isDebugEnabled()) {
            mods.removeChildren("extension", MCRConstants.MODS_NAMESPACE);
            try {
                LOGGER.debug("\n-------------------- " + headline + ": --------------------\n");
                XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
                LOGGER.debug(xout.outputString(mods));
                LOGGER.debug("\n");
            } catch (Exception ignored) {
            }
        }
    }
}
