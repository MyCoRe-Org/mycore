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

package org.mycore.ocfl.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUsageException;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStreamContent;
import org.mycore.datamodel.common.MCRObjectIDDate;
import org.mycore.datamodel.common.MCRXMLMetadataManagerAdapter;
import org.mycore.datamodel.ifs2.MCRObjectIDDateImpl;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.history.MCRMetadataHistoryManager;
import org.mycore.ocfl.layout.MCRStorageLayoutConfig;
import org.mycore.ocfl.layout.MCRStorageLayoutExtension;
import org.mycore.ocfl.repository.MCROCFLHashRepositoryProvider;
import org.mycore.ocfl.repository.MCROCFLMCRRepositoryProvider;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.util.MCROCFLDeleteUtils;
import org.mycore.ocfl.util.MCROCFLMetadataVersion;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.OcflOption;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.exception.OverwriteException;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.VersionDetails;
import io.ocfl.api.model.VersionInfo;
import io.ocfl.api.model.VersionNum;
import io.ocfl.core.extension.OcflExtensionConfig;
import io.ocfl.core.extension.storage.layout.HashedNTupleIdEncapsulationLayoutExtension;
import io.ocfl.core.extension.storage.layout.config.HashedNTupleIdEncapsulationLayoutConfig;

/**
 * Manages persistence of MCRObject and MCRDerivate xml metadata. Provides
 * methods to create, retrieve, update and delete object metadata using OCFL
 */
public class MCROCFLXMLMetadataManager implements MCRXMLMetadataManagerAdapter {

    private static final String MESSAGE_CREATED = "Created";

    private static final String MESSAGE_UPDATED = "Updated";

    private static final String MESSAGE_DELETED = "Deleted";

    private static final Map<String, Character> MESSAGE_TYPE_MAPPING = Collections.unmodifiableMap(Map.ofEntries(
        Map.entry(MESSAGE_CREATED, MCROCFLMetadataVersion.CREATED),
        Map.entry(MESSAGE_UPDATED, MCROCFLMetadataVersion.UPDATED),
        Map.entry(MESSAGE_DELETED, MCROCFLMetadataVersion.DELETED)));

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
        String ocflObjectID = getOCFLObjectID(mcrid);
        VersionInfo info = buildVersionInfo(MESSAGE_CREATED, lastModified, user);
        try (InputStream objectAsStream = xml.getInputStream()) {
            getRepository().updateObject(ObjectVersionId.head(ocflObjectID), info,
                init -> init.writeFile(objectAsStream, buildFilePath(mcrid)));
        } catch (IOException | OverwriteException e) {
            throw new MCRPersistenceException("Failed to create object '" + ocflObjectID + "'", e);
        }
    }

    @Override
    public void delete(MCRObjectID mcrid) throws MCRPersistenceException {
        delete(mcrid, null, null);
    }

    public void delete(MCRObjectID mcrid, Date date, String user) throws MCRPersistenceException {
        String prefix = Objects.equals(mcrid.getTypeId(), "derivate") ? MCROCFLObjectIDPrefixHelper.MCRDERIVATE
            : MCROCFLObjectIDPrefixHelper.MCROBJECT;

        if (MCROCFLDeleteUtils.checkPurgeObject(mcrid, prefix)) {
            purge(mcrid, date, user);
            return;
        }

        String ocflObjectID = getOCFLObjectID(mcrid);
        if (!exists(mcrid)) {
            throw new MCRUsageException("Cannot delete nonexistent object '" + ocflObjectID + "'");
        }
        OcflRepository repo = getRepository();
        VersionInfo headVersion = repo.describeObject(ocflObjectID).getHeadVersion().getVersionInfo();
        char versionType = convertMessageToType(headVersion.getMessage());
        if (versionType == MCROCFLMetadataVersion.DELETED) {
            throw new MCRUsageException("Cannot delete already deleted object '" + ocflObjectID + "'");
        }
        repo.updateObject(ObjectVersionId.head(ocflObjectID), buildVersionInfo(MESSAGE_DELETED, date, null),
            init -> init.removeFile(buildFilePath(mcrid)));
    }

    public void purge(MCRObjectID mcrid, Date date, String user) {
        String ocflObjectID = getOCFLObjectID(mcrid);
        if (!getRepository().containsObject(ocflObjectID)) {
            throw new MCRUsageException("Cannot delete nonexistent object '" + ocflObjectID + "'");
        }

        OcflRepository repo = getRepository();
        repo.purgeObject(ocflObjectID);
    }

    @Override
    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified) throws MCRPersistenceException {
        update(mcrid, xml, lastModified, null);
    }

    public void update(MCRObjectID mcrid, MCRContent xml, Date lastModified, String user)
        throws MCRPersistenceException {
        String ocflObjectID = getOCFLObjectID(mcrid);
        if (!exists(mcrid)) {
            throw new MCRUsageException("Cannot update nonexistent object '" + ocflObjectID + "'");
        }
        try (InputStream objectAsStream = xml.getInputStream()) {
            VersionInfo versionInfo = buildVersionInfo(MESSAGE_UPDATED, lastModified, user);
            getRepository().updateObject(ObjectVersionId.head(ocflObjectID), versionInfo,
                init -> init.writeFile(objectAsStream, buildFilePath(mcrid), OcflOption.OVERWRITE));
        } catch (IOException e) {
            throw new MCRPersistenceException("Failed to update object '" + ocflObjectID + "'", e);
        }
    }

    private String getOCFLObjectID(MCRObjectID mcrid) {
        return getOCFLObjectID(mcrid.toString());
    }

    private String getOCFLObjectID(String mcrid) {
        String objectType = MCRObjectID.getInstance(mcrid).getTypeId();
        return Objects.equals(objectType, "derivate") ? MCROCFLObjectIDPrefixHelper.MCRDERIVATE + mcrid
            : MCROCFLObjectIDPrefixHelper.MCROBJECT + mcrid;
    }

    private String buildFilePath(MCRObjectID mcrid) {
        return "metadata/" + mcrid + ".xml";
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid) throws IOException {
        String ocflObjectID = getOCFLObjectID(mcrid);
        OcflObjectVersion storeObject;
        try {
            storeObject = getRepository().getObject(ObjectVersionId.head(ocflObjectID));
        } catch (NotFoundException e) {
            throw new IOException("Object '" + ocflObjectID + "' could not be found", e);
        }

        if (convertMessageToType(
            storeObject.getVersionInfo().getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new IOException("Cannot read already deleted object '" + ocflObjectID + "'");
        }

        // "metadata/" +
        try (InputStream storedContentStream = storeObject.getFile(buildFilePath(mcrid)).getStream()) {
            return new MCRJDOMContent(new MCRStreamContent(storedContentStream).asXML());
        } catch (JDOMException e) {
            throw new IOException("Can not parse XML from OCFL-Store", e);
        }
    }

    @Override
    public MCRContent retrieveContent(MCRObjectID mcrid, String revision) throws IOException {
        if (revision == null) {
            return retrieveContent(mcrid);
        }
        String ocflObjectID = getOCFLObjectID(mcrid);
        OcflObjectVersion storeObject;
        OcflRepository repo = getRepository();
        try {
            storeObject = repo.getObject(ObjectVersionId.version(ocflObjectID, revision));
        } catch (NotFoundException e) {
            throw new IOException("Object '" + ocflObjectID + "' could not be found", e);
        }

        // maybe use .head(ocflObjectID) instead to prevent requests of old versions of deleted objects
        if (convertMessageToType(repo.getObject(ObjectVersionId.version(ocflObjectID, revision)).getVersionInfo()
            .getMessage()) == MCROCFLMetadataVersion.DELETED) {
            throw new IOException("Cannot read already deleted object '" + ocflObjectID + "'");
        }

        try (InputStream storedContentStream = storeObject.getFile(buildFilePath(mcrid)).getStream()) {
            Document xml = new MCRStreamContent(storedContentStream).asXML();
            xml.getRootElement().setAttribute("rev", revision); // bugfix: MCR-2510, PR #1373
            return new MCRJDOMContent(xml);
        } catch (JDOMException e) {
            throw new IOException("Can not parse XML from OCFL-Store", e);
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
    public List<MCROCFLMetadataVersion> listRevisions(MCRObjectID id) {
        String ocflObjectID = getOCFLObjectID(id);
        Map<VersionNum, VersionDetails> versionMap;

        try {
            versionMap = getRepository().describeObject(ocflObjectID).getVersionMap();
        } catch (NotFoundException e) {
            throw new MCRUsageException("Object '" + ocflObjectID + "' could not be found", e);
        }

        return versionMap.entrySet().stream().map(v -> {
            VersionNum key = v.getKey();
            VersionDetails details = v.getValue();
            VersionInfo versionInfo = details.getVersionInfo();

            MCROCFLContent content = new MCROCFLContent(getRepository(), ocflObjectID, buildFilePath(id),
                key.toString());
            return new MCROCFLMetadataVersion(content,
                key.toString(),
                versionInfo.getUser().getName(),
                Date.from(details.getCreated().toInstant()), convertMessageToType(versionInfo.getMessage()));
        }).collect(Collectors.toList());
    }

    private boolean isMetadata(String id) {
        return id.startsWith(MCROCFLObjectIDPrefixHelper.MCROBJECT)
            || id.startsWith(MCROCFLObjectIDPrefixHelper.MCRDERIVATE);
    }

    private String removePrefix(String id) {
        return id.startsWith(MCROCFLObjectIDPrefixHelper.MCRDERIVATE)
            ? id.substring(MCROCFLObjectIDPrefixHelper.MCRDERIVATE.length())
            : id.substring(MCROCFLObjectIDPrefixHelper.MCROBJECT.length());
    }

    public IntStream getStoredIDs(String project, String type) throws MCRPersistenceException {
        return getRepository().listObjectIds()
            .filter(this::isMetadata)
            .map(this::removePrefix)
            .filter(id -> id.startsWith(project + "_" + type))
            .mapToInt((fullId) -> Integer.parseInt(fullId.substring(project.length() + type.length() + 2))).sorted();
    }

    @Override
    public int getHighestStoredID(String project, String type) {
        int highestStoredID = 0;
        int maxDepth = Integer.MAX_VALUE;
        MCROCFLRepositoryProvider oclfRepoProvider = MCRConfiguration2
            .getSingleInstanceOf("MCR.OCFL.Repository." + repositoryKey)
            .map(MCROCFLRepositoryProvider.class::cast).orElseThrow();

        OcflExtensionConfig config = oclfRepoProvider.getExtensionConfig();
        Path basePath = null;

        // optimization for known layouts
        if (Objects.equals(config.getExtensionName(), MCRStorageLayoutExtension.EXTENSION_NAME)) {
            maxDepth = ((MCRStorageLayoutConfig) config).getSlotLayout().split("-").length;
            basePath = ((MCROCFLMCRRepositoryProvider) oclfRepoProvider).getRepositoryRoot()
                .resolve(MCROCFLObjectIDPrefixHelper.MCROBJECT.replace(":", ""))
                .resolve(project).resolve(type);
            highestStoredID = traverseMCRStorageDirectory(basePath, maxDepth);
        } else if (Objects.equals(config.getExtensionName(),
            HashedNTupleIdEncapsulationLayoutExtension.EXTENSION_NAME)) {
            maxDepth = ((HashedNTupleIdEncapsulationLayoutConfig) config).getNumberOfTuples() + 1;
            basePath = ((MCROCFLHashRepositoryProvider) oclfRepoProvider).getRepositoryRoot();
        } else {
            //for other repository provider implementation start with root directory
            basePath = MCRConfiguration2.getString("MCR.OCFL.Repository." + repositoryKey + ".RepositoryRoot")
                .map(Path::of).orElse(null);
        }

        if (basePath != null && highestStoredID == 0) {
            Pattern pattern = Pattern.compile("^.*" + project + "_{1}" + type + "_{1}\\d+$");
            try (Stream<Path> stream = Files.find(basePath, maxDepth,
                (filePath, fileAttr) -> pattern.matcher(filePath.getFileName().toString()).matches())) {
                highestStoredID = stream.map(entry -> entry.getFileName().toString())
                    .map(fileName -> Integer.parseInt(fileName.substring(fileName.lastIndexOf('_') + 1)))
                    .max(Integer::compareTo).orElse(0);
            } catch (Exception e) {
                // nothing found, let the HistoryManager take over
            }
        }
        return Math.max(highestStoredID, MCRMetadataHistoryManager.getHighestStoredID(project, type)
            .map(MCRObjectID::getNumberAsInteger)
            .orElse(0));
    }

    /** 
     * This method recursively parses a directory structure in MCRStorageLayout,
     * like this:
     * <pre>
     * ocfl-root
     * +-- mcrobject
     *     +-- mir
     *         +-- mods
     *             +-- 0000
     *                 +-- 00
     *                     +-- mir_mods_0000000018
     *                         ...
     *                     +-- mir_mods_0000004567
     *                         ...
     *                     +-- mir_mods_0000009997
     *                 +-- 01
     *                     +-- mir_mods_0000010001
     *                         ...
     *                     +-- mir_mods_0000014567
     *                         ...
     *                     +-- mir_mods_0000017654 
     *  </pre>
     *  
     * and searches on each level for the directory with the highest number at the end of it's name
     * finally it returns the highest available number part of a MyCoRe object id.
     * 
     * @param path - the root path
     * @param depth - the level of subdirectories to look into
     * @return the highest number, or 0 if such cannot be found 
     */
    private int traverseMCRStorageDirectory(Path path, int depth) {
        int max = -1;
        Path newPath = path;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path,
            p -> p.getFileName().toString().matches("^.*\\d+$"))) {
            for (Path entry : ds) {
                int current = Integer.parseInt(entry.getFileName().toString().replaceAll("^.*_", ""));
                if (max < current) {
                    max = current;
                    newPath = entry;
                }
            }
        } catch (IOException | NumberFormatException e) {
            // fallback to slower full tree search
            return 0;
        }
        if (depth <= 1) {
            return Math.max(0, max);
        } else {
            return traverseMCRStorageDirectory(newPath, depth - 1);
        }
    }

    @Override
    public boolean exists(MCRObjectID mcrid) throws MCRPersistenceException {
        String ocflObjectID = getOCFLObjectID(mcrid);
        return getRepository().containsObject(ocflObjectID) && isNotDeleted(ocflObjectID);
    }

    @Override
    public List<String> listIDsForBase(String base) {
        return getRepository().listObjectIds()
            .filter(this::isMetadata)
            .filter(this::isNotDeleted)
            .map(this::removePrefix)
            .filter(s -> s.startsWith(base))
            .collect(Collectors.toList());

    }

    @Override
    public List<String> listIDsOfType(String type) {
        return getRepository().listObjectIds()
            .filter(this::isMetadata)
            .filter(this::isNotDeleted)
            .map(this::removePrefix)
            .filter(s -> type.equals(s.split("_")[1]))
            .collect(Collectors.toList());
    }

    @Override
    public List<String> listIDs() {
        OcflRepository repo = getRepository();
        return repo
            .listObjectIds()
            .filter(this::isMetadata)
            .filter(this::isNotDeleted)
            .map(this::removePrefix)
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
            .filter(this::isMetadata)
            .map(this::removePrefix)
            .map(s -> s.split("_")[1])
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getObjectBaseIds() {
        return getRepository()
            .listObjectIds()
            .filter(this::isMetadata)
            .map(this::removePrefix)
            .map(s -> s.substring(0, s.lastIndexOf("_")))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<MCRObjectIDDate> retrieveObjectDates(List<String> ids) throws IOException {
        try {
            return ids.stream()
                .filter(this::isMetadata)
                .filter(this::isNotDeleted)
                .map(this::removePrefix)
                .map(id -> {
                    try {
                        return new MCRObjectIDDateImpl(new Date(getLastModified(getOCFLObjectID(id))), id);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .collect(Collectors.toList());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public long getLastModified(MCRObjectID id) throws IOException {
        return getLastModified(getOCFLObjectID(id));
    }

    public long getLastModified(String ocflObjectId) throws IOException {
        try {
            return Date.from(getRepository()
                .getObject(ObjectVersionId.head(ocflObjectId))
                .getVersionInfo()
                .getCreated()
                .toInstant())
                .getTime();
        } catch (NotFoundException e) {
            throw new IOException(e);
        }
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
