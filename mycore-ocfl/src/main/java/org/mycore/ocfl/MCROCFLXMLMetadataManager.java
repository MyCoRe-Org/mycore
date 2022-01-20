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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManagerAdapter;
import org.mycore.datamodel.ifs2.MCRObjectIDDateImpl;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

import edu.wisc.library.ocfl.api.OcflOption;
import edu.wisc.library.ocfl.api.OcflRepository;
import edu.wisc.library.ocfl.api.exception.NotFoundException;
import edu.wisc.library.ocfl.api.exception.OverwriteException;
import edu.wisc.library.ocfl.api.model.ObjectVersionId;
import edu.wisc.library.ocfl.api.model.OcflObjectVersion;
import edu.wisc.library.ocfl.api.model.VersionDetails;
import edu.wisc.library.ocfl.api.model.VersionInfo;
import edu.wisc.library.ocfl.api.model.VersionNum;

/**
 * Manages persistence of MCRObject and MCRDerivate xml metadata. Provides
 * methods to create, retrieve, update and delete object metadata using OCFL
 **/
public class MCROCFLXMLMetadataManager implements MCRXMLMetadataManagerAdapter {

    private static final String MESSAGE_CREATED = "Created";

    private static final String MESSAGE_UPDATED = "Updated";

    private static final String MESSAGE_DELETED = "Deleted";

    private static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCROCFLMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCROCFLMetadataVersion.DELETED)));

    public static final String MCR_OBJECT_ID_PREFIX = "mcrobject:";

    public static final String MCR_DERIVATE_ID_PREFIX = "mcrderivate:";

    private String repositoryKey = "Default";

    private static char convertMessageToType(String message) throws MCRPersistenceException {
        if (!MESSAGE_TYPE_MAPPING.containsKey(message)) {
            throw new MCRPersistenceException("Cannot identify version type from message '" + message + "'");
        }
        return MESSAGE_TYPE_MAPPING.get(message);
    }

    public OcflRepository getRepository() {
        return MCROCFLRepositoryProvider.getRepository(getRepositoryKey());
    }

    public String getRepositoryKey() {
        return repositoryKey;
    }

    @MCRProperty(name = "Repository", required = false)
    public void setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
    }

    @Override
    public void reload() {
        // nothing to reload here
    }

    @Override
    public void verifyStore(String base) {
        // not supported yet
    }

    @Override
    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        create(mcrid, xml, lastModified, null);
    }

    public void create(MCRObjectID mcrid, MCRContent xml, Date lastModified, String user)
        throws MCRPersistenceException {
        String objName = getObjName(mcrid);
        VersionInfo info = buildVersionInfo(MESSAGE_CREATED, lastModified, user);
        try (InputStream objectAsStream = xml.getInputStream()) {
            getRepository().updateObject(ObjectVersionId.head(objName), info, init -> {
                init.writeFile(objectAsStream, buildFilePath(mcrid));
            });
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to create object '" + objName + "'", e);
        }
    }

    @Override
    public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
        delete(mcrid, null, null);
    }

    public void delete(MCRObjectID mcrid, Date date, String user) throws MCRPersistenceException {
        String objName = getObjName(mcrid);
        if (!exists(mcrid)) {
            throw new MCRUsageException("Cannot delete nonexistent object '" + objName + "'");
        }
        OcflRepository repo = getRepository();
        VersionInfo headVersion = repo.describeObject(objName).getHeadVersion().getVersionInfo();
        char versionType = convertMessageToType(headVersion.getMessage());
        if (versionType == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot delete already deleted object '" + objName + "'");
        }
        repo.updateObject(ObjectVersionId.head(objName), buildVersionInfo(MESSAGE_DELETED, date, null), init -> {
            init.removeFile(buildFilePath(mcrid));
        });
    }

    @Override
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        update(mcrid, xml, lastModified, null);
    }

    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified, String user)
        throws MCRPersistenceException {
        String objName = getObjName(mcrid);
        if (!exists(mcrid)) {
            throw new MCRUsageException("Cannot update nonexistent object '" + objName + "'");
        }
        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(MESSAGE_UPDATED, lastModified, user);
            getRepository().updateObject(ObjectVersionId.head(objName), versionInfo, init -> {
                init.writeFile(objectAsStream, buildFilePath(mcrid), OcflOption.OVERWRITE);
            });
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + objName + "'", e);
        }
    }

    private String getObjName(MCRObjectID mcrid) {
        return getObjName(mcrid.toString());
    }

    private String getObjName(String mcrid) {
        String objectType = MCRObjectID.getIDParts(mcrid.trim())[1].toLowerCase(Locale.ROOT).intern();
        return "derivate".equals(objectType) ? MCR_DERIVATE_ID_PREFIX + mcrid : MCR_OBJECT_ID_PREFIX + mcrid;
    }

    private String buildFilePath(MCRObjectID objName) {
        return "metadata/" + objName + ".xml";
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid) throws IOException {
        String objName = getObjName(mcrid);
        OcflObjectVersion storeObject;
        try {
            storeObject = getRepository().getObject(ObjectVersionId.head(objName));
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }

        if (convertMessageToType(
            storeObject.getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }

        // "metadata/" +
        try (InputStream storedContentStream = storeObject.getFile(buildFilePath(mcrid)).getStream()) {
            return new MCRJDOMContent(new MCRStreamContent(storedContentStream).asXML());
        } catch (JDOMException | SAXException e) {
            throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        }
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid, String revision) throws IOException {
        if (revision == null) {
            return retrieveContent(mcrid);
        }
        String objName = getObjName(mcrid);
        OcflObjectVersion storeObject;
        OcflRepository repo = getRepository();
        try {
            storeObject = repo.getObject(ObjectVersionId.version(objName, revision));
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + objName + "' could not be found", e);
        }

        // maybe use .head(objName) instead to prevent requests of old versions of deleted objects
        if (convertMessageToType(repo.getObject(ObjectVersionId.version(objName, revision)).getVersionInfo()
            .getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot read already deleted object '" + objName + "'");
        }

        try (InputStream storedContentStream = storeObject.getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            xml.getRootElement().setAttribute("rev", revision);
            return new MCRJDOMContent(xml);
        } catch (JDOMException | SAXException e) {
            throw new MCRPersistenceException("Can not parse XML from OCFL-Store", e);
        }
    }

    private VersionInfo buildVersionInfo(String message, Date versionDate, String user) {
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.setMessage(message);
        versionInfo.setCreated((versionDate == null ? new Date() : versionDate).toInstant().atOffset(ZoneOffset.UTC));
        String userID = user != null ? user
            : Optional.ofNullable(MCRSessionMgr.getCurrentSession())
                .map(MCRSession::getUserInformation)
                .map(MCRUserInformation::getUserID)
                .orElse(null);
        versionInfo.setUser(userID, null);
        return versionInfo;
    }

    @Override
    public List<MCROCFLMetadataVersion> listRevisions(MCRObjectID id) throws IOException, NotFoundException {
        String objName = getObjName(id);
        Map<VersionNum, VersionDetails> versionMap = getRepository().describeObject(objName).getVersionMap();
        return versionMap.entrySet().stream().map(v -> {
            VersionNum key = v.getKey();
            VersionDetails details = v.getValue();
            VersionInfo versionInfo = details.getVersionInfo();

            MCROCFLContent content = new MCROCFLContent(getRepository(), objName, buildFilePath(id), key.toString());
            return new MCROCFLMetadataVersion(content,
                key.toString(),
                versionInfo.getUser().getName(),
                Date.from(details.getCreated().toInstant()), convertMessageToType(versionInfo.getMessage()));
        }).collect(Collectors.toList());
    }

    public IntStream getStoredIDs(String project, String type) throws MCRPersistenceException {
        return getRepository().listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .filter(id -> id.startsWith(project + "_" + type))
            .mapToInt((fullId) -> Integer.parseInt(fullId.substring(project.length() + type.length() + 2))).sorted();
    }

    @Override
    public int getHighestStoredID(String project, String type) {
        return getStoredIDs(project, type).max().orElse(0);
    }

    @Override
    public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
        String objName = getObjName(mcrid);
        return getRepository().containsObject(objName) && isNotDeleted(objName);
    }

    @Override
    public List<String> listIDsForBase(String base) {
        return getRepository().listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .filter(this::isNotDeleted)
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .filter(s -> s.startsWith(base))
            .collect(Collectors.toList());

    }

    @Override
    public List<String> listIDsOfType(String type) {
        return getRepository().listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .filter(this::isNotDeleted)
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .filter(s -> type.equals(s.split("_")[1]))
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listIDs() {
        OcflRepository repo = getRepository();
        return repo
            .listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .filter(this::isNotDeleted)
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .collect(Collectors.toList());
    }

    private boolean isNotDeleted(String ocflID) {
        return convertMessageToType(getRepository().describeVersion(ObjectVersionId.head(ocflID)).getVersionInfo()
            .getMessage()) != MCROCFLMetadataVersion.DELETED;
    }

    @Override
    public Collection<String> getObjectTypes() {
        return getRepository()
            .listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .map(s -> s.split("_")[1])
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getObjectBaseIds() {
        return getRepository()
            .listObjectIds()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .map(s -> s.substring(MCR_OBJECT_ID_PREFIX.length()))
            .map(s -> s.substring(0, s.lastIndexOf("_")))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException {
        return ids.stream()
            .filter(id -> id.startsWith(MCR_OBJECT_ID_PREFIX))
            .filter(this::isNotDeleted)
            .map(id -> id.substring(MCR_OBJECT_ID_PREFIX.length()))
            .map(id -> new MCRObjectIDDateImpl(new Date(getLastModified(getObjName(id))), id))
            .collect(Collectors.toList());
    }

    @Override
    public long getLastModified(MCRObjectID id) throws IOException {
        return getLastModified(getObjName(id));
    }

    public long getLastModified(String objName) {
        return Date.from(getRepository()
            .getObject(ObjectVersionId.head(objName))
            .getVersionInfo()
            .getCreated()
            .toInstant())
            .getTime();
    }

    @Override
    public MCRCache.ModifiedHandle getLastModifiedHandle(MCRObjectID id, long expire, TimeUnit unit) {
        return new MCROCFLXMLMetadataManager.StoreModifiedHandle(id, expire, unit);
    }

    private final class StoreModifiedHandle implements MCRCache.ModifiedHandle {

        private final long expire;

        private final MCRObjectID id;

        private StoreModifiedHandle(MCRObjectID id, long time, TimeUnit unit) {
            this.expire = unit.toMillis(time);
            this.id = id;
        }

        @Override
        public long getCheckPeriod() {
            return expire;
        }

        @Override
        public long getLastModified() throws IOException {
            return MCROCFLXMLMetadataManager.this.getLastModified(id);
        }
    }
}
