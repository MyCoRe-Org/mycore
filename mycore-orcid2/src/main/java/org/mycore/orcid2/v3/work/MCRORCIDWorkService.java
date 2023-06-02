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

package org.mycore.orcid2.v3.work;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.MCRORCIDUserClient;
import org.mycore.orcid2.client.exception.MCRORCIDInvalidScopeException;
import org.mycore.orcid2.client.exception.MCRORCIDNotFoundException;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDWorkAlreadyExistsException;
import org.mycore.orcid2.metadata.MCRORCIDFlagContent;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRORCIDUserProperties;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.MCRORCIDClientHelper;
import org.mycore.orcid2.v3.MCRORCIDSectionImpl;
import org.mycore.orcid2.v3.MCRORCIDSearchImpl;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.message.ScopeConstants;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.search.Search;

/**
 * Provides functionalities to create, update or delete works in ORCID profile.
 */
public class MCRORCIDWorkService {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String orcid;

    private final MCRORCIDCredential credential;

    private final MCRORCIDUserProperties userProperties;

    /**
     * Creates new MCRORCIDWorkService instance.
     * 
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @param userProperties the MCRORCIDUserProperties
     */
    public MCRORCIDWorkService(String orcid, MCRORCIDCredential credential, MCRORCIDUserProperties userProperties) {
        this.orcid = orcid;
        this.credential = credential;
        this.userProperties = userProperties;
    }

    /**
     * Creates MCRObject in ORCID profile for given MCRORCIDCredential and updates flag.
     *
     * Update and create strategies can be set via:
     * 
     * MCR.ORCID2.Work.CollectExternalPutCodes=
     * 
     * @param object the MCRObject
     * @throws MCRORCIDException if cannot create work, transformation fails or cannot update flags
     */
    public void createWork(MCRObject object) {
        if (!MCRORCIDUtils.checkPublishState(object)) {
            throw new MCRORCIDException("Object has wrong state");
        }
        final MCRObject filteredObject = MCRORCIDUtils.filterObject(object);
        if (!MCRORCIDUtils.checkEmptyMODS(filteredObject)) {
            throw new MCRORCIDException("Filtered MODS is empty.");
        }
        try {
            final MCRORCIDUserInfo userInfo
                = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                    .orElse(new MCRORCIDUserInfo(orcid));
            if (userInfo.getWorkInfo() == null) {
                userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
            }
            final Work work
                = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(filteredObject.createXML()));
            doUpdateWorkInfo(MCRORCIDWorkUtils.listTrustedIdentifiers(work), userInfo.getWorkInfo(), orcid, credential);
            if (userInfo.getWorkInfo().hasOwnPutCode()) {
                // there is an inconsistent state
                throw new MCRORCIDWorkAlreadyExistsException();
            } else if (userInfo.getWorkInfo().getOtherPutCodes() != null && userProperties.isCreateOwnDuplicate()) {
                // there is already work, a duplicate is allowed to be created
                doCreateWork(work, userInfo.getWorkInfo(), orcid, credential);
            } else if (userInfo.getWorkInfo().getOtherPutCodes() == null && userProperties.isCreateOwn()) {
                doCreateWork(work, userInfo.getWorkInfo(), orcid, credential);
            } else {
                throw new MCRORCIDWorkAlreadyExistsException();
            }
            MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
            MCRMetadataManager.update(object);
        } catch (Exception e) {
            throw new MCRORCIDException("Cannot create work", e);
        }
    }

    /**
     * Deletes Work in ORCID profile and updates flag.
     * 
     * @param object the MCRObject
     * @throws MCRORCIDException if delete fails
     */
    public void deleteWork(MCRObject object) {
        try {
            final MCRORCIDUserInfo userInfo
                = Optional.ofNullable(MCRORCIDMetadataUtils.getUserInfoByORCID(object, orcid))
                    .orElse(new MCRORCIDUserInfo(orcid));
            if (userInfo.getWorkInfo() == null) {
                userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
            }
            final MCRObject filteredObject = MCRORCIDUtils.filterObject(object);
            final Work work
                = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(filteredObject.createXML()));
            // do not trust user/work info
            doUpdateWorkInfo(MCRORCIDWorkUtils.listTrustedIdentifiers(work), userInfo.getWorkInfo(), orcid, credential);
            doDeleteWork(userInfo.getWorkInfo(), orcid, credential);
            MCRORCIDMetadataUtils.updateUserInfoByORCID(object, orcid, userInfo);
            MCRMetadataManager.update(object);
        } catch (Exception e) {
            throw new MCRORCIDException("Cannot remove work", e);
        }
    }

    /**
     * Deletes Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     * 
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDRequestException if the request fails
     * @throws MCRORCIDNotFoundException if specified Work does not exist
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     */
    protected static void doDeleteWork(MCRORCIDPutCodeInfo workInfo, String orcid, MCRORCIDCredential credential) {
        removeWork(workInfo.getOwnPutCode(), orcid, credential);
        workInfo.setOwnPutCode(0);
    }

    /**
     * Creates Work and updates MCRORCIDPutCodeInfo.
     * 
     * @param work the Work
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDRequestException if the request fails
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     */
    protected static void doCreateWork(Work work, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential) {
        workInfo.setOwnPutCode(createWork(work, orcid, credential));
    }

    /**
     * Updates Work and MCRORCIDPutCodeInfo.
     * 
     * @param putCode the putCode
     * @param work the Work
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDRequestException if the request fails
     * @throws MCRORCIDNotFoundException if specified Work does not exist
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     */
    protected static void doUpdateWork(long putCode, Work work, String orcid, MCRORCIDCredential credential) {
        final Work remoteWork = fetchWork(putCode, orcid, credential);
        if (MCRORCIDWorkUtils.checkWorkEquality(work, remoteWork)) {
            updateWork(work, orcid, credential, putCode);
        } else {
            LOGGER.info("Update of work of user {} with put code {} not required.", orcid, putCode);
        }
    }

    /**
     * Updates work info for MCRORCIDCredential with MCRIdentifier as reference.
     * 
     * @param identifiers the identifiers
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDException look up request fails
     * @see MCRORCIDUtils#checkTrustedIdentifier
     */
    protected static void doUpdateWorkInfo(Set<MCRIdentifier> identifiers, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential) {
        if (credential.getAccessToken() != null) {
            try {
                final List<WorkSummary> summaries = MCRORCIDClientHelper.getClientFactory()
                    .createUserClient(orcid, credential).fetch(MCRORCIDSectionImpl.WORKS, Works.class)
                    .getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
                MCRORCIDWorkSummaryUtils
                    .updateWorkInfoFromSummaries(identifiers, summaries, workInfo);
            } catch (MCRORCIDRequestException e) {
                throw new MCRORCIDException("Error during update", e);
            }
        } else {
            doUpdateWorkInfo(identifiers, workInfo, orcid);
        }
    }

    private static Work fetchWork(long putCode, String orcid, MCRORCIDCredential credential) {
        try {
            return MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential)
                .fetch(MCRORCIDSectionImpl.WORK, Work.class, putCode);
        } catch (MCRORCIDRequestException e) {
            if (Objects.equals(e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode())) {
                throw new MCRORCIDNotFoundException(e.getResponse());
            }
            throw e;
        }
    }

    private static void updateWork(Work work, String orcid, MCRORCIDCredential credential, long putCode) {
        checkScope(credential);
        try {
            final MCRORCIDUserClient memberClient
                = MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential);
            work.setPutCode(putCode);
            memberClient.update(MCRORCIDSectionImpl.WORK, putCode, work);
        } catch (MCRORCIDRequestException e) {
            if (Objects.equals(e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode())) {
                throw new MCRORCIDNotFoundException(e.getResponse());
            }
            throw e;
        }
    }

    private static long createWork(Work work, String orcid, MCRORCIDCredential credential) {
        checkScope(credential);
        return MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential)
            .create(MCRORCIDSectionImpl.WORK, work);
    }

    private static void removeWork(long putCode, String orcid, MCRORCIDCredential credential) {
        checkScope(credential);
        MCRORCIDClientHelper.getClientFactory().createUserClient(orcid, credential).delete(MCRORCIDSectionImpl.WORK,
            putCode);
    }

    private static void checkScope(MCRORCIDCredential credential) {
        if (credential.getScope() != null && !credential.getScope().contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            throw new MCRORCIDInvalidScopeException();
        }
    }

    private static List<String> getMatchingORCIDs(Set<MCRIdentifier> identifiers) {
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

    private static void collectOtherPutCodes(Set<MCRIdentifier> identifiers, List<String> orcids,
        MCRORCIDFlagContent flagContent) {
        orcids.forEach(orcid -> {
            try {
                final MCRORCIDUserInfo userInfo = Optional.ofNullable(flagContent.getUserInfoByORCID(orcid))
                    .orElseGet(() -> new MCRORCIDUserInfo(orcid));
                if (userInfo.getWorkInfo() == null) {
                    userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
                }
                doUpdateWorkInfo(identifiers, userInfo.getWorkInfo(), orcid);
                if (userInfo.getWorkInfo().getOwnPutCode() != 0) {
                    final long ownPutCode = userInfo.getWorkInfo().getOwnPutCode();
                    userInfo.getWorkInfo().addOtherPutCode(ownPutCode);
                    userInfo.getWorkInfo().setOwnPutCode(0);
                }
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (MCRORCIDException e) {
                LOGGER.warn("Could not collect put codes for {}.", orcid);
            }
        });
    }

    private static void doUpdateWorkInfo(Set<MCRIdentifier> identifiers, MCRORCIDPutCodeInfo workInfo, String orcid) {
        try {
            final List<WorkSummary> summaries = MCRORCIDClientHelper.getClientFactory().createReadClient()
                .fetch(orcid, MCRORCIDSectionImpl.WORKS, Works.class).getWorkGroup().stream()
                .flatMap(g -> g.getWorkSummary().stream()).toList();
            MCRORCIDWorkSummaryUtils.updateWorkInfoFromSummaries(identifiers, summaries, workInfo);
        } catch (MCRORCIDRequestException e) {
            throw new MCRORCIDException("Error during update", e);
        }
    }
}
