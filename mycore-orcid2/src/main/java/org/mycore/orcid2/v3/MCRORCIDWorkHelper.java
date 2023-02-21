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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDUserClient;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.message.ScopeConstants;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.search.Search;

/**
 * Provides utility methods for ORCID Work.
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

    private static final boolean COLLECT_EXTERNAL_PUT_CODES
        = MCRConfiguration2.getOrThrow(CONFIG_WORK_PREFIX + "CollectExternalPutCodes", Boolean::parseBoolean);

    /**
     * Publishes MCRObject to ORCID for MCRCredentials.
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @throws MCRORCIDException if there is a general error
     * @see #publishObjectToORCID(MCRObject, List<MCRORCIDCredentials>)
     *
     */
    public static void publishObjectToORCID(MCRObject object, MCRORCIDCredentials credentials) {
        publishObjectToORCID(object, List.of(credentials));
    }

    /**
     * Publishes MCRObject to ORCID for given List of MCRCredentials.
     * In the process, put codes may be collected and stored in the flags.
     *
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdateOwn=
     * MCR.ORCID2.Work.AlwaysCreateOwn=
     * MCR.ORCID2.Work.AlwaysUpdatePutCodes=
     * MCR.ORCID2.Work.CollectExternalPutCodes=
     * 
     * @param object the MCRObject
     * @param credentialsList List of MCRORCIDCredentials
     * @throws MCRORCIDException if there is a general error
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    public static void publishObjectToORCID(MCRObject object, List<MCRORCIDCredentials> credentialsList)
        throws MCRORCIDException {
        Work work = null;
        try {
            work = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(object.createXML()));
        } catch (MCRORCIDTransformationException e) {
            throw new MCRORCIDException("Cannot publish object", e);
        }
        final List<String> failedORCIDs = new ArrayList<>();
        for (MCRORCIDCredentials credentials : credentialsList) {
            final String orcid = credentials.getORCID();
            try {
                final MCRORCIDUserInfo userInfo
                    = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                        .orElseGet(() -> new MCRORCIDUserInfo(orcid));
                final MCRORCIDPutCodeInfo currentWorkInfo = userInfo.getWorkInfo(); // no clone necessary
                retrieveWorkInfo(object, credentials, userInfo);
                if (!Objects.equals(currentWorkInfo, userInfo.getWorkInfo())) {
                    // save is safe ;)
                    MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
                }
                if (!userInfo.getWorkInfo().hasOwnPutCode() && !ALWAYS_CREATE_OWN_WORK) {
                    // optimization
                    return;
                }
                final long putCode = publishWork(work, credentials, userInfo.getWorkInfo());
                userInfo.getWorkInfo().setOwnPutCode(putCode);
                MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
            } catch (Exception e) {
                failedORCIDs.add(orcid);
                LOGGER.warn("Could not publish {} to ORCID profile: {}.", object.getId(), orcid, e);
            }
        }
        // collect put codes for work without credentials
        if (MCRORCIDMetadataUtils.SAVE_OTHER_PUT_CODES) {
            collectAndSaveOtherPutCodes(failedORCIDs, object);
            if (COLLECT_EXTERNAL_PUT_CODES) {
                final Set<String> matchingORCIDs = Set.copyOf(getMatchingORCIDs(object));
                matchingORCIDs.removeAll(credentialsList.stream().map(MCRORCIDCredentials::getORCID).toList());
                collectAndSaveOtherPutCodes(List.copyOf(matchingORCIDs), object);
            }
        }
    }

    /**
     * Retrieves work info for MCRORCIDUserInfo for ORCID iD of MCRObject.
     * 
     * Strategie can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdatePutCodes=
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @param userInfo the userInfo
     * @throws MCRORCIDException if retrieving fails
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    public static void retrieveWorkInfo(MCRObject object, String orcid, MCRORCIDUserInfo userInfo)
        throws MCRORCIDException {
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials(orcid);
        retrieveWorkInfo(object, credentials, userInfo);
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
     * @see MCRORCIDUtils#getTrustedIdentifiers
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
     * Updates work info for MCRORCIDCredentials with MCRObject as reference.
     * 
     * @param object the MCRObject
     * @param credentials the MCRORCIDCredentials
     * @param workInfo the initial work info
     * @throws MCRORCIDException look up request fails
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    private static void updateWorkInfo(MCRObject object, MCRORCIDCredentials credentials,
        MCRORCIDPutCodeInfo workInfo) throws MCRORCIDException {
        Stream<WorkSummary> summaries = null;
        try {
            Works works = null;
            if (credentials.getAccessToken() != null) {
                works = MCRORCIDClientHelper.getClientFactory().createUserClient(credentials)
                    .fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            } else {
                // Use read client instead
                works = MCRORCIDClientHelper.getClientFactory().createReadClient()
                    .fetch(credentials.getORCID(), MCRORCIDSectionImpl.WORKS, Works.class);
            }
            summaries = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream());
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Error during update", e);
        }
        MCRORCIDWorkSummaryUtils.updateWorkInfoFromSummaries(object, summaries, workInfo);
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
     * @throws MCRORCIDException if scope is invalid
     * @throws MCRORCIDRequestException if publishing fails
     */
    private static long publishWork(Work work, MCRORCIDCredentials credentials, MCRORCIDPutCodeInfo workInfo)
        throws IllegalArgumentException, MCRORCIDRequestException, MCRORCIDException {
        final Optional<String> scope = Optional.ofNullable(credentials.getScope());
        if (scope.isPresent() && !scope.get().contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            throw new MCRORCIDException("The scope is invalid");
        }
        final MCRORCIDUserClient memberClient = MCRORCIDClientHelper.getClientFactory().createUserClient(credentials);
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
                        if (!MCRORCIDWorkUtils.checkWorkEquality(work, remoteWork)) {
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

    private static List<String> getMatchingORCIDs(MCRObject object) throws MCRORCIDException {
        final Set<MCRIdentifier> identifiers = MCRORCIDUtils.getTrustedIdentifiers(new MCRMODSWrapper(object));
        final String query = buildORCIDIdentifierSearchQuery(identifiers);
        try {
            return MCRORCIDClientHelper.getClientFactory().createReadClient()
                .search(MCRORCIDSearchImpl.DEFAULT, query, Search.class).getResults().stream()
                .map(r -> r.getOrcidIdentifier().getPath()).toList();
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Error while finding matching ORCIDs", e);
        }
    }

    // TODO test if escaping is necessary
    private static String buildORCIDIdentifierSearchQuery(Set<MCRIdentifier> identifiers) {
        String query = "";
        for (MCRIdentifier i : List.copyOf(identifiers)) {
            if (!query.isEmpty()) {
                query += " OR ";
            }
            final String value = i.getValue();
            query += String.format(Locale.ROOT, "%s-self:(%s OR %s OR %s)", i.getType(), value, value.toUpperCase(),
                value.toLowerCase());
        }
        return query;
    }

    private static void collectAndSaveOtherPutCodes(List<String> orcids, MCRObject object) throws MCRORCIDException {
        orcids.forEach(orcid -> {
            try {
                final MCRORCIDUserInfo userInfo
                    = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                        .orElseGet(() -> new MCRORCIDUserInfo(orcid));
                final MCRORCIDPutCodeInfo currentWorkInfo = userInfo.getWorkInfo(); // no clone necessary
                retrieveWorkInfo(object, orcid, userInfo);
                if (!Objects.equals(currentWorkInfo, userInfo.getWorkInfo())) {
                    MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
                }
            } catch (MCRORCIDException e) {
                LOGGER.warn("Could not collect put codes for {} and {}.", object.getId(), orcid);
            }
        });
    }
}
