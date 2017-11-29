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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.bibtex.MCRBibTeX2MODSTransformer;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid.MCRORCIDConstants;
import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.oauth.MCRReadPublicTokenFactory;
import org.xml.sax.SAXException;

/**
 * Provides functionality to fetch work groups, work summaries and work details
 * from a remote ORCID profile
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorksFetcher {

    private static final Logger LOGGER = LogManager.getLogger(MCRWorksFetcher.class);

    /** The maximum number of works to fetch at once in a bulk request */
    private static final int BULK_FETCH_SIZE = MCRConfiguration.instance().getInt("MCR.ORCID.Works.BulkFetchSize");

    /** Transformer used to convert ORCID's work XML schema to MODS and a representation we use here */
    private static final MCRContentTransformer T_WORK2MCR = MCRContentTransformerFactory.getTransformer("Work2MyCoRe");

    /** Transformer used to parse bibTeX to MODS */
    private static final MCRContentTransformer T_BIBTEX2MODS = new MCRBibTeX2MODSTransformer();

    private MCRORCIDProfile orcid;

    public MCRWorksFetcher(MCRORCIDProfile orcid) {
        this.orcid = orcid;
    }

    List<MCRGroupOfWorks> fetchGroups(MCRWorksSection worksSection)
        throws JDOMException, IOException, SAXException {
        WebTarget target = orcid.getWebTarget().path("works");
        Element worksXML = fetchWorksXML(target);

        List<MCRGroupOfWorks> groups = new ArrayList<>();
        for (Element groupXML : worksXML.getChildren("group", MCRORCIDConstants.NS_ACTIVITIES)) {
            MCRGroupOfWorks group = new MCRGroupOfWorks();
            groups.add(group);

            for (Element workSummary : groupXML.getChildren("work-summary", MCRORCIDConstants.NS_WORK)) {
                String putCode = workSummary.getAttributeValue("put-code");
                MCRWork work = worksSection.getWork(putCode);
                if (work == null) {
                    work = new MCRWork(orcid, putCode);
                    setFromWorkXML(work, workSummary);
                }
                group.add(work);
            }
        }
        return groups;
    }

    void fetchDetails(MCRWorksSection worksSection) throws IOException, JDOMException, SAXException {
        List<String> putCodes = new ArrayList<>();
        worksSection.getWorks().forEach(work -> putCodes.add(work.getPutCode()));

        for (int offset = 0; offset < putCodes.size(); offset += BULK_FETCH_SIZE) {
            int chunkEndIndex = Math.min(offset + BULK_FETCH_SIZE, putCodes.size());
            String joinedPutCodes = StringUtils.join(putCodes.subList(offset, chunkEndIndex), ',');
            WebTarget target = orcid.getWebTarget().path("works").path(joinedPutCodes);
            Element bulk = fetchWorksXML(target);

            for (Element workXML : bulk.getChildren("work", MCRORCIDConstants.NS_WORK)) {
                String putCode = workXML.getAttributeValue("put-code");
                MCRWork work = worksSection.getWork(putCode);
                setFromWorkXML(work, workXML);
            }
        }
    }

    void fetchDetails(MCRWork work) throws JDOMException, IOException, SAXException {
        WebTarget target = orcid.getWebTarget().path("work").path(work.getPutCode());
        Element workXML = fetchWorksXML(target);
        setFromWorkXML(work, workXML);
    }

    private Element fetchWorksXML(WebTarget target) throws JDOMException, IOException, SAXException {
        LOGGER.info("get {}", target.getUri());
        Builder b = target.request().accept(MCRORCIDConstants.ORCID_XML_MEDIA_TYPE)
            .header("Authorization", "Bearer " + MCRReadPublicTokenFactory.getToken());
        MCRContent response = new MCRStreamContent(b.get(InputStream.class));
        MCRContent transformed = T_WORK2MCR.transform(response);
        return transformed.asXML().detachRootElement();
    }

    /** Sets the work's properties from the pre-processed, transformed works XML */
    private void setFromWorkXML(MCRWork work, Element workXML) {
        Element mods = workXML.getChild("mods", MCRConstants.MODS_NAMESPACE).detach();

        String bibTeX = workXML.getChildTextTrim("bibTeX");
        Optional<Element> modsFromBibTeX = bibTeX2MODS(bibTeX);
        modsFromBibTeX.ifPresent(m -> MCRMergeTool.merge(mods, m));

        work.setMODS(mods);
        String sourceID = workXML.getAttributeValue("source");
        work.setSource(MCRWorkSource.getInstance(sourceID));

        if (work.getSource().isThisApplication()) {
            String oid = workXML.getAttributeValue("oid");
            work.setObjectID(MCRObjectID.getInstance(oid));
        }
    }

    /**
     * Parses the bibTeX code that may be included in the work entry
     * and returns its transformation to MODS
     */
    private Optional<Element> bibTeX2MODS(String bibTeX) {
        if ((bibTeX != null) && !bibTeX.isEmpty()) {
            try {
                MCRContent result = T_BIBTEX2MODS.transform(new MCRStringContent(bibTeX));
                Element modsCollection = result.asXML().getRootElement();
                Element modsFromBibTeX = modsCollection.getChild("mods", MCRConstants.MODS_NAMESPACE);
                // Remove mods:extension containing the original BibTeX:
                modsFromBibTeX.removeChildren("extension", MCRConstants.MODS_NAMESPACE);
                return Optional.of(modsFromBibTeX);
            } catch (Exception ex) {
                String msg = "Exception parsing BibTeX: " + bibTeX;
                LOGGER.warn("{} {}", msg, ex.getMessage());
            }
        }
        return Optional.empty();
    }
}
