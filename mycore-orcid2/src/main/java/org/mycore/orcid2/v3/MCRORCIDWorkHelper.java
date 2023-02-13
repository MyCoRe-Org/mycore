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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.user.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.message.ScopeConstants;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.xml.sax.SAXException;

/**
 * Provides utility methods for Work and WorkSummaries.
 */
public class MCRORCIDWorkHelper {

    private static final String CONFIG_WORK_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "Work.";

    private static final boolean ALWAYS_UPDATE_OWN_WORK
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "AlwaysUpdateOwn", Boolean::parseBoolean);

    private static final boolean ALWAYS_CREATE_OWN_WORK
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "AlwaysCreateOwn", Boolean::parseBoolean);

    /**
     * Transforms and merges List of works to Element.
     * 
     * @param works List of works
     * @return merged Element
     * @throws MCRORCIDTransformationException if transformation fails
     * @see MCRORCIDWorkHelper#buildUnmergedMODSFromWorks
     */
    public static Element buildMergedMODSFromWorks(List<Work> works) throws MCRORCIDTransformationException {
        return mergeElements(buildUnmergedMODSFromWorks(works));
    }

    /**
     * Transforms List of works to List of elements.
     * 
     * @param works List of works
     * @return List of elements
     * @throws MCRORCIDTransformationException if transformation fails
     */
    public static List<Element> buildUnmergedMODSFromWorks(List<Work> works) throws MCRORCIDTransformationException {
        final List<Element> modsElements = new ArrayList<>();
        works.forEach(w -> {
            try {
                modsElements.add(MCRORCIDWorkTransformerHelper.transformWork(w).asXML().detachRootElement());
            } catch (IOException | JDOMException | SAXException e) {
                throw new MCRORCIDTransformationException(e);
            }
        });
        return modsElements;
    }

    /**
     * Transforms List of work summaries to List of elements.
     * 
     * @param works List of work summaries
     * @return List of elements
     * @throws MCRORCIDTransformationException if transformation fails
     */
    public static List<Element> buildUnmergedMODSFromWorkSummaries(List<WorkSummary> works)
        throws MCRORCIDTransformationException {
        final List<Element> modsElements = new ArrayList<>();
        works.forEach(w -> {
            try {
                modsElements.add(MCRORCIDWorkTransformerHelper.transformWorkSummary(w).asXML().detachRootElement());
            } catch (IOException | JDOMException | SAXException e) {
                throw new MCRORCIDTransformationException(e);
            }
        });
        return modsElements;
    }

    /**
     * Publishes MCRObject with given MCRORCIDCredentials to ORCID.
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @return ORCID put code of published MCRObject
     * @throws MCRORCIDException if scope is invalid
     * @throws MCRORCIDTransformationException if transformation to orcid model fails
     * @throws MCRORCIDRequestException if publishing fails
     * @see #publishWork
     */
    public static long publishToORCID(MCRObject object, MCRORCIDCredentials credentials)
        throws MCRORCIDException, MCRORCIDTransformationException, MCRORCIDRequestException {
        return publishToORCID(object, Arrays.asList(credentials)).get(0);
    }

    /**
     * Publishes MCRObject with given list of MCRORCIDCredentials to ORCID.
     * 
     * @param object the MCRObject
     * @param credentialsList the list of MCRORCIDCredentials
     * @return List of ORCID put codes
     * @throws MCRORCIDException if scope is invalid
     * @throws MCRORCIDTransformationException if transformation to orcid model fails
     * @throws MCRORCIDRequestException if publishing fails
     * @see #publishWork
     */
    public static List<Long> publishToORCID(MCRObject object, List<MCRORCIDCredentials> credentialsList)
        throws MCRORCIDException, MCRORCIDTransformationException, MCRORCIDRequestException {
        final ArrayList<Long> putCodes = new ArrayList<Long>();
        try {
            final MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(object.getId());
            final Work work = MCRORCIDWorkTransformerHelper.transformContent(content);
            final Set<MCRIdentifier> identifiers = MCRORCIDUtils.getIdentifiers(new MCRMODSWrapper(object));
            for (MCRORCIDCredentials credentials : credentialsList) {
                putCodes.add(publishWork(work, identifiers, credentials)); // TODO Exception
            }
        } catch (IOException e) {
            throw new MCRORCIDTransformationException(e);
        }
        return putCodes;
    }

    /**
     * Creates/Updates Work to ORCID profile by credentials.
     * 
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdateOwn=
     * MCR.ORCID2.Work.AlwaysCreateOwn=
     * 
     * @param work the Work
     * @param identifiers List of MCRIdentifier to to determine a possible matching work
     * @param credentials the MCRORCIDCredentials
     * @return ORCID put code of created/updated work
     * @throws MCRORCIDException if scope is invalid
     * @throws MCRORCIDRequestException if publishing fails
     */
    private static long publishWork(Work work, Set<MCRIdentifier> identifiers, MCRORCIDCredentials credentials)
        throws MCRORCIDException, MCRORCIDRequestException {
        final String scope = credentials.getScope();
        if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            throw new MCRORCIDException("The scope is invalid"); // TODO maybe own exception
        }
        final MCRORCIDClient memberClient = MCRORCIDAPIClientFactoryImpl.getInstance().createMemberClient(credentials);
        final Works works = memberClient.fetch(MCRORCIDSectionImpl.WORKS, Works.class);
        final List<WorkSummary> summaries
            = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
        final Stream<WorkSummary> matchingWorks = findMatchingSummaries(identifiers, summaries);
        if (matchingWorks.count() != 0) {
            final WorkSummary matchingOwnWork = matchingWorks
                .filter(w -> MCRORCIDUtils.isCreatedByThisApplication(w.retrieveSourcePath())).findFirst()
                .orElse(null);
            if (matchingOwnWork != null) {
                if (ALWAYS_UPDATE_OWN_WORK) {
                    final Work remoteWork =
                        memberClient.fetch(MCRORCIDSectionImpl.WORK, Work.class, matchingOwnWork.getPutCode());
                    if (updateRequired(work, remoteWork)) {
                        memberClient.update(MCRORCIDSectionImpl.WORK, matchingOwnWork.getPutCode(), work);
                    }
                }
                return matchingOwnWork.getPutCode();
            }
            return ALWAYS_CREATE_OWN_WORK ? memberClient.create(MCRORCIDSectionImpl.WORK, work) : 0;
        }
        return memberClient.create(MCRORCIDSectionImpl.WORK, work);
    }

    /**
     * Returns a Stream of WorkSummaries matching the identifiers of the MCRObject.
     *
     * @param object the MCRObject
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummaries(MCRObject object, List<WorkSummary> workSummaries) {
        return findMatchingSummaries(MCRORCIDUtils.getIdentifiers(new MCRMODSWrapper(object)), workSummaries);
    }

    /**
     * Returns a Stream of WorkSummaries matching a set of MCRIdentifiers.
     *
     * @param identifiers the set of MCRIdentifiers
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummaries(Set<MCRIdentifier> identifiers,
            List<WorkSummary> workSummaries) {
        if (identifiers.isEmpty()) {
            return Stream.empty();
        }
        return workSummaries.stream()
            .filter(w -> hasMatch(
                w.getExternalIdentifiers().getExternalIdentifier().stream()
                    .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet()),
                identifiers));
    }

    private static Element mergeElements(List<Element> elements) {
        final Element result = elements.get(0);
        for (int i = 1; i < elements.size(); i++) {
            MCRMergeTool.merge(result, elements.get(i));
        }
        return result;
    }

    private static boolean hasMatch(Set<MCRIdentifier> a, Set<MCRIdentifier> b) {
        a.retainAll(b);
        return !a.isEmpty();
    }

    // TODO method name
    private static boolean updateRequired(Work localWork, Work remoteWork) {
        if (!Objects.equals(localWork.getWorkTitle(), remoteWork.getWorkTitle()) ||
                !Objects.equals(localWork.getShortDescription(), remoteWork.getShortDescription()) ||
                !Objects.equals(localWork.getWorkCitation(), remoteWork.getWorkCitation()) ||
                !Objects.equals(localWork.getWorkType(), remoteWork.getWorkType()) ||
                !Objects.equals(localWork.getPublicationDate(), remoteWork.getPublicationDate()) ||
                !Objects.equals(localWork.getUrl(), remoteWork.getUrl()) ||
                !Objects.equals(localWork.getJournalTitle(), remoteWork.getJournalTitle()) ||
                !Objects.equals(localWork.getLanguageCode(), remoteWork.getLanguageCode()) ||
                !Objects.equals(localWork.getCountry(), remoteWork.getCountry()) ||
                !Objects.equals(localWork.getExternalIdentifiers(), remoteWork.getExternalIdentifiers())) {
            return true;
        }
        if (!Objects.equals(localWork.getWorkContributors(), remoteWork.getWorkContributors())) {
            if (localWork.getWorkContributors() == null || remoteWork.getWorkContributors() == null) {
                return true;
            } else {
                final List<Contributor> localContributor = localWork.getWorkContributors().getContributor();
                final List<Contributor> remoteContributor = remoteWork.getWorkContributors().getContributor();
                if (localContributor.size() != remoteContributor.size()) {
                    return true;
                }
                final Iterator<Contributor> itl = localContributor.iterator();
                final Iterator<Contributor> itr = remoteContributor.iterator();
                while (itl.hasNext()) {
                    final Contributor cl = itl.next();
                    final Contributor cr = itr.next();
                    if (!Objects.equals(cl.getContributorOrcid(), cr.getContributorOrcid()) ||
                            cl.getContributorOrcid() == null &&
                            !Objects.equals(cl.getCreditName(), cr.getCreditName()) ||
                            !Objects.equals(cl.getContributorEmail(), cr.getContributorEmail()) ||
                            !Objects.equals(cl.getContributorAttributes(), cr.getContributorAttributes())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
