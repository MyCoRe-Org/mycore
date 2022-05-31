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

package org.mycore.ocfl;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.impl.MCRCategoryImpl;
import org.mycore.datamodel.classifications2.utils.MCRCategoryTransformer;
import org.mycore.datamodel.common.MCRXMLClassificationManager;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.MutableOcflRepository;
import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.LockException;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.VersionInfo;

/**
 * OCFL File Manager for MyCoRe Classifications
 * @author Tobias Lenhardt [Hammer1279]
 */
public class MCROCFLXMLClassificationManager implements MCRXMLClassificationManager {

    private static final Logger LOGGER = LogManager.getLogger(MCROCFLXMLClassificationManager.class);

    protected static final String CLASSIFICATION_PREFIX = "mcrclass:";

    // TODO replace this with a setting
    private static final String Q_DATA = "classQueue";

    // include the "/classification" directory
    private static final boolean INC_CLSDIR = MCRConfiguration2.getBoolean("MCR.OCFL.CM.UseClassSubDir").orElse(false);

    private static final boolean USE_COUNTER = MCRConfiguration2.getBoolean("MCR.OCFL.Classification.Counter")
        .orElse(false);

    private String rootFolder = INC_CLSDIR ? "classification/" : "";

    @MCRProperty(name = "Repository", required = true)
    public String repositoryKey;

    protected static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MCREvent.CREATE_EVENT, MCROCFLMetadataVersion.CREATED),
        Map.entry(MCREvent.UPDATE_EVENT, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MCREvent.DELETE_EVENT, MCROCFLMetadataVersion.DELETED),
        Map.entry(MCREvent.REPAIR_EVENT, MCROCFLMetadataVersion.REPAIRED),
        Map.entry("Auto-generated empty object version.", MCROCFLMetadataVersion.INITIALIZED)));

    protected static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    protected MutableOcflRepository getRepository() throws ClassCastException {
        return (MutableOcflRepository) MCROCFLRepositoryProvider.getRepository(repositoryKey);
    }

    protected boolean isMutable() {
        try {
            getRepository();
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    protected boolean isClean(MCRCategoryID mcrid) {
        return !getRepository().hasStagedChanges(getName(mcrid));
    }

    public void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        fileUpdate(mcrid, mcrCg, xml, xml, eventData);
    }

    public void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent xml, MCREvent eventData) {
        fileDelete(mcrid, mcrCg, xml, xml, eventData);
    }

    public void commitSession(MCRSession session) {
        commitSession(Optional.ofNullable(session));
    }

    public void rollbackSession(MCRSession session) {
        rollbackSession(Optional.ofNullable(session));
    }

    @SuppressWarnings("unchecked")
    public void rollbackSession(Optional<MCRSession> sessionOpt) {
        MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
        ArrayList<MCREvent> list = (ArrayList<MCREvent>) session.get(Q_DATA);
        if (list == null) {
            LogManager.getLogger(MCRXMLClassificationManager.class).error("List is empty!");
            return;
        }
        list.forEach(this::dropChanges);
        session.deleteObject(Q_DATA);
    }

    public void fileUpdate(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData) {

        String objName = getName(mcrid);
        String message = eventData.getEventType();
        Date lastModified = new Date();
        MCRContent xml = mcrCg.isClassification() ? clXml : cgXml;
        lastModified = new Date(MCRCategoryDAOFactory.getInstance().getLastModified(mcrid.getRootID()));

        if (MCROCFLCategoryIDLockTable.isLocked(mcrid)
            && !MCROCFLCategoryIDLockTable.isLockedByCurrentSession(mcrid)) {
            throw new LockException("The Object <" + mcrid + "> is currently locked by <"
                + MCROCFLCategoryIDLockTable.getLockingUserName(mcrid.getID()) + ">");
        }
        MCROCFLCategoryIDLockTable.lock(mcrid);

        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(message, lastModified);
            getRepository().stageChanges(ObjectVersionId.head(objName), versionInfo, updater -> {
                updater.writeFile(objectAsStream, buildFilePath(mcrCg), OcflOption.OVERWRITE);
            });
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        } catch (IllegalArgumentException e) {
            // Issue seems to be with the OCFL Library trying to find a file it deleted itself
            // LOGGER.warn("IllegalArgumentException happened, Ignoring...");

            // dropChanges(mcrid);
            // LOGGER.error("Something has gone Wrong!", e);
        }

    }

    public void fileDelete(MCRCategoryID mcrid, MCRCategory mcrCg, MCRContent clXml, MCRContent cgXml,
        MCREvent eventData) {
        String objName = getName(mcrid);
        String message = eventData.getEventType();
        Date lastModified = new Date();
        lastModified = new Date(MCRCategoryDAOFactory.getInstance().getLastModified(mcrid.getRootID()));
        if (MCROCFLCategoryIDLockTable.isLocked(mcrid)
            && !MCROCFLCategoryIDLockTable.isLockedByCurrentSession(mcrid)) {
            throw new LockException("The Object <" + mcrid + "> is currently locked by <"
                + MCROCFLCategoryIDLockTable.getLockingUserName(mcrid.getID()) + ">");
        }
        MCROCFLCategoryIDLockTable.lock(mcrid);
        VersionInfo versionInfo = buildVersionInfo(message, lastModified);
        getRepository().stageChanges(ObjectVersionId.head(objName), versionInfo, updater -> {
            updater.removeFile(buildFilePath(mcrCg));
        });
    }

    public void fileMove(Map<String, Object> data, MCREvent eventData) {
        MCRCategoryImpl oldParent = (MCRCategoryImpl) data.get(MCROCFLEventDataTypes.CATEGORY);
        MCRCategoryImpl newParent = (MCRCategoryImpl) eventData.get("parent");
        if (!(MCROCFLCategoryIDLockTable.isLockedByCurrentSession(oldParent.getId())
            || !MCROCFLCategoryIDLockTable.isLocked(oldParent.getId()))
            && (MCROCFLCategoryIDLockTable.isLockedByCurrentSession(newParent.getId())
                || !MCROCFLCategoryIDLockTable.isLocked(newParent.getId()))) {
            throw new LockException("The Object <" + oldParent.getId() + "> or <" + newParent.getId()
                + "> is currently locked by <" + MCROCFLCategoryIDLockTable.getLockingUserName(oldParent.getId().getRootID())
                + "> and <" + MCROCFLCategoryIDLockTable.getLockingUserName(newParent.getId().getRootID()) + ">");
        }
        MCROCFLCategoryIDLockTable.lock(oldParent.getId());
        MCROCFLCategoryIDLockTable.lock(newParent.getId());
        int index = (int) eventData.get("index");
        MCRCategoryImpl mcrCg = (MCRCategoryImpl) data.get(MCROCFLEventDataTypes.CATEGORY);
        fileDelete(mcrCg.getId(), mcrCg, (MCRContent) data.get(MCROCFLEventDataTypes.CATEGORY_ROOT_CONTENT), eventData);
        mcrCg.setParent(newParent);
        List<MCRCategory> children = newParent.getChildren();
        children.remove(mcrCg);
        children.add(index, mcrCg);
        MCRContent newParentXml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataElement(newParent, USE_COUNTER));
        MCRContent oldParentXml = new MCRJDOMContent(MCRCategoryTransformer.getMetaDataElement(oldParent, USE_COUNTER));
        fileUpdate(newParent.getId(), newParent, newParentXml, eventData);
        fileUpdate(oldParent.getId(), oldParent, oldParentXml, eventData);
    }

    public void commitChanges(MCREvent evt) {
        Map<String, Object> data = MCROCFLEventHandler.getEventData(evt);
        MCRCategoryID mcrid = (MCRCategoryID) data.get(MCROCFLEventDataTypes.CATEGORY_ID);
        MCRCategory mcrCg = (MCRCategory) data.get(MCROCFLEventDataTypes.CATEGORY);
        MCRContent rtXml = (MCRContent) data.get(MCROCFLEventDataTypes.CATEGORY_ROOT_CONTENT); // class
        Date lastModified = new Date(MCRCategoryDAOFactory.getInstance().getLastModified(mcrid.getRootID()));
        if (mcrCg.isCategory()) {
            fileUpdate(mcrCg.getRoot().getId(), mcrCg.getRoot(), rtXml, evt);
        }
        LOGGER.debug("Committing <{}> in Object <{}>", mcrid.getID(), mcrid.getRootID());
        VersionInfo versionInfo = buildVersionInfo(evt.getEventType(), lastModified);
        getRepository().commitStagedChanges(getName(mcrid), versionInfo);
        MCROCFLCategoryIDLockTable.unlock(mcrid);
    }

    @SuppressWarnings("unchecked")
    public void commitSession(Optional<MCRSession> sessionOpt) {
        MCRSession session = sessionOpt.orElse(MCRSessionMgr.getCurrentSession());
        ArrayList<MCREvent> list = (ArrayList<MCREvent>) session.get(Q_DATA);
        list.forEach(this::commitChanges);
        session.deleteObject(Q_DATA);
    }

    public void dropChanges(MCREvent evt) {
        dropChanges((MCRCategoryID) MCROCFLEventHandler.getEventData(evt).get(MCROCFLEventDataTypes.CATEGORY_ID));
    }

    public void dropChanges(MCREvent evt, Map<String, Object> data) {
        MCRCategoryID mcrid = (MCRCategoryID) data.get(MCROCFLEventDataTypes.CATEGORY_ID);
        dropChanges(mcrid);
    }

    public void dropChanges(MCRCategoryID mcrid) {
        if (getRepository().hasStagedChanges(getName(mcrid))) {
            getRepository().purgeStagedChanges(getName(mcrid));
            LOGGER.debug("Dropped changes of {}", getName(mcrid));
            MCROCFLCategoryIDLockTable.unlock(mcrid);
        } else {
            LOGGER.debug("No changes to Drop for {}", getName(mcrid));
            MCROCFLCategoryIDLockTable.unlock(mcrid);
        }
    }

    /**
     * Undoes an Action after a Failed Event
     * @param data {@link MCROCFLEventHandler#getEventData(MCREvent, boolean)}
     * @param evt {@link MCREvent} | {@link MCREvent}
     */
    public void undoAction(Map<String, Object> data, MCREvent evt) {
        dropChanges(evt, data);
    }

    /**
     * Load a Classification from the OCFL Store.
     * @param mcrid ID of the Category
     * @param revision Revision of the Category
     * @return Content of the Classification
     */
    public MCRContent retrieveContent(MCRCategoryID mcrid, String revision) {
        String objName = getName(mcrid);
        OcflRepository repo = getRepository();
        ObjectVersionId vId = revision != null ? ObjectVersionId.version(objName, revision)
            : ObjectVersionId.head(objName);

        try {
            repo.getObject(vId);
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }

        if (convertMessageToType(repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }

        if (convertMessageToType(
            repo.getObject(vId).getVersionInfo().getMessage()) == MCROCFLMetadataVersion.INITIALIZED) {
            throw new MCRUsageException("'" + objName + "' does not have any content yet");
        }

        try (InputStream storedContentStream = repo.getObject(vId).getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            if (revision != null) {
                xml.getRootElement().setAttribute("rev", revision);
            }
            return new MCRJDOMContent(xml);
        } catch (JDOMException | SAXException | IOException e) {
            throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        }
    }

    protected String getName(MCRCategoryID mcrid) {
        return CLASSIFICATION_PREFIX + mcrid.getRootID();
    }

    @Deprecated(forRemoval = false)
    protected String buildFilePath(MCRCategoryID mcrid) {
        if (mcrid.isRootID()) {
            return rootFolder + mcrid.toString() + ".xml";
        } else {
            return rootFolder + mcrid.getRootID() + '/' + mcrid.toString() + ".xml";
        }
    }

    protected String buildFilePath(MCRCategory mcrCg) {
        StringBuilder builder = new StringBuilder(rootFolder);
        if (mcrCg.isClassification()) {
            builder.append(mcrCg.getId()).append(".xml");
        } else if (mcrCg.isCategory()) {
            ArrayList<String> list = new ArrayList<>();
            list.add(".xml");
            MCRCategory cg = mcrCg;
            int level = mcrCg.getLevel();
            while (level > 0) {
                MCRCategory cgt = cg;
                list.add(cg.getId().toString());
                list.add("/");
                cg = cgt.getParent();
                level = cg.getLevel();
            }
            list.add(cg.getId().toString());
            for (int i = list.size() - 1; i >= 0; i--) {
                builder.append(list.get(i));
            }
        } else {
            throw new MCRException("The MCRClass is of Invalid Type, it must be either a Class or Category");
        }
        return builder.toString();
    }

    protected VersionInfo buildVersionInfo(String message, Date versionDate) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setMessage(message);
        versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
        return versionInfo;
    }

}
