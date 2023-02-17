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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
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

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_WORK_PREFIX = MCRORCIDConstants.CONFIG_PREFIX + "Work.";

    private static final boolean ALWAYS_UPDATE_OWN_WORK
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "AlwaysUpdateOwn", Boolean::parseBoolean);

    private static final boolean ALWAYS_CREATE_OWN_WORK
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "AlwaysCreateOwn", Boolean::parseBoolean);

    private static final boolean ALWAYS_UPDATE_PUT_CODES
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "AlwaysUpdatePutCodes", Boolean::parseBoolean);

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
     * Returns a Stream of WorkSummaries matching the identifiers of the MCRObject.
     *
     * @param object the MCRObject
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(MCRObject object,
        List<WorkSummary> workSummaries) {
        return findMatchingSummariesByIdentifiers(object, workSummaries.stream());
    }

    /**
     * Returns a Stream of WorkSummaries matching the identifiers of the MCRObject.
     *
     * @param object the MCRObject
     * @param workSummaries Stream of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(MCRObject object,
        Stream<WorkSummary> workSummaries) {
        return findMatchingSummariesByIdentifiers(MCRORCIDUtils.getIdentifiers(new MCRMODSWrapper(object)),
            workSummaries);
    }

    /**
     * Returns a Stream of WorkSummaries matching a set of MCRIdentifiers.
     *
     * @param identifiers the set of MCRIdentifiers
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(Set<MCRIdentifier> identifiers,
        Stream<WorkSummary> workSummaries) {
        if (identifiers.isEmpty()) {
            return Stream.empty();
        }
        return workSummaries
            .filter(
                w -> hasMatch(
                    w.getExternalIdentifiers().getExternalIdentifier().stream()
                        .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet()),
                    identifiers));
    }

    /**
     * Publishes MCRObject with given MCRORCIDCredentials to ORCID.
     * 
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdateOwn=
     * MCR.ORCID2.Work.AlwaysCreateOwn=
     * MCR.ORCID2.Work.AlwaysUpdatePutCodes=
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @return ORCID put code of published MCRObject or 0
     * @throws MCRORCIDException if publishing fails
     * @see #retrieveWorkInfo
     * @see #publishWork
     */
    public static long publishObjectToORCID(MCRObject object, MCRORCIDCredentials credentials)
        throws MCRORCIDException {
        try {
            final MCRORCIDUserInfo userInfo
                = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, credentials.getORCID()))
                    .orElseGet(() -> new MCRORCIDUserInfo(credentials.getORCID()));
            final MCRORCIDPutCodeInfo currentWorkInfo = userInfo.getWorkInfo(); // no clone necessary
            retrieveWorkInfo(object, credentials, userInfo);
            if (!Objects.equals(currentWorkInfo, userInfo.getWorkInfo())) {
                MCRORCIDMetadataUtils.updateUserInfoByORCID(object, credentials.getORCID(), userInfo);
            }
            if (!userInfo.getWorkInfo().hasOwnPutCode() && !ALWAYS_CREATE_OWN_WORK) {
                return 0;
            }
            final Work work = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(object.createXML()));
            final long putCode = publishWork(work, credentials, userInfo.getWorkInfo());
            userInfo.getWorkInfo().setOwnPutCode(putCode);
            MCRORCIDMetadataUtils.updateUserInfoByORCID(object, credentials.getORCID(), userInfo);
            return putCode;
        } catch (Exception e) {
            throw new MCRORCIDException("Publishing to ORCID failed", e);
        }
    }

    /**
     * Retrieves work info for MCRORCIDUserInfo of MCRObject with MCRORCIDCredentials.
     * 
     * Strategie can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdatePutCodes=
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @param userInfo the userInfo
     * @throws MCRORCIDException if retrieving fails
     */
    public static void retrieveWorkInfo(MCRObject object, MCRORCIDCredentials credentials, MCRORCIDUserInfo userInfo)
        throws MCRORCIDException {
        if (userInfo.getWorkInfo() == null || ALWAYS_UPDATE_PUT_CODES) {
            if (userInfo.getWorkInfo() == null) {
                userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
            }
            updateWorkInfo(object, credentials, userInfo.getWorkInfo());
        }
    }

    /**
     * Creates work info for MCRORCIDCredentials with MCRObject as reference.
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @param workInfo the initial work info
     * @throws MCRORCIDException look up request fails
     */
    private static void updateWorkInfo(MCRObject object, MCRORCIDCredentials credentials,
        MCRORCIDPutCodeInfo workInfo) throws MCRORCIDException {
        final MCRORCIDClient memberClient = MCRORCIDClientHelper.getClientFactory().createMemberClient(credentials);
        Stream<WorkSummary> summaries = null;
        try {
            summaries = memberClient.fetch(MCRORCIDSectionImpl.WORKS, Works.class).getWorkGroup().stream()
                .flatMap(g -> g.getWorkSummary().stream());
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Error during update", e);
        }
        final Stream<WorkSummary> matchingWorks = findMatchingSummariesByIdentifiers(object, summaries);
        long ownPutCode = 0;
        // validate current own put code
        if (workInfo.hasOwnPutCode()) {
            // try to find own work via identifiers as fallback
            ownPutCode = summaries.filter(w -> w.getPutCode().equals(workInfo.getOwnPutCode())).findFirst()
                .map(WorkSummary::getPutCode).orElseGet(
                    () -> findSummaryCreateByThisApplication(matchingWorks).map(WorkSummary::getPutCode).orElse(0l));
        } else {
            ownPutCode = findSummaryCreateByThisApplication(matchingWorks).map(WorkSummary::getPutCode).orElse(0l);
        }
        workInfo.setOwnPutCode(ownPutCode);
        final long[] otherPutCodes
            = matchingWorks.filter(w -> !MCRORCIDUtils.isCreatedByThisApplication(w.retrieveSourcePath()))
                .map(WorkSummary::getPutCode).mapToLong(l -> (long) l).toArray();
        workInfo.setOtherPutCodes(otherPutCodes);
    }

    /**
     * Creates/Updates Work to ORCID profile by MCRORCIDCredentials and MCRORCIDUserInfo.
     * 
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdateOwn=
     * MCR.ORCID2.Work.AlwaysCreateOwn=
     * 
     * @param work the Work
     * @param credentials the MCRORCIDCredentials
     * @param workInfo the MCRORCIDUserInfo
     * @return ORCID put code of created/updated work or 0
     * @throws MCRORCIDException if scope is invalid
     * @throws MCRORCIDRequestException if publishing fails
     */
    protected static long publishWork(Work work, MCRORCIDCredentials credentials, MCRORCIDPutCodeInfo workInfo) 
        throws IllegalArgumentException, MCRORCIDRequestException, MCRORCIDException {
        final Optional<String> scope = Optional.ofNullable(credentials.getScope());
        if (scope.isPresent() && !scope.get().contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            throw new MCRORCIDException("The scope is invalid"); // TODO maybe own exception
        }
        final MCRORCIDClient memberClient = MCRORCIDClientHelper.getClientFactory().createMemberClient(credentials);
        final long ownPutCode = workInfo.getOwnPutCode();
        final long[] otherPutCodes = workInfo.getOtherPutCodes();
        // matching work should exists in profile
        if (ownPutCode > 0 || (otherPutCodes != null && otherPutCodes.length > 0)) {
            if (ownPutCode > 0) {
                try {
                    if (ALWAYS_UPDATE_OWN_WORK) {
                        final Work remoteWork =
                            memberClient.fetch(MCRORCIDSectionImpl.WORK, Work.class, ownPutCode);
                        // check if update is required
                        if (!checkWorkEquality(work, remoteWork)) {
                            memberClient.update(MCRORCIDSectionImpl.WORK, ownPutCode, work);
                        }
                    }
                    return ownPutCode;
                } catch (MCRORCIDRequestException e) {
                    // skip 404
                    if (Objects.equals(e.getErrorResponse().getStatus(), Response.Status.NOT_FOUND)) {
                        LOGGER.info("Work of user {} with put code {} not found.", credentials.getORCID(), ownPutCode);
                    } else {
                        throw e;
                    }
                }
            }
            return ALWAYS_CREATE_OWN_WORK ? memberClient.create(MCRORCIDSectionImpl.WORK, work) : 0;
        }
        return memberClient.create(MCRORCIDSectionImpl.WORK, work);
    }

    /**
     * Compares works and possibly ignores the credit name of the contributors.
     * 
     * @param localWork the local Work
     * @param remoteWork the remote work
     * @return true if localWork equals remoteWork
     */
    private static boolean checkWorkEquality(Work localWork, Work remoteWork) {
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
            return false;
        }
        if (!Objects.equals(localWork.getWorkContributors(), remoteWork.getWorkContributors())) {
            if (localWork.getWorkContributors() == null || remoteWork.getWorkContributors() == null) {
                return false;
            } else {
                final List<Contributor> localContributor = localWork.getWorkContributors().getContributor();
                final List<Contributor> remoteContributor = remoteWork.getWorkContributors().getContributor();
                if (localContributor.size() != remoteContributor.size()) {
                    return false;
                }
                final Iterator<Contributor> itl = localContributor.iterator();
                final Iterator<Contributor> itr = remoteContributor.iterator();
                while (itl.hasNext()) {
                    final Contributor cl = itl.next();
                    final Contributor cr = itr.next();
                    if (!Objects.equals(cl.getContributorOrcid(), cr.getContributorOrcid()) ||
                        cl.getContributorOrcid() == null &&
                            !Objects.equals(cl.getCreditName(), cr.getCreditName())
                        ||
                        !Objects.equals(cl.getContributorEmail(), cr.getContributorEmail()) ||
                        !Objects.equals(cl.getContributorAttributes(), cr.getContributorAttributes())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Optional<WorkSummary> findSummaryCreateByThisApplication(Stream<WorkSummary> summaries) {
        return summaries.filter(s -> MCRORCIDUtils.isCreatedByThisApplication(s.retrieveSourcePath())).findFirst();
    }

    private static Element mergeElements(List<Element> elements) {
        final Element result = elements.get(0);
        elements.stream().skip(1).forEach(e -> MCRMergeTool.merge(result, e));
        return result;
    }

    private static boolean hasMatch(Set<MCRIdentifier> a, Set<MCRIdentifier> b) {
        a.retainAll(b);
        return !a.isEmpty();
    }
}
