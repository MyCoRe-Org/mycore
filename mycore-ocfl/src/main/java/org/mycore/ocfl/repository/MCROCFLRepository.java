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

package org.mycore.ocfl.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.ocfl.api.OcflConfig;
import io.ocfl.api.OcflConstants;
import io.ocfl.api.OcflObjectUpdater;
import io.ocfl.api.OcflOption;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.exception.NotFoundException;
import io.ocfl.api.model.FileChange;
import io.ocfl.api.model.FileChangeHistory;
import io.ocfl.api.model.FileChangeType;
import io.ocfl.api.model.FileDetails;
import io.ocfl.api.model.ObjectDetails;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.ValidationResults;
import io.ocfl.api.model.VersionDetails;
import io.ocfl.api.model.VersionInfo;
import io.ocfl.api.model.VersionNum;

/**
 * A wrapper class for an OCFL repository that adds custom functionality specific to MyCoRe.
 * <p>
 * This class implements the {@link OcflRepository} interface and delegates most of its operations to a base OCFL
 * repository. It also provides additional methods for directory change history tracking and repository management.
 * </p>
 */
public class MCROCFLRepository implements OcflRepository {

    private final String id;

    private final OcflRepository base;

    /**
     * Constructs a new {@code MCROCFLRepository}.
     *
     * @param id the unique identifier for the repository.
     * @param repository the base OCFL repository to delegate operations to.
     */
    public MCROCFLRepository(String id, OcflRepository repository) {
        this.id = id;
        this.base = repository;
    }

    /**
     * Returns the unique identifier for this repository.
     *
     * @return the repository ID.
     */
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectVersionId putObject(ObjectVersionId objectVersionId, Path path, VersionInfo versionInfo,
        OcflOption... options) {
        return base.putObject(objectVersionId, path, versionInfo, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectVersionId updateObject(ObjectVersionId objectVersionId, VersionInfo versionInfo,
        Consumer<OcflObjectUpdater> objectUpdater) {
        return base.updateObject(objectVersionId, versionInfo, objectUpdater);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getObject(ObjectVersionId objectVersionId, Path outputPath) {
        base.getObject(objectVersionId, outputPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OcflObjectVersion getObject(ObjectVersionId objectVersionId) {
        return base.getObject(objectVersionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectDetails describeObject(String objectId) {
        return base.describeObject(objectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionDetails describeVersion(ObjectVersionId objectVersionId) {
        return base.describeVersion(objectVersionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileChangeHistory fileChangeHistory(String objectId, String logicalPath) throws NotFoundException {
        return base.fileChangeHistory(objectId, logicalPath);
    }

    /**
     * Returns the FileChangeHistory for a certain version.
     * TODO unit tests & javadoc
     *
     * @param objectId  the id of the object
     * @param logicalPath the logical path
     * @param targetVersion maximum version
     * @return the change history for the logical path
     * @throws NotFoundException when object or logical path cannot be found
     */
    public FileChangeHistory fileChangeHistory(String objectId, String logicalPath, VersionNum targetVersion)
        throws NotFoundException {
        FileChangeHistory originalChangeHistory = base.fileChangeHistory(objectId, logicalPath);
        FileChangeHistory newChangeHistory = new FileChangeHistory();
        newChangeHistory.setPath(originalChangeHistory.getPath());
        for (FileChange fileChange : originalChangeHistory.getFileChanges()) {
            if (fileChange.getVersionNum().getVersionNum() > targetVersion.getVersionNum()) {
                break;
            }
            newChangeHistory.getFileChanges().add(fileChange);
        }
        return newChangeHistory;
    }

    /**
     * Retrieves the complete directory change history for a logical path within an object. Each entry in the change
     * history marks a directory version where the contents at the logical path were changed or the logical path was
     * removed.
     * Object versions where there were no changes to the logical path are not included.
     *
     * @param objectId  the id of the object
     * @param logicalPath the logical path
     * @return the change history for the logical path
     * @throws NotFoundException when object or logical path cannot be found
     */
    public FileChangeHistory directoryChangeHistory(String objectId, String logicalPath) throws NotFoundException {
        ObjectVersionId headVersion = ObjectVersionId.head(objectId);
        VersionDetails headDetails = describeVersion(headVersion);
        VersionNum headVersionNumber = headDetails.getVersionNum();
        return directoryChangeHistory(objectId, logicalPath, headVersionNumber);
    }

    /**
     * Retrieves the complete directory change history for a logical path within an object. Each entry in the change
     * history marks a directory version where the contents at the logical path were changed or the logical path was
     * removed.
     * Object versions where there were no changes to the logical path are not included.
     * 
     * @param objectId  the id of the object
     * @param logicalPath the logical path
     * @param targetVersion maximum version
     * @return the change history for the logical path
     * @throws NotFoundException when object or logical path cannot be found
     */
    public FileChangeHistory directoryChangeHistory(String objectId, String logicalPath, VersionNum targetVersion)
        throws NotFoundException {
        String relativeDirectoryPath = logicalPath.startsWith("/") ? logicalPath.substring(1) : logicalPath;

        VersionNum tailVersionNumber = VersionNum.V1;

        FileChangeHistory directoryChangeHistory = new FileChangeHistory();
        directoryChangeHistory.setPath(relativeDirectoryPath);
        List<FileChange> directoryChanges = new ArrayList<>();
        directoryChangeHistory.setFileChanges(directoryChanges);

        boolean empty = true;
        VersionNum versionNum = tailVersionNumber;
        do {
            VersionDetails details = describeVersion(ObjectVersionId.version(objectId, versionNum));
            List<String> directoryContent = details.getFiles().stream()
                .map(FileDetails::getPath)
                .map(filePath -> toDirectoryContent(relativeDirectoryPath, filePath))
                .filter(Objects::nonNull)
                .toList();
            if ((empty && !directoryContent.isEmpty()) || (!empty && directoryContent.isEmpty())) {
                empty = !empty;
                FileChange directoryChange = new FileChange();
                directoryChange.setPath(relativeDirectoryPath);
                directoryChange.setChangeType(empty ? FileChangeType.REMOVE : FileChangeType.UPDATE);
                directoryChange.setTimestamp(details.getCreated());
                directoryChange.setVersionInfo(details.getVersionInfo());
                directoryChange.setObjectVersionId(details.getObjectVersionId());
                String finalVersion = versionNum.toString();
                details.getFiles().stream().findAny().ifPresent(fileDetails -> {
                    String fileRelativePath = fileDetails.getStorageRelativePath();
                    String relativeStoragePath = getStorageRelativePath(fileRelativePath, objectId, finalVersion);
                    String directoryStoragePath = relativeDirectoryPath.isEmpty() ? relativeStoragePath
                        : relativeStoragePath + "/" + relativeDirectoryPath;
                    directoryChange.setStorageRelativePath(directoryStoragePath);
                });
                directoryChanges.add(directoryChange);
            }
            versionNum = versionNum.nextVersionNum();
        } while (versionNum.getVersionNum() <= targetVersion.getVersionNum());

        return directoryChangeHistory;
    }

    private String toDirectoryContent(String directory, String file) {
        if (!directory.isEmpty() && !file.startsWith(directory)) {
            return null;
        }
        String relativePath = file.substring(directory.length());
        int nextPathSegmentIndex = file.indexOf("/");
        return nextPathSegmentIndex != -1 ? relativePath.substring(0, nextPathSegmentIndex) : relativePath;
    }

    private String getStorageRelativePath(String fileRelativePath, String objectId, String version) {
        String layoutConfigPath = fileRelativePath.substring(0, fileRelativePath.indexOf("/" + objectId));
        return layoutConfigPath + "/" + objectId + "/" + version + "/" + OcflConstants.DEFAULT_CONTENT_DIRECTORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsObject(String objectId) {
        return base.containsObject(objectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<String> listObjectIds() {
        return base.listObjectIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void purgeObject(String objectId) {
        base.purgeObject(objectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValidationResults validateObject(String objectId, boolean contentFixityCheck) {
        return base.validateObject(objectId, contentFixityCheck);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectVersionId replicateVersionAsHead(ObjectVersionId objectVersionId, VersionInfo versionInfo) {
        return base.replicateVersionAsHead(objectVersionId, versionInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollbackToVersion(ObjectVersionId objectVersionId) {
        base.rollbackToVersion(objectVersionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportVersion(ObjectVersionId objectVersionId, Path outputPath, OcflOption... options) {
        base.exportVersion(objectVersionId, outputPath, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportObject(String objectId, Path outputPath, OcflOption... options) {
        base.exportObject(objectId, outputPath, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importVersion(Path versionPath, OcflOption... options) {
        base.importVersion(versionPath, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importObject(Path objectPath, OcflOption... options) {
        base.importObject(objectPath, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        base.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OcflConfig config() {
        return base.config();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCache(String objectId) {
        base.invalidateCache(objectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCache() {
        base.invalidateCache();
    }

}
