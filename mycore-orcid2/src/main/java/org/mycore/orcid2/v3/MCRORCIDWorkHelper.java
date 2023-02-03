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

package org.mycore.orcid2.v3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkSummaryTransformer;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformer;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.xml.sax.SAXException;

/**
 * Provides utility methods for Work and WorkSummaries.
 */
public class MCRORCIDWorkHelper {

    /**
     * Transforms and merges List of work summaries to Element.
     * 
     * @param works List of work summaries
     * @return merged Element
     * @throws JAXBException
     * @throws JDOMException
     * @throws SAXException
     * @throws IOException
     */
    public static Element buildMergedMODSFromWorkSummaries(List<WorkSummary> workSummaries)
        throws IOException, JAXBException, JDOMException, SAXException {
        final List<Element> modsElements = new ArrayList<Element>();
        for (WorkSummary workSummary : workSummaries) {
            modsElements.add(MCRORCIDWorkSummaryTransformer.getInstance().transformToMODS(workSummary));
        }
        return mergeElements(modsElements);
    }

    /**
     * Transforms and merges List of works to Element.
     * 
     * @param works List of works
     * @return merged Element
     * @throws JAXBException
     * @throws JDOMException
     * @throws SAXException
     * @throws IOException
     */
    public static Element buildMergedMODSFromWorks(List<Work> works)
        throws JAXBException, JDOMException, SAXException, IOException {
        final List<Element> modsElements = new ArrayList<Element>();
        for (Work work : works) {
            modsElements.add(MCRORCIDWorkTransformer.getInstance().transformToMODS(work));
        }
        return mergeElements(modsElements);
    }

    /**
     * Transforms List of work summaries to List of elements.
     * 
     * @param works List of work summaries
     * @return List of elements
     * @throws JAXBException
     * @throws JDOMException
     * @throws SAXException
     * @throws IOException
     */
    public static List<Element> buildUnmergedMODSFromWorkSummaries(List<WorkSummary> workSummaries)
        throws JAXBException, JDOMException, SAXException, IOException {
        final List<Element> modslist = new ArrayList<>();
        for (WorkSummary workSummary : workSummaries) {
            modslist.add(MCRORCIDWorkSummaryTransformer.getInstance().transformToMODS(workSummary).detach());
        }
        return modslist;
    }

    /**
     * Transforms List of works to List of elements.
     * 
     * @param works List of works
     * @return List of elements
     * @throws JAXBException
     * @throws JDOMException
     * @throws SAXException
     * @throws IOException
     */
    public static List<Element> buildUnmergedMODSFromWorks(List<Work> works)
        throws JAXBException, JDOMException, SAXException, IOException {
        final List<Element> modslist = new ArrayList<>();
        for (Work work : works) {
            modslist.add(MCRORCIDWorkTransformer.getInstance().transformToMODS(work).detach());
        }
        return modslist;
    }

    /**
     * Publishes MCRObject with given MCRORCIDCredentials to ORCID.
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @return ORCID put code of published MCRObject
     * @throws MCRORCIDRequestException if publishing fails
     * @throws MCRException if transformation to orcid model fails
     */
    public static long publishToORCID(MCRObject object, MCRORCIDCredentials credentials)
        throws MCRException, MCRORCIDRequestException {
        final MCRORCIDClient memberClient = MCRORCIDAPIClientFactoryImpl.getInstance().createMemberClient(credentials);
        try {
            final Works works = memberClient.fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            final List<WorkSummary> summaries
                = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
            final MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(object.getId());
            final Work transformedWork = MCRORCIDWorkTransformer.getInstance().transformToWork(content);
            final WorkSummary work = findMatchingSummaries(object, summaries).findFirst()
                .orElse(null);
            if (work != null) {
                // TODO check if need to set put code in model...
                // TODO check if update is required
                memberClient.update(MCRORCIDSectionImpl.WORK, work.getPutCode(), transformedWork);
                return work.getPutCode();
            }
            return memberClient.create(MCRORCIDSectionImpl.WORK, transformedWork);
        } catch (IOException | JAXBException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Returns a Stream of WorkSummaries matching the identifiers of the MCRObject.
     *
     * @param object the MCRObject
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummaries(MCRObject object, List<WorkSummary> workSummaries) {
        final Set<String> identifiersKeys = MCRORCIDUtils.getIdentifierKeys(new MCRMODSWrapper(object));
        if (identifiersKeys.isEmpty()) {
            return Stream.empty();
        }
        return workSummaries.stream()
            .filter(w -> hasMatch(
                w.getExternalIdentifiers().getExternalIdentifier().stream()
                    .map(i -> MCRORCIDUtils.buildIdentifierKey(i.getType(), i.getValue())).collect(Collectors.toSet()),
                identifiersKeys));
    }

    private static Element mergeElements(List<Element> elements) {
        final Element result = elements.get(0);
        for (int i = 1; i < elements.size(); i++) {
            MCRMergeTool.merge(result, elements.get(i));
        }
        return result;
    }

    private static boolean hasMatch(Set<String> a, Set<String> b) {
        a.retainAll(b);
        return !a.isEmpty();
    }
}
