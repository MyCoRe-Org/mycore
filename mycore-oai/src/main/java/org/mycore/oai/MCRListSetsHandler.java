/*
 * $Revision$ 
 * $Date$
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
package org.mycore.oai;
import static org.mycore.oai.MCROAIConstants.ARG_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ERROR_BAD_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ERROR_NO_SET_HIERARCHY;
import static org.mycore.oai.MCROAIConstants.NS_OAI;
import static org.mycore.oai.MCROAIConstants.V_EXCLUSIVE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.oai.classmapping.MCRClassificationAndSetMapper;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;

/**
 * Implements the ListSets request. 
 * For a data provider instance, set support is optional and must be configured as described below.
 * Typically, sets are mapped to categories of a classification in MyCoRe.
 * 
 * The set specifications are read from one or more URIs using MCRURIResolver.
 * This allows for sets that are typically built by applying an xsl stylesheet 
 * to the output of the classification URI resolver, but also for other ways to 
 * dynamically create set specifications, or for a static set specification 
 * that is read from an xml file. 
 * 
 * Example: 
 * 
 * MCR.OAIDataProvider.OAI.Sets.OA=webapp:oai/open_access.xml
 * MCR.OAIDataProvider.OAI.Sets.DDC=xslStyle:classification2sets:classification:DDC
 * 
 * The first line reads a set specification from a static xml file stored in the 
 * web application. The DINI certificate demands that there always is a set 
 * open_access that contains all public Open Access documents. Since this set always exists,
 * its set specification can be read from a static file.
 * 
 * The second line uses the classification resolver to read in a classification, then
 * transforms the xml to build set specifications from the listed categories.
 * 
 * It is recommended not to list sets that are completely empty, to simplify harvesting.
 * The fastest way to accomplish this is to somehow ensure that no set specifications
 * from empty sets are delivered from the URIs, which means that the classification resolver
 * filters out empty categories, or the xsl stylesheet somehow decides to filter empty sets.
 * 
 * Another way to filter out empty sets can be activated by setting a property:
 * 
 * MCR.OAIDataProvider.OAI.FilterEmptySets=true

 * When set to true, the ListSets handler filters out empty sets itself after reading in the URIs.
 * This is done by constructing a query for each set and looking for matching hits. Set queries are built
 * using the OAI Adapter's buildSetCondition method. Filtering empty sets this way may be useful for some
 * implementations, but it is slower and should be avoided for large set hierarchies.   
 * 
 * @see MCRURIResolver
 * @see MCROAIAdapter#buildSetCondition(String)
 * 
 * @author Frank L\u00fctzenkirchen
 */
class MCRListSetsHandler extends MCRVerbHandler {
    final static String VERB = "ListSets";

    void setAllowedParameters(Properties p) {
        p.setProperty(ARG_RESUMPTION_TOKEN, V_EXCLUSIVE);
    }

    MCRListSetsHandler(MCROAIDataProvider provider) {
        super(provider);
    }

    @SuppressWarnings("unchecked")
    void handleRequest() {
        String resumptionToken = parms.getProperty(ARG_RESUMPTION_TOKEN);
        if (resumptionToken != null) {
            addError(ERROR_BAD_RESUMPTION_TOKEN, "Bad resumption token: " + resumptionToken);
            return;
        }

        if (setURIs.isEmpty()) {
            addError(ERROR_NO_SET_HIERARCHY, "This repository does not provide sets");
            return;
        }

        Set<String> setSpecs = new HashSet<String>();
        List<Element> sets = new ArrayList<Element>();
        for (String uri : setURIs) {
            Element resolved = MCRURIResolver.instance().resolve(uri);
            for (Element set : (List<Element>) (resolved.getChildren("set", NS_OAI))) {
                String setSpec = set.getChildText("setSpec", NS_OAI);
                if (!setSpecs.contains(setSpec)){
                    Element setClone = (Element) set.clone();
                    if (setSpec.contains(":")) {
                        String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
                        classID = MCRClassificationAndSetMapper.mapClassificationToSet(provider.getAdapter().prefix, classID);
                        setClone.getChild("setSpec", NS_OAI).setText(classID+setSpec.substring(setSpec.indexOf(':')));  
                    }
                    sets.add((Element) (setClone));
                }
            }
        }

        // Filter out empty sets
        if (MCRConfiguration.instance().getBoolean(provider.getPrefix() + "FilterEmptySets", true)) {
            setSpecs.clear();
            for (Iterator<Element> it = sets.iterator(); it.hasNext();) {
                Element set = it.next();
                String setSpec = set.getChildText("setSpec", NS_OAI);

                // Check parent set, if existing
                if (setSpec.contains(":") && (setSpec.lastIndexOf(":") > setSpec.indexOf(":"))) {
                    String parentSetSpec = setSpec.substring(0, setSpec.lastIndexOf(":"));
                    // If parent set is empty, all child sets must be empty, too
                    if (!setSpecs.contains(parentSetSpec)) {
                        it.remove();
                        continue;
                    }
                }

                // Build a query to count results
                MCRAndCondition query = new MCRAndCondition();
                query.addChild(provider.getAdapter().buildSetCondition(setSpec));
                if (restriction != null)
                    query.addChild(restriction);

                if (MCRQueryManager.search(new MCRQuery(query)).getNumHits() == 0)
                    it.remove();
                else
                    setSpecs.add(setSpec);
            }
        }
        output.addContent(sets);
    }
}
