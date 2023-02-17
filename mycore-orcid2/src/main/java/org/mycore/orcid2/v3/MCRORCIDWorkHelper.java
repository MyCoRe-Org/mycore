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

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.MCRORCIDConstants;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.message.ScopeConstants;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;

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
     * Updates work info for MCRORCIDCredentials with MCRObject as reference.
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
}
