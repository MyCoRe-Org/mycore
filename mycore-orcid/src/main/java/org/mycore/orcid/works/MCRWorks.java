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

package org.mycore.orcid.works;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.orcid.MCRORCIDClient;
import org.mycore.orcid.MCRORCIDNamespaces;
import org.mycore.orcid.MCRORCIDProfile;
import org.xml.sax.SAXException;

/**
 * Represents the "works" section of an ORCID profile
 * and contains functionality to fetch works from remote ORCID server.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorks {

    private final static String CFG_PREFIX = "MCR.ORCID.Works.";

    private final static String WORK2MODS_XSL = MCRConfiguration.instance().getString(CFG_PREFIX + "XSL.Works2MODS");

    /** Transformer used to transform the ORCID response with one or more works or work summaries to MODS */
    private final static MCRContentTransformer T_WORKXML2MODS = new MCRXSLTransformer("xsl/" + WORK2MODS_XSL);

    /** The maximum number of works to fetch at once in a bulk request */
    private final static int BULK_FETCH_SIZE = MCRConfiguration.instance().getInt(CFG_PREFIX + "BulkFetchSize");

    /** The ORCID profile these works belongs to */
    private MCRORCIDProfile profile;

    /** The groups of works this ORCID profile contains */
    private List<MCRGroupOfWorks> groups = new ArrayList<>();

    private Map<String, MCRWork> putCode2work = new HashMap<>();

    private List<String> putCodes = new ArrayList<>();

    /** Creates a new works representation for the given ORCID profile */
    public MCRWorks(MCRORCIDProfile profile) {
        this.profile = profile;
    }

    /**
     * Fetches the work summaries from the ORCID profile.
     * This is the first method you must call when you want to fetch all works.
     */
    public void fetchSummaries() throws JDOMException, IOException, SAXException {
        Element worksXML = fetchWorksXML("works");

        for (Element groupXML : worksXML.getChildren("group", MCRORCIDNamespaces.NS_ACTIVITIES)) {
            MCRGroupOfWorks group = new MCRGroupOfWorks();
            for (Element workSummary : groupXML.getChildren("work-summary", MCRORCIDNamespaces.NS_WORK)) {
                MCRWork work = new MCRWork();
                work.setFromWorkXML(workSummary);
                putCode2work.put(work.getPutCode(), work);
                putCodes.add(work.getPutCode());
                group.add(work);
            }
            groups.add(group);
        }
    }

    /**
     * Returns the list of grouped works after fetching work summaries.
     * Multiple works from different sources which are assumed to represent the same publication
     * are grouped together by ORCID.
     */
    public List<MCRGroupOfWorks> getGroups() {
        return groups;
    }

    /**
     * Returns a mods:modsCollection containing all MODS representations of the works.
     * The MODS from multiple works within the same groups is merged together,
     * so for each group of works there will be a single mods within the collection.
     */
    public Element buildMODSCollection() {
        Element modsCollection = new Element("modsCollection", MCRConstants.MODS_NAMESPACE);
        groups.forEach(g -> modsCollection.addContent(g.buildMergedMODS()));
        return modsCollection;
    }

    /**
     * Fetches the work details of all works within the ORCID profile.
     * Note that fetchSummaries() must have been called first.
     * Does a bulk request to fetch multiple works at once, where
     *
     * MCR.ORCID.Works.BulkFetchSize=20
     *
     * determines the maximum number of works to fetch at once.
     */
    public void fetchDetails() throws IOException, JDOMException, SAXException {
        for (int offset = 0; offset < putCodes.size(); offset += BULK_FETCH_SIZE) {
            int chunkEndIndex = Math.min(offset + BULK_FETCH_SIZE, putCodes.size());
            String joinedPutCodes = StringUtils.join(putCodes.subList(offset, chunkEndIndex), ',');
            Element bulk = fetchWorksXML("works/" + joinedPutCodes);

            for (Element workXML : bulk.getChildren("work", MCRORCIDNamespaces.NS_WORK)) {
                String putCode = workXML.getAttributeValue("put-code");
                MCRWork work = putCode2work.get(putCode);
                work.setFromWorkXML(workXML);
            }
        }
    }

    Element fetchWorksXML(String path) throws JDOMException, IOException, SAXException {
        MCRContent response = MCRORCIDClient.instance().get(profile.getORCID() + "/" + path);
        return T_WORKXML2MODS.transform(response).asXML().detachRootElement();
    }
}
