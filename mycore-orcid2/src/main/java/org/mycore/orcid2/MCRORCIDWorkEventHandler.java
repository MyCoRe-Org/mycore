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

package org.mycore.orcid2;

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
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.user2.MCRUser;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.client.exception.MCRORCIDNotFoundException;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.metadata.MCRORCIDFlagContent;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.metadata.MCRORCIDUserInfo;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserProperties;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.util.MCRIdentifier;
import org.orcid.jaxb.model.message.ScopeConstants;

/**
 * When a publication is created or updated locally in this application,
 * collects all orcid name identifiers from the MODS metadata,
 * looks up login users that have one of these identifiers stored in their user attributes,
 * checks if these users have an ORCID profile we know of
 * and have authorized us to update their profile as trusted party,
 * and then creates/updates the publication in the works section of that profile.
 */
public abstract class MCRORCIDWorkEventHandler<T> extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

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
        final MCRORCIDFlagContent flagContent = MCRORCIDMetadataUtils.getORCIDFlagContent(object);
        // TODO trust empty flag content?
        if (flagContent == null) {
            return;
        }
        final List<MCRORCIDUserInfo> userInfos = flagContent.getUserInfos();
        if (userInfos.isEmpty()) {
            return;
        }
        for (MCRORCIDUserInfo userInfo : userInfos) {
            final MCRORCIDPutCodeInfo workInfo = userInfo.getWorkInfo();
            if (workInfo != null && workInfo.hasOwnPutCode()) {
                final String orcid = userInfo.getORCID();
                try {
                    final MCRORCIDCredential credential = MCRORCIDUserUtils.getCredentialByORCID(orcid);
                    if (credential != null) {
                        removeWork(workInfo, orcid, credential);
                    } else {
                        LOGGER.info("Cannot remove work. There are no credentials for {}", orcid);
                    }
                } catch (MCRORCIDException e) {
                    LOGGER.error("Cannot remove work for {}", orcid, e);
                }
            }
        }
    }

    private void handlePublication(MCRObject object) {
        final MCRObjectID objectID = object.getId();
        LOGGER.info("Start publishing {} to ORCID.", objectID);
        if (!MCRMODSWrapper.isSupported(object)) {
            return;
        }
        if (!MCRORCIDUtils.checkPublishState(object)) {
            LOGGER.info("Object has wrong state. Skipping {}.", objectID);
            return;
        }
        final MCRORCIDFlagContent flagContent = Optional.ofNullable(MCRORCIDMetadataUtils.getORCIDFlagContent(object))
            .orElse(new MCRORCIDFlagContent());
        final Map<MCRORCIDUser, String> userOrcidPairFromFlag = listUserOrcidPairFromFlag(flagContent);
        final Map<MCRORCIDUser, String> userOrcidPairFromObject = listUserOrcidPairFromObject(object);
        if (userOrcidPairFromFlag.isEmpty() && userOrcidPairFromFlag.isEmpty()) {
            LOGGER.info("Nothing to do...", objectID);
            return;
        }

        final Map<MCRORCIDUser, String> toDelete = new HashMap(userOrcidPairFromFlag);
        toDelete.keySet().removeAll(userOrcidPairFromObject.keySet());
        if (!toDelete.isEmpty()) {
            deleteWorks(toDelete, flagContent);
        }

        final Map<MCRORCIDUser, String> toPublish = new HashMap(userOrcidPairFromFlag);
        toPublish.putAll(userOrcidPairFromObject);
        toPublish.keySet().removeAll(toDelete.keySet());
        if (!toPublish.isEmpty()) {
            publishWorks(toPublish, object, flagContent);
        }

        MCRORCIDMetadataUtils.setORCIDFlagContent(object, flagContent);
        LOGGER.info("Finished publishing {} to ORCID.", objectID);
    }

    private void deleteWorks(Map<MCRORCIDUser, String> userOrcidPair, MCRORCIDFlagContent flagContent) {
        for (Map.Entry<MCRORCIDUser, String> entry : userOrcidPair.entrySet()) {
            final MCRORCIDUser user = entry.getKey();
            final String orcid = entry.getValue();
            final MCRORCIDUserInfo userInfo = flagContent.getUserInfoByORCID(orcid);
            try {
                final MCRORCIDCredential credential = user.getCredentialByORCID(orcid);
                if (credential == null) {
                    // user is no longer an author and we no longer have credentials
                    continue;
                }
                removeWork(userInfo.getWorkInfo(), orcid, credential);
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (MCRORCIDNotFoundException e) {
                userInfo.getWorkInfo().setOwnPutCode(0);
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (Exception e) {
                LOGGER.warn("Error while deleting {}", userInfo.getWorkInfo().getOwnPutCode(), e);
            }
        }
    }

    private void publishWorks(Map<MCRORCIDUser, String> userOrcidPair, MCRObject object,
        MCRORCIDFlagContent flagContent) {
        final MCRObject filteredObject = MCRORCIDUtils.filterObject(object);
        if (!MCRORCIDUtils.checkEmptyMODS(filteredObject)) {
            throw new MCRORCIDException("Filtered MODS is empty.");
        }
        final T work = transformWork(filteredObject);
        for (Map.Entry<MCRORCIDUser, String> entry : userOrcidPair.entrySet()) {
            final MCRORCIDUser user = entry.getKey();
            final String orcid = entry.getValue();
            try {
                final MCRORCIDUserInfo userInfo = Optional.ofNullable(flagContent.getUserInfoByORCID(orcid))
                    .orElse(new MCRORCIDUserInfo(orcid));
                publish(work, user.getUserProperties(), userInfo, user.getCredentialByORCID(orcid));
                flagContent.updateUserInfoByORCID(orcid, userInfo);
            } catch (MCRORCIDNotFoundException e) {
                // TODO handle recreate
            } catch (Exception e) {
                LOGGER.warn("Error while publishing {} to ORCID:", object.getId(), e);
            }
        }
    }

    private void publish(T work, MCRORCIDUserProperties userProperties, MCRORCIDUserInfo userInfo,
        MCRORCIDCredential credential) {
        final String scope = credential.getScope();
        if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
            LOGGER.info("The scope is invalid. Skipping...");
        }
        if (userInfo.getWorkInfo() == null) {
            final MCRORCIDPutCodeInfo workInfo = new MCRORCIDPutCodeInfo();
            userInfo.setWorkInfo(workInfo);
            updateWorkInfo(work, userInfo.getWorkInfo(), userInfo.getORCID(), credential);
            if (workInfo.hasOwnPutCode()) {
                // there is an inconsistent state
                doUpdateWork(userInfo.getWorkInfo().getOwnPutCode(), work, userProperties, userInfo.getORCID(),
                    credential);
            } else if (workInfo.getOtherPutCodes() != null && userProperties.isCreateOwnDuplicate()) {
                // there is already work, a duplicate is allowed to be created
                createWork(work, userInfo.getWorkInfo(), userInfo.getORCID(), credential);
            } else if (workInfo.getOtherPutCodes() == null && userProperties.isCreateOwn()) {
                createWork(work, userInfo.getWorkInfo(), userInfo.getORCID(), credential);
            } else {
                throw new MCRORCIDException("Check user properties.");
            }
        } else if (userInfo.getWorkInfo().hasOwnPutCode()) {
            doUpdateWork(userInfo.getWorkInfo().getOwnPutCode(), work, userProperties, userInfo.getORCID(), credential);
        }
    }

    private void doUpdateWork(long putCode, T work, MCRORCIDUserProperties userProperties, String orcid,
        MCRORCIDCredential credential) {
        if (userProperties.isAlwaysUpdate()) {
            updateWork(putCode, work, orcid, credential);
        }
    }

    private Map<MCRORCIDUser, String> listUserOrcidPairFromFlag(MCRORCIDFlagContent flagContent) {
        if (flagContent.getUserInfos() == null) {
            return Collections.emptyMap();
        }
        final List<String> orcids = flagContent.getUserInfos().stream().filter(u -> u.getWorkInfo() != null)
            .filter(u -> u.getWorkInfo().hasOwnPutCode()).map(MCRORCIDUserInfo::getORCID).toList();
        final Map<MCRORCIDUser, String> userOrcidPair = new HashMap<>();
        for (String orcid : orcids) {
            try {
                userOrcidPair.put(MCRORCIDUserUtils.getORCIDUserByORCID(orcid), orcid);
            } catch (MCRORCIDException e) {
                LOGGER.warn(e);
            }
        }
        return userOrcidPair;
    }

    private Map<MCRORCIDUser, String> listUserOrcidPairFromObject(MCRObject object) {
        final Map<MCRORCIDUser, String> userOrcidPair = new HashMap<>();
        for (Element nameElement : MCRORCIDUtils.listNameElements(new MCRMODSWrapper(object))) {
            String orcid = null;
            final Set<MCRUser> users = new HashSet();
            for (MCRIdentifier id : listTrustedNameIdentifiers(nameElement)) {
                if (Objects.equals("orcid", id.getType())) {
                    orcid = id.getValue();
                }
                users.addAll(MCRORCIDUserUtils.getUserByID(id));
            }
            if (orcid != null && users.size() == 1) {
                userOrcidPair.put(new MCRORCIDUser(users.iterator().next()), orcid);
            } else if (orcid != null && users.size() > 1) {
                final List<MCRORCIDUser> orcidUsers = new ArrayList();
                for (MCRUser user : users) {
                    final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
                    if (orcidUser.hasCredential() && orcidUser.getCredentialByORCID(orcid) != null) {
                        orcidUsers.add(orcidUser);
                    }
                }
                if (orcidUsers.size() == 1) {
                    userOrcidPair.put(orcidUsers.get(0), orcid);
                } else if (orcidUsers.size() > 1) {
                    // TODO
                    LOGGER.warn("This case is not implemented");
                }
            } else if (orcid == null && users.size() == 1) {
                final MCRORCIDUser orcidUser = new MCRORCIDUser(users.iterator().next());
                final Map<String, MCRORCIDCredential> pairs = orcidUser.getCredentials();
                if (pairs.size() == 1) {
                    userOrcidPair.put(orcidUser, pairs.entrySet().iterator().next().getKey());
                } else if (pairs.size() > 1) {
                    LOGGER.info("Try to find credentials for {}, but found more than one pair",
                        users.iterator().next().getUserID());
                }
            } else if (orcid == null && users.size() > 1) {
                // TODO
                LOGGER.warn("This case is not implemented");
            }
        }
        return userOrcidPair;
    }

    private List<MCRIdentifier> listTrustedNameIdentifiers(Element nameElement) {
        return MCRORCIDUtils.getNameIdentifiers(nameElement).stream()
            .filter(i -> MCRORCIDUser.TRUSTED_NAME_IDENTIFIER_TYPES.contains(i.getType())).toList();
    }

    /**
     * Removes Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     * 
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
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
     */
    abstract protected void updateWork(long putCode, T work, String orcid, MCRORCIDCredential credential);

    /**
     * Creates Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     * 
     * @param work the Work
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     */
    abstract protected void createWork(T work, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential);

    /**
     * Updates Work in ORCID profile and updates MCRORCIDPutCodeInfo.
     * 
     * @param work the Work
     * @param workInfo the MCRORCIDPutCodeInfo
     * @param orcid the ORCID iD
     * @param credential the MCRORCIDCredential
     */
    abstract protected void updateWorkInfo(T work, MCRORCIDPutCodeInfo workInfo, String orcid,
        MCRORCIDCredential credential);

    /**
     * Transforms MCRObject to Work.
     * 
     * @param object the MCRObject
     * @param <T> the work type
     * @return the Work
     */
    abstract protected <T> T transformWork(MCRObject object);
}
