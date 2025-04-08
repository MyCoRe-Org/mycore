/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.orcid2.work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDInvalidScopeException;
import org.mycore.orcid2.client.exception.MCRORCIDNotFoundException;
import org.mycore.orcid2.client.exception.MCRORCIDRequestException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.metadata.MCRORCIDFlagContent;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserProperties;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;
import org.orcid.jaxb.model.message.ScopeConstants;

/**
 * When a publication is created or updated locally in this application,
 * collects all ORCID name identifiers from the MODS metadata,
 * looks up login users that have one of these identifiers stored in their user attributes,
 * checks if these users have an ORCID profile we know of
 * and have authorized us to update their profile as trusted party,
 * and then creates/updates the publication in the works section of that profile.
 */
public abstract class MCRORCIDWorkEventHandler<T> extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String WORK_EVENT_HANDLER_PROPERTY_PREFIX = "MCR.ORCID2.WorkEventHandler.";

    private static final boolean COLLECT_EXTERNAL_PUT_CODES = MCRConfiguration2
        .getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "CollectExternalPutCodes").orElse(false);

    private static final boolean CREATE_FIRST = MCRConfiguration2
        .getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "CreateFirstWork").orElse(false);

    private static final boolean ALWAYS_UPDATE = MCRConfiguration2
        .getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "AlwaysUpdateWork").orElse(false);

    private static final boolean CREATE_OWN_DUPLICATE = MCRConfiguration2
        .getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "CreateDuplicateWork").orElse(false);

    private static final boolean RECREATE_DELETED = MCRConfiguration2
        .getBoolean(WORK_EVENT_HANDLER_PROPERTY_PREFIX + "RecreateDeletedWork").orElse(false);

    private static final boolean SAVE_OTHER_PUT_CODES = MCRConfiguration2
        .getBoolean("MCR.ORCID2.Metadata.WorkInfo.SaveOtherPutCodes").orElse(false);

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject object) {
        handlePublication(object);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject object) {
        handlePublication(object);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject object) {
        final MCRObjectID objectID = object.getId();
        LOGGER.info("Start deleting {} to ORCID.", objectID);
        if (!MCRMODSWrapper.isSupported(object)) {
            return;
        }
        try {
            final T work = transformObject(new MCRJDOMContent(object.createXML()));
            final Set<MCRIdentifier> identifiers = listTrustedIdentifiers(work);
            final MCRORCIDFlagContent flagContent = Optional
                .ofNullable(MCRORCIDMetadataUtils.getORCIDFlagContent(object)).orElse(new MCRORCIDFlagContent());
            final Map<String, MCRORCIDUser> toDelete = listUserOrcidPairFromFlag(flagContent);
            toDelete.putAll(listUserOrcidPairFromObject(object));
            deleteWorks(toDelete, identifiers, flagContent);
            LOGGER.info("Finished deleting {} from ORCID successfully.", objectID);
        } catch (Exception e) {
            LOGGER.warn("Error while deleting object {}.", objectID, e);
        }
    }

    @SuppressWarnings("PMD.NPathComplexity")
    private void handlePublication(MCRObject object) {
        final MCRObjectID objectID = object.getId();
        LOGGER.info("Start publishing {} to ORCID.", objectID);
        if (!MCRMODSWrapper.isSupported(object)) {
            LOGGER.info("MODS object is required. Skipping {}...", objectID);
            return;
        }
        MCRObject filteredObject;
        try {
            filteredObject = MCRORCIDUtils.filterObject(object);
        } catch (MCRORCIDException e) {
            LOGGER.warn("Error while filtering {}.", objectID);
            return;
        }
        if (!MCRORCIDUtils.checkEmptyMODS(filteredObject)) {
            LOGGER.info("Filtered MODS is empty. Skipping {}...", objectID);
            return;
        }
        if (!MCRORCIDUtils.checkPublishState(object)) {
            LOGGER.info("Object has wrong state. Skipping {}...", objectID);
            tryCollectAndSaveExternalPutCodes(filteredObject);
            return;
        }
        if (MCRMetadataManager.exists(objectID)) {
            final MCRObject outdatedObject = MCRMetadataManager.retrieveMCRObject(objectID);
            if (MCRXMLHelper.deepEqual(new MCRMODSWrapper(object).getMODS(),
                new MCRMODSWrapper(outdatedObject).getMODS())) {
                LOGGER.info("Metadata does not changed. Skipping {}...", objectID);
                tryCollectAndSaveExternalPutCodes(filteredObject);
                return;
            }
        }
        final MCRORCIDFlagContent flagContent = Optional.ofNullable(MCRORCIDMetadataUtils.getORCIDFlagContent(object))
            .orElse(new MCRORCIDFlagContent());
        final Map<String, MCRORCIDUser> userOrcidPairFromFlag = listUserOrcidPairFromFlag(flagContent);
        final Map<String, MCRORCIDUser> userOrcidPairFromObject = listUserOrcidPairFromObject(object);
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, MCRORCIDUser> toDelete = new HashMap<>(userOrcidPairFromFlag);
        toDelete.keySet().removeAll(userOrcidPairFromObject.keySet());
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, MCRORCIDUser> toPublish = new HashMap<>(userOrcidPairFromFlag);
        toPublish.putAll(userOrcidPairFromObject);
        toPublish.keySet().removeAll(toDelete.keySet());
        if (toDelete.isEmpty() && toPublish.isEmpty()) {
            LOGGER.info("Nothing to delete or publish. Skipping {}...", objectID);
            tryCollectAndSaveExternalPutCodes(filteredObject);
            return;
        }
        try {
            final T work = transformObject(new MCRJDOMContent(filteredObject.createXML()));
            final Set<MCRIdentifier> identifiers = listTrustedIdentifiers(work);
            if (!toDelete.isEmpty()) {
                deleteWorks(toDelete, identifiers, flagContent);
            }
            final Set<String> successfulPublished = new HashSet<>();
            if (!toPublish.isEmpty()) {
                publishWorks(toPublish, identifiers, work, flagContent, successfulPublished);
            }
            if (COLLECT_EXTERNAL_PUT_CODES && SAVE_OTHER_PUT_CODES) {
                collectExternalPutCodes(flagContent, identifiers, successfulPublished);
            }
            LOGGER.info("Finished publishing {} to ORCID successfully.", objectID);
        } catch (Exception e) {
            LOGGER.warn("Error while publishing {} to ORCID.", objectID, e);
        } finally {
            try {
                MCRORCIDMetadataUtils.setORCIDFlagContent(object, flagContent);
            } catch (MCRORCIDException e) {
                LOGGER.warn("Error while setting ORCID flag content to {}.", objectID, e);
            }
        }
    }

    private void deleteWorks(Map<String, MCRORCIDUser> userOrcidPair, Set<MCRIdentifier> identifiers,
        MCRORCIDFlagContent flagContent) {
        for (Map.Entry<String, MCRORCIDUser> entry : userOrcidPair.entrySet()) {
            final String orcid = entry.getKey();
            final MCRORCIDUser user = entry.getValue();
            final MCRORCIDUserInfo userInfo = Optional.ofNullable(flagContent.getUserInfoByORCID(orcid))
                .orElse(new MCRORCIDUserInfo(orcid));
            if (userInfo.getWorkInfo() == null) {
                userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
            }
            try {
                final MCRORCIDCredential credential = user.getCredentialByORCID(orcid);
                if (credential == null) {
                    // user is no longer an author and we no longer have credentials
                    continue;
                }
                updateWorkInfo(identifiers, userInfo.getWorkInfo(), orcid, credential);
                if (userInfo.getWorkInfo().hasOwnPutCode()) {
                    removeWork(userInfo.getWorkInfo(), orcid, credential);
                    flagContent.updateUserInfoByORCID(orcid, userInfo);
                }
            } catch (MCRORCIDNotFoundException e) {
                userInfo.getWorkInfo().setOwnPutCode(-1);
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (Exception e) {
                LOGGER.warn(() -> "Error while deleting " + userInfo.getWorkInfo().getOwnPutCode(), e);
            }
        }
    }

    private void publishWorks(Map<String, MCRORCIDUser> userOrcidPair, Set<MCRIdentifier> identifiers, T work,
        MCRORCIDFlagContent flagContent, Set<String> successful) {
        for (Map.Entry<String, MCRORCIDUser> entry : userOrcidPair.entrySet()) {
            final String orcid = entry.getKey();
            final MCRORCIDUser user = entry.getValue();
            try {
                final MCRORCIDCredential credential = user.getCredentialByORCID(orcid);
                if (credential == null) {
                    continue;
                }
                final String scope = credential.getScope();
                if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
                    LOGGER.info("The scope is invalid. Skipping...");
                    continue;
                }
                final MCRORCIDUserInfo userInfo = Optional.ofNullable(flagContent.getUserInfoByORCID(orcid))
                    .orElse(new MCRORCIDUserInfo(orcid));
                if (userInfo.getWorkInfo() == null) {
                    userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
                }
                updateWorkInfo(identifiers, userInfo.getWorkInfo(), orcid, credential);
                final MCRORCIDUserProperties userProperties = user.getUserPropertiesByORCID(orcid);
                fixUserProperties(userProperties);
                publishWork(work, userProperties, userInfo.getWorkInfo(), orcid, credential);
                successful.add(orcid);
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (Exception e) {
                LOGGER.warn("Error while publishing Work to {}", orcid, e);
            }
        }
    }

    private void fixUserProperties(MCRORCIDUserProperties userProperties) {
        if (userProperties.isCreateFirstWork() == null) {
            userProperties.setCreateFirstWork(CREATE_FIRST);
        }
        if (userProperties.isAlwaysUpdateWork() == null) {
            userProperties.setAlwaysUpdateWork(ALWAYS_UPDATE);
        }
        if (userProperties.isCreateDuplicateWork() == null) {
            userProperties.setCreateDuplicateWork(CREATE_OWN_DUPLICATE);
        }
        if (userProperties.isRecreateDeletedWork() == null) {
            userProperties.setRecreateDeleted(RECREATE_DELETED);
        }
    }

    private void publishWork(T work, MCRORCIDUserProperties userProperties, MCRORCIDPutCodeInfo workInfo,
        String orcid, MCRORCIDCredential credential) {
        if (workInfo.hasOwnPutCode()) {
            if (userProperties.isAlwaysUpdateWork()) {
                try {
                    updateWork(workInfo.getOwnPutCode(), work, orcid, credential);
                } catch (MCRORCIDNotFoundException e) {
                    if (userProperties.isRecreateDeletedWork()) {
                        createWork(work, workInfo, orcid, credential);
                    }
                }
            }
        } else if (workInfo.hadOwnPutCode()) {
            if (userProperties.isRecreateDeletedWork()) {
                createWork(work, workInfo, orcid, credential);
            }
        } else if (workInfo.hasOtherPutCodes()) {
            if (userProperties.isCreateDuplicateWork()) {
                createWork(work, workInfo, orcid, credential);
            }
        } else if (userProperties.isCreateFirstWork()) {
            createWork(work, workInfo, orcid, credential);
        }
    }

    private Map<String, MCRORCIDUser> listUserOrcidPairFromFlag(MCRORCIDFlagContent flagContent) {
        if (flagContent.getUserInfos() == null) {
            return new HashMap<>();
        }
        final List<String> orcids = flagContent.getUserInfos().stream().filter(u -> u.getWorkInfo() != null)
            .filter(u -> u.getWorkInfo().hasOwnPutCode()).map(MCRORCIDUserInfo::getORCID).toList();
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, MCRORCIDUser> userOrcidPair = new HashMap<>();
        for (String orcid : orcids) {
            try {
                userOrcidPair.put(orcid, MCRORCIDUserUtils.getORCIDUserByORCID(orcid));
            } catch (MCRORCIDException e) {
                LOGGER.warn(e);
            }
        }
        return Collections.unmodifiableMap(userOrcidPair);
    }

    private Map<String, MCRORCIDUser> listUserOrcidPairFromObject(MCRObject object) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        final Map<String, MCRORCIDUser> userOrcidPair = new HashMap<>();
        for (Element nameElement : MCRORCIDUtils.listNameElements(new MCRMODSWrapper(object))) {
            String orcid = null;
            final Set<MCRUser> users = new HashSet<>();
            for (MCRIdentifier id : listTrustedNameIdentifiers(nameElement)) {
                if (Objects.equals(id.getType(), "orcid")) {
                    orcid = id.getValue();
                }
                users.addAll(MCRORCIDUserUtils.getUserByID(id));
            }
            updateOrcidPair(orcid, users, userOrcidPair);
        }
        return Collections.unmodifiableMap(userOrcidPair);
    }

    private void updateOrcidPair(String orcid, Set<MCRUser> users, Map<String, MCRORCIDUser> userOrcidPair) {
        if (orcid != null && users.size() == 1) {
            userOrcidPair.put(orcid, new MCRORCIDUser(users.iterator().next()));
        } else if (orcid != null && users.size() > 1) {
            final List<MCRORCIDUser> orcidUsers = new ArrayList<>();
            for (MCRUser user : users) {
                final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
                if (orcidUser.hasCredential(orcid)) {
                    orcidUsers.add(orcidUser);
                }
            }
            if (orcidUsers.size() == 1) {
                userOrcidPair.put(orcid, orcidUsers.getFirst());
            } else if (orcidUsers.size() > 1) {
                LOGGER.warn("{}: Multiple users found. This case is not implemented.", orcid);
            }
        } else if (orcid == null && users.size() == 1) {
            final MCRORCIDUser orcidUser = new MCRORCIDUser(users.iterator().next());
            final Map<String, MCRORCIDCredential> pairs = orcidUser.getCredentials();
            if (pairs.size() == 1) {
                userOrcidPair.put(pairs.entrySet().iterator().next().getKey(), orcidUser);
            } else if (pairs.size() > 1) {
                LOGGER.info("Try to find credentials for {}, but found more than one pair",
                    () -> users.iterator().next().getUserID());
            }
        } else if (orcid == null && users.size() > 1) {
            LOGGER.warn("Unknown orcid and multiple users found. This case is not implemented.");
        }
    }

    private void tryCollectAndSaveExternalPutCodes(MCRObject object) {
        if (COLLECT_EXTERNAL_PUT_CODES && SAVE_OTHER_PUT_CODES) {
            try {
                collectAndSaveExternalPutCodes(object);
            } catch (Exception e) {
                LOGGER.warn(() -> "Error while collecting external put codes for {}." + object.getId(), e);
            }
        }
    }

    private void collectAndSaveExternalPutCodes(MCRObject object) {
        final MCRORCIDFlagContent flagContent = Optional.ofNullable(MCRORCIDMetadataUtils
            .getORCIDFlagContent(object)).orElse(new MCRORCIDFlagContent());
        final T work = transformObject(new MCRJDOMContent(object.createXML()));
        final Set<MCRIdentifier> identifiers = listTrustedIdentifiers(work);
        collectExternalPutCodes(flagContent, identifiers, Collections.emptySet());
        MCRORCIDMetadataUtils.setORCIDFlagContent(object, flagContent);
    }

    private void collectExternalPutCodes(MCRORCIDFlagContent flagContent, Set<MCRIdentifier> identifiers,
        Set<String> excludeORCIDs) {
        final Set<String> matchingOrcids = findMatchingORCIDs(identifiers);
        matchingOrcids.removeAll(excludeORCIDs);
        collectOtherPutCodes(identifiers, new ArrayList<>(matchingOrcids), flagContent);
    }

    private void collectOtherPutCodes(Set<MCRIdentifier> identifiers, List<String> orcids,
        MCRORCIDFlagContent flagContent) {
        orcids.forEach(orcid -> {
            try {
                final MCRORCIDUserInfo userInfo = Optional.ofNullable(flagContent.getUserInfoByORCID(orcid))
                    .orElse(new MCRORCIDUserInfo(orcid));
                final MCRORCIDPutCodeInfo workInfo = userInfo.getWorkInfo();
                if (workInfo == null) {
                    userInfo.setWorkInfo(new MCRORCIDPutCodeInfo());
                    updateWorkInfo(identifiers, userInfo.getWorkInfo(), orcid);
                } else {
                    updateWorkInfo(identifiers, workInfo, orcid);
                    userInfo.getWorkInfo().setOtherPutCodes(workInfo.getOtherPutCodes());
                }
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (MCRORCIDException e) {
                LOGGER.warn("Could not collect put codes for {}.", orcid);
            }
        });
    }

    private List<MCRIdentifier> listTrustedNameIdentifiers(Element nameElement) {
        return MCRORCIDUtils.getNameIdentifiers(nameElement).stream()
            .filter(i -> MCRORCIDUser.TRUSTED_NAME_IDENTIFIER_TYPES.contains(i.getType())).toList();
    }

    /**
     * Lists trusted identifiers as Set of MCRIdentifier.
     *
     * @param work the Work
     * @return Set of MCRIdentifier
     */
    abstract protected Set<MCRIdentifier> listTrustedIdentifiers(T work);

    /**
     * Lists matching ORCID iDs based on search via MCRIdentifier
     *
     * @param identifiers the MCRIdentifiers
     * @return Set of ORCID iDs as String
     * @throws MCRORCIDException if request fails
     */
    abstract protected Set<String> findMatchingORCIDs(Set<MCRIdentifier> identifiers);

    /**
     * Removes Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     *
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     * @throws MCRORCIDRequestException if the request fails
     * @throws MCRORCIDNotFoundException if specified Work does not exist
     */
    abstract protected void removeWork(MCRORCIDPutCodeInfo workInfo, String orcid, MCRORCIDCredential credential);

    /**
     * Updates Work in ORCID profile.
     *
     * @param putCode the put code
     * @param work the Work
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     * @throws MCRORCIDRequestException if the request fails
     * @throws MCRORCIDNotFoundException if specified Work does not exist
     */
    abstract protected void updateWork(long putCode, T work, String orcid, MCRORCIDCredential credential);

    /**
     * Creates Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     *
     * @param work the Work
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDInvalidScopeException if scope is invalid
     * @throws MCRORCIDRequestException if the request fails
     */
    abstract protected void createWork(T work, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential);

    /**
     * Updates work info based on MCRIdentifier.
     *
     * @param identifiers the MCRIdentifier
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     * @throws MCRORCIDException look up request fails
     */
    abstract protected void updateWorkInfo(Set<MCRIdentifier> identifiers, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential);

    /**
     * Updates work info based on MCRIdentifier.
     *
     * @param identifiers the MCRIdentifier
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @throws MCRORCIDException look up request fails
     */
    abstract protected void updateWorkInfo(Set<MCRIdentifier> identifiers, MCRORCIDPutCodeInfo workInfo, String orcid);

    /**
     * Transforms MCRObject as MCRJDOMContent to Work.
     *
     * @param object the MCRObject
     * @return the Work
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    abstract protected <T> T transformObject(MCRJDOMContent object);
}
