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

package org.mycore.ocfl.user;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.ocfl.MCROCFLObjectIDPrefixHelper;
import org.mycore.ocfl.MCROCFLRepositoryProvider;
import org.mycore.user2.MCRTransientUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.utils.MCRUserTransformer;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.OverwriteException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * XML Manager to handle MCRUsers in a MyCoRe OCFL Repository
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLUserManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String MESSAGE_CREATED = "Created";

    public static final String MESSAGE_UPDATED = "Updated";

    public static final String MESSAGE_DELETED = "Deleted";

    private static final String IGNORING_TRANSIENT_USER = "Got TransientUser, ignoring...";

    private String repositoryKey;
    
    private OcflRepository repository;

    /**
     * Initializes the UserManager
     */
    public MCROCFLXMLUserManager() {

    }

    /**
     * Initializes the UserManager with the given repositoryKey.
     * @param repositoryKey the ID for the repository to be used
     */
    public MCROCFLXMLUserManager(String repositoryKey) {
        initOCFLRepository(repositoryKey);
    }
    
    /**
     * initializes the OCFL repository with the given repositoryKey
     * 
     * @param respositoryKey
     */
    @MCRProperty(name = "OCFL.Repository")
    public void initOCFLRepository(String respositoryKey) {
        this.repositoryKey = respositoryKey;
        repository = MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }
    
    public OcflRepository getRepository() {
        return repository;
    }

    public void updateUser(MCRUser user) {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + user.getUserID();

        /*
         * On every Login, a update event gets called to update the "lastLogin" of a user.
         * This would create many unnecessary versions/copies every time someone logs in.
         * To prevent this, the update event gets ignored if the current user is a guest.
         * Usually guests do not have rights to modify users, so the only time they trigger this event
         * is during the update of "lastLogin", since the user switch has not happened yet.
         */
        if (MCRSystemUserInformation.getGuestInstance().getUserID().equals(currentUser.getUserID())) {
            LOGGER.debug("Login Detected, ignoring...");
            return;
        }

        // Transient users are not MyCoRe managed users, but rather from external sources
        if (user instanceof MCRTransientUser) {
            LOGGER.debug(IGNORING_TRANSIENT_USER);
            return;
        }

        if (!exists(ocflUserID)) {
            createUser(user);
            return;
        }

        VersionInfo info = new VersionInfo()
            .setMessage(MESSAGE_UPDATED)
            .setCreated(OffsetDateTime.now(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), buildEmail(currentUser));
        MCRJDOMContent content = new MCRJDOMContent(MCRUserTransformer.buildExportableXML(user));
        try (InputStream userAsStream = content.getInputStream()) {
            repository.updateObject(ObjectVersionId.head(ocflUserID), info,
                updater -> {
                    updater.writeFile(userAsStream, user.getUserID() + ".xml", OcflOption.OVERWRITE);
                });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to update user '" + ocflUserID + "'", e);
        }
    }

    public void createUser(MCRUser user) {
        if (user instanceof MCRTransientUser) {
            LOGGER.debug(IGNORING_TRANSIENT_USER);
            return;
        }

        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + user.getUserID();

        if (exists(ocflUserID)) {
            throw new MCRUsageException("The User '" + user.getUserID() + "' already exists in OCFL Repository");
        }

        VersionInfo info = new VersionInfo()
            .setMessage(MESSAGE_CREATED)
            .setCreated(OffsetDateTime.now(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), buildEmail(currentUser));
        MCRJDOMContent content = new MCRJDOMContent(MCRUserTransformer.buildExportableXML(user));
        try (InputStream userAsStream = content.getInputStream()) {
            repository.updateObject(ObjectVersionId.head(ocflUserID), info,
                updater -> {
                    updater.writeFile(userAsStream, user.getUserID() + ".xml");
                });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to update user '" + ocflUserID + "'", e);
        }
    }

    public void deleteUser(MCRUser user) {
        if (user instanceof MCRTransientUser) {
            LOGGER.debug(IGNORING_TRANSIENT_USER);
            return;
        }
        deleteUser(user.getUserID());
    }

    public void deleteUser(String userId) {
        MCRUser currentUser = MCRUserManager.getCurrentUser();
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + userId;

        if (!exists(ocflUserID)) {
            throw new MCRUsageException(
                "The User '" + userId + "' does not exist or has already been deleted!");
        }

        VersionInfo info = new VersionInfo()
            .setMessage(MESSAGE_DELETED)
            .setCreated(OffsetDateTime.now(ZoneOffset.UTC))
            .setUser(currentUser.getUserName(), buildEmail(currentUser));
        repository.updateObject(ObjectVersionId.head(ocflUserID), info,
            updater -> {
                updater.removeFile(userId + ".xml");
            });
    }

    private String buildEmail(MCRUser currentUser) {
        return Optional.ofNullable(currentUser.getEMailAddress()).map(email -> "mailto:" + email).orElse(null);
    }

    /**
     * Retrieve a MCRUser from the ocfl store.
     * @param userId the userId of the requested user
     * @param revision the version in ocfl store or <code>null</code> for latest
     * @return the requested MCRUser
     * @throws IOException if a error occurs during retrieval
     */
    public MCRUser retrieveContent(String userId, String revision) throws IOException {
        String ocflUserID = MCROCFLObjectIDPrefixHelper.USER + userId;

        if (!repository.containsObject(ocflUserID)) {
            throw new MCRUsageException("The User '" + ocflUserID + "' does not exist!");
        }

        ObjectVersionId version = revision == null ? ObjectVersionId.head(ocflUserID)
            : ObjectVersionId.version(ocflUserID, revision);

        if (isDeleted(version)) {
            throw new MCRUsageException("The User '" + ocflUserID + "' with version '" + revision
                + "' has been deleted!");
        }

        try (InputStream storedContentStream = repository.getObject(version).getFile(userId + ".xml").getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            return MCRUserTransformer.buildMCRUser(xml.getRootElement());
        } catch (JDOMException | IOException | SAXException e) {
            throw new IOException("Can not parse XML from OCFL-Store", e);
        }
    }

    private boolean isDeleted(ObjectVersionId version) {
        return MESSAGE_DELETED.equals(repository.describeVersion(version).getVersionInfo().getMessage());
    }

    boolean exists(String ocflUserID) {
        return repository.containsObject(ocflUserID)
            && !isDeleted(ObjectVersionId.head(ocflUserID));
    }

    boolean exists(String ocflUserID, String revision) {
        return repository.containsObject(ocflUserID)
            && !isDeleted(ObjectVersionId.version(ocflUserID, revision));
    }
}
