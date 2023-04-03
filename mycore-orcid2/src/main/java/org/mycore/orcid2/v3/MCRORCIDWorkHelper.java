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
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.MCRORCIDUserClient;
import org.mycore.orcid2.client.exception.MCRORCIDInvalidScopeException;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
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
     * Publishes MCRObject to ORCID for MCRORCIDCredential.
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDException if there is a general error
     * @see #publishObjectToORCID(MCRObject, Map<String, MCRORCIDCredential>)
     *
     */
    public static void publishToORCIDAndUpdateWorkInfo(MCRObject object, String orcid, MCRORCIDCredential credential) {
        final Map<String, MCRORCIDCredential> credentials = new HashMap<>();
        credentials.put(orcid, credential);
        publishObjectToORCID(object, credentials);
    }

    /**
     * Publishes MCRObject to ORCID for given List of MCRORCIDCredential.
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
     * @param credentials List of MCRORCIDCredential
     * @throws MCRORCIDException if work transformation fails or cannot update flags
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    public static void publishToORCIDAndUpdateWorkInfo(MCRObject object, Map<String, MCRORCIDCredential> credentials) {
        final List<String> orcids = credentials.entrySet().stream().map(Map.Entry::getKey).toList();
        MCRORCIDMetadataUtils.cleanUpWorkInfosExcludingORCIDs(object, orcids);
        if (!credentials.isEmpty()) {
            try {
                publishObjectToORCID(object, credentials);
            } catch (MCRORCIDTransformationException e) {
                throw new MCRORCIDException("Cannot publish object", e);
            }
        }
        // collect put codes for work without credential
        if (MCRORCIDMetadataUtils.SAVE_OTHER_PUT_CODES && COLLECT_EXTERNAL_PUT_CODES) {
            final Set<String> matchingORCIDs = new HashSet<>(getMatchingORCIDs(object));
            matchingORCIDs.removeAll(orcids);
            collectAndSaveOtherPutCodes(List.copyOf(matchingORCIDs), object);
        }
    }

    private static void publishObjectToORCID(MCRObject object, Map<String, MCRORCIDCredential> credentials) {
        if (!MCRORCIDUtils.checkPublishState(object)) {
            throw new MCRORCIDException("Object has wrong state");
        }
        final MCRObject filteredObject = MCRORCIDUtils.filterObject(object);
        if (!MCRORCIDUtils.checkEmptyMODS(filteredObject)) {
            throw new MCRORCIDException("Filtered MODS is empty.");
        }
        final Work work = MCRORCIDWorkTransformerHelper
            .transformContent(new MCRJDOMContent(filteredObject.createXML()));
        final List<String> failedORCIDs = new ArrayList<>();
        for (Map.Entry<String, MCRORCIDCredential> entry : credentials.entrySet()) {
            final String orcid = entry.getKey();
            final MCRORCIDCredential credential = entry.getValue();
            try {
                final MCRORCIDUserInfo userInfo
                    = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                        .orElseGet(() -> new MCRORCIDUserInfo(orcid));
                final MCRORCIDPutCodeInfo currentWorkInfo = userInfo.getWorkInfo();
                retrieveWorkInfo(object, orcid, credential, userInfo);
                if (!Objects.equals(currentWorkInfo, userInfo.getWorkInfo())) {
                    // save is safe ;)
                    MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
                }
                if (!userInfo.getWorkInfo().hasOwnPutCode() && !ALWAYS_CREATE_OWN_WORK) {
                    // optimization
                    return;
                }
                publishWork(work, orcid, credential, userInfo.getWorkInfo());
                MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
            } catch (Exception e) {
                failedORCIDs.add(orcid);
                LOGGER.warn("Could not publish {} to ORCID profile: {}.", object.getId(), orcid, e);
            }
        }
        final List<String> successfullORCIDs = credentials.entrySet().stream().map(Map.Entry::getKey)
            .filter(o -> !failedORCIDs.contains(o)).toList();
        MCRORCIDMetadataUtils.cleanUpWorkInfosExcludingORCIDs(object, successfullORCIDs);
        if (MCRORCIDMetadataUtils.SAVE_OTHER_PUT_CODES) {
            collectAndSaveOtherPutCodes(failedORCIDs, object);
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
        retrieveWorkInfo(object, orcid, new MCRORCIDCredential(), userInfo);
    }

    /**
     * Retrieves work info for MCRORCIDUserInfo of MCRObject with MCRORCIDCredential.
     * 
     * Strategie can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdatePutCodes=
     * 
     * @param object the MCRObject
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @param userInfo the userInfo
     * @throws MCRORCIDException if retrieving fails
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    public static void retrieveWorkInfo(MCRObject object, String orcid, MCRORCIDCredential credential,
        MCRORCIDUserInfo userInfo) {
        if (userInfo.getWorkInfo() == null || ALWAYS_UPDATE_PUT_CODES) {
            if (userInfo.getWorkInfo() == null) {
                userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
            }
            updateWorkInfo(object, orcid, credential, userInfo.getWorkInfo());
        }
    }

    /**
     * Updates work info for MCRORCIDCredential with MCRObject as reference.
     * 
     * @param object the MCRObject
     * @param credential the MCRORCIDCredential
     * @param workInfo the initial work info
     * @throws MCRORCIDException look up request fails
     * @see MCRORCIDUtils#getTrustedIdentifiers
     */
    private static void updateWorkInfo(MCRObject object, String orcid, MCRORCIDCredential credential,
        MCRORCIDPutCodeInfo workInfo) {
        List<WorkSummary> summaries = null;
        try {
            Works works = null;
            if (credential.getAccessToken() != null) {
                works = MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential)
                    .fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            } else {
                // Use read client instead
                works = MCRORCIDClientHelper.getClientFactory().createReadClient()
                    .fetch(orcid, MCRORCIDSectionImpl.WORKS, Works.class);
            }
            summaries = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Error during update", e);
        }
        MCRORCIDWorkSummaryUtils.updateWorkInfoFromSummaries(object, summaries, workInfo);
    }

    /**
     * Creates/Updates Work to ORCID profile by MCRORCIDCredential and MCRORCIDUserInfo.
     * 
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.AlwaysUpdateOwn=
     * MCR.ORCID2.Work.AlwaysCreateOwn=
     * 
     * @param work the Work
     * @param credential the MCRORCIDCredential
     * @param workInfo the MCRORCIDUserInfo
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     * @throws MCRORCIDRequestException if publishing fails
     */
    private static void publishWork(Work work, String orcid, MCRORCIDCredential credential,
        MCRORCIDPutCodeInfo workInfo) {
        final Optional<String> scope = Optional.ofNullable(credential.getScope());
        if (scope.isPresent() && !scope.get().contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            throw new MCRORCIDInvalidScopeException();
        }
        final MCRORCIDUserClient memberClient
            = MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential);
        long ownPutCode = workInfo.getOwnPutCode();
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
                            work.setPutCode(ownPutCode);
                            memberClient.update(MCRORCIDSectionImpl.WORK, ownPutCode, work);
                            LOGGER.info("Updated work of user {} with put code {}.", orcid, ownPutCode);
                        } else {
                          LOGGER.info("Update of work of user {} with put code {} not required.", orcid, ownPutCode);
                        }
                    }
                    return;
                } catch (MCRORCIDRequestException e) {
                    // skip 404
                    if (Objects.equals(e.getResponse().getStatus(), Response.Status.NOT_FOUND)) {
                        LOGGER.info("Work of user {} with put code {} not found.", orcid, ownPutCode);
                    } else {
                        throw e;
                    }
                }
            }
            // clean up own put code
            ownPutCode = 0;
            if (ALWAYS_CREATE_OWN_WORK) {
                ownPutCode = memberClient.create(MCRORCIDSectionImpl.WORK, work);
                LOGGER.info("Created work of user {} with put code {}.", orcid, ownPutCode);
            }
            workInfo.setOwnPutCode(ownPutCode);
        } else {
            ownPutCode = memberClient.create(MCRORCIDSectionImpl.WORK, work);
            LOGGER.info("Created work of user {} with put code {}.", orcid, ownPutCode);
            workInfo.setOwnPutCode(ownPutCode);
        }
    }

    private static List<String> getMatchingORCIDs(MCRObject object) {
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

    private static String buildORCIDIdentifierSearchQuery(Set<MCRIdentifier> identifiers) {
        String query = "";
        for (MCRIdentifier i : List.copyOf(identifiers)) {
            if (!query.isEmpty()) {
                query += " OR ";
            }
            final String value = i.getValue();
            query += String.format(Locale.ROOT, "%s-self:(%s OR %s OR %s)", i.getType(), value,
                value.toUpperCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
        }
        return query;
    }

    private static void collectAndSaveOtherPutCodes(List<String> orcids, MCRObject object) {
        orcids.forEach(orcid -> {
            try {
                final MCRORCIDUserInfo userInfo
                    = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                        .orElseGet(() -> new MCRORCIDUserInfo(orcid));
                final MCRORCIDPutCodeInfo currentWorkInfo = userInfo.getWorkInfo();
                retrieveWorkInfo(object, orcid, userInfo);
                if (userInfo.getWorkInfo().getOwnPutCode() != 0) {
                    final long ownPutCode = userInfo.getWorkInfo().getOwnPutCode();
                    userInfo.getWorkInfo().addOtherPutCode(ownPutCode);
                    userInfo.getWorkInfo().setOwnPutCode(0);
                }
                if (!Objects.equals(currentWorkInfo, userInfo.getWorkInfo())) {
                    MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
                }
            } catch (MCRORCIDException e) {
                LOGGER.warn("Could not collect put codes for {} and {}.", object.getId(), orcid);
            }
        });
    }
}
