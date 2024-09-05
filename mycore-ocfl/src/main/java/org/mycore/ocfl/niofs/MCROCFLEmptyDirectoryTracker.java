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

package org.mycore.ocfl.niofs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mycore.datamodel.niofs.MCRVersionedPath;

/**
 * Class for tracking empty directories.
 */
public class MCROCFLEmptyDirectoryTracker {

    protected final MCRVersionedPath rootPath;

    protected final Map<MCRVersionedPath, Boolean> original;

    protected final Map<MCRVersionedPath, Boolean> current;

    /**
     * Constructs a new {@code MCROCFLDirectoryTracker} with the specified original paths.
     *
     * @param rootPath path to the root
     * @param originalPaths the original paths to be tracked.
     */
    public MCROCFLEmptyDirectoryTracker(MCRVersionedPath rootPath, Map<MCRVersionedPath, Boolean> originalPaths) {
        this.rootPath = rootPath;
        this.original = new HashMap<>();
        this.current = new HashMap<>();
        for (Map.Entry<MCRVersionedPath, Boolean> entry : originalPaths.entrySet()) {
            update(this.original, entry.getKey(), entry.getValue());
            update(this.current, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Constructs a new {@code MCROCFLDirectoryTracker} with the specified original and current maps.
     *
     * @param rootPath path to the root
     * @param original the original paths map.
     * @param current the current paths map.
     */
    protected MCROCFLEmptyDirectoryTracker(MCRVersionedPath rootPath, Map<MCRVersionedPath, Boolean> original,
        Map<MCRVersionedPath, Boolean> current) {
        this.rootPath = rootPath;
        this.original = original;
        this.current = current;
    }

    /**
     * Updates the current state with the specified path and empty flag.
     *
     * @param path the path to update.
     * @param empty the empty flag indicating if the path is empty.
     */
    public void update(MCRVersionedPath path, boolean empty) {
        update(this.current, path, empty);
    }

    /**
     * Returns an unmodifiable set of all tracked paths.
     *
     * @return a set of all tracked paths.
     */
    public Set<MCRVersionedPath> paths() {
        return Collections.unmodifiableSet(this.current.keySet());
    }

    /**
     * Checks if the specified path exists in the current state.
     *
     * @param path the path to check.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean exists(MCRVersionedPath path) {
        return this.rootPath.equals(path) || this.current.containsKey(path);
    }

    /**
     * Checks if the specified path is empty in the current state.
     *
     * @param path the path to check.
     * @return {@code true} if the path is empty, {@code false} otherwise.
     */
    public boolean isEmpty(MCRVersionedPath path) {
        return this.current.get(path);
    }

    /**
     * Checks if the specified path is newly added.
     *
     * @param path the path to check.
     * @return {@code true} if the path was added, {@code false} otherwise.
     */
    public boolean isAdded(MCRVersionedPath path) {
        boolean existsNow = exists(path);
        boolean existsInOriginal = this.original.containsKey(path);
        return existsNow && !existsInOriginal;
    }

    /**
     * Removes the specified path from the current state.
     *
     * @param path the path to remove.
     */
    public void remove(MCRVersionedPath path) {
        this.current.remove(path);
    }

    /**
     * Renames the specified source path to the target path in the current state.
     *
     * @param source the source path to rename.
     * @param target the target path.
     */
    public void rename(MCRVersionedPath source, MCRVersionedPath target) {
        this.update(this.current, target, isEmpty(source));
        this.remove(source);
    }

    /**
     * Returns a list of changes between the original and current states.
     *
     * @return a list of changes.
     */
    public List<Change> changes() {
        List<Change> changes = new ArrayList<>();
        for (MCRVersionedPath path : this.original.keySet()) {
            if (!this.current.containsKey(path)) {
                changes.add(new Change(ChangeType.REMOVE_KEEP, toKeepFile(path)));
            }
        }
        for (Map.Entry<MCRVersionedPath, Boolean> entry : this.current.entrySet()) {
            MCRVersionedPath path = entry.getKey();
            Boolean currentKeep = entry.getValue();
            Boolean originalKeep = this.original.get(path);
            Boolean keep = originalKeep != null && originalKeep.equals(currentKeep) ? null : currentKeep;
            if (keep != null) {
                changes.add(new Change(keep ? ChangeType.ADD_KEEP : ChangeType.REMOVE_KEEP, toKeepFile(path)));
            }
        }
        return changes;
    }

    public void purge() {
        this.current.clear();
        this.update(this.rootPath, true);
    }

    /**
     * Updates the specified map with the given path and empty flag, and ensures all parent directories are tracked as
     * non-empty.
     *
     * @param mapToUpdate the map to update.
     * @param path the path to update.
     * @param empty the empty flag indicating if the path is empty.
     */
    private void update(Map<MCRVersionedPath, Boolean> mapToUpdate, MCRVersionedPath path, Boolean empty) {
        mapToUpdate.put(path, empty);
        MCRVersionedPath directoryPath = getParent(path);
        while (directoryPath != null) {
            mapToUpdate.put(directoryPath, false);
            directoryPath = getParent(directoryPath);
        }
    }

    /**
     * Creates a deep clone of this directory tracker.
     *
     * @return a deep clone of this directory tracker.
     */
    public MCROCFLEmptyDirectoryTracker deepClone() {
        return new MCROCFLEmptyDirectoryTracker(
            this.rootPath,
            new HashMap<>(this.original),
            new HashMap<>(this.current));
    }

    /**
     * Gets the parent directory of the specified path.
     *
     * @param path the path for which to get the parent directory.
     * @return the parent directory.
     */
    protected MCRVersionedPath getParent(MCRVersionedPath path) {
        return path.getParent();
    }

    /**
     * Converts the specified path to a keep file path.
     *
     * @param path the path to convert.
     * @return the keep file path.
     */
    protected MCRVersionedPath toKeepFile(MCRVersionedPath path) {
        return MCRVersionedPath.toVersionedPath(path.resolve(MCROCFLVirtualObject.KEEP_FILE));
    }

    /**
     * Enum representing the types of changes that can occur in the directory structure.
     */
    public enum ChangeType {
        ADD_KEEP, REMOVE_KEEP;
    }

    /**
     * Record representing a change in the directory structure.
     */
    public record Change(ChangeType type, MCRVersionedPath keepFile) {
    }

}
