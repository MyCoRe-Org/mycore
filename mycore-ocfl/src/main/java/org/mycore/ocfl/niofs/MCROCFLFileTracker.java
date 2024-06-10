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
import java.util.Objects;
import java.util.function.Function;

/**
 * Tracks the state of files within an OCFL virtual object.
 * <p>
 * This class is responsible for tracking file changes such as additions, deletions, modifications, and renames.
 * It uses a digest calculator to determine if a file has been modified. The tracker maintains the state of the files
 * before and after the changes, allowing for easy identification of the changes that have occurred.
 * </p>
 *
 * @param <P> the type representing a file path.
 * @param <D> the type representing a file digest.
 */
public class MCROCFLFileTracker<P, D> {

    private final Map<P, D> originalPaths;

    private final Map<P, PathNode<P, D>> trackMap;

    private Function<P, D> digestCalculator;

    /**
     * Constructs a new {@code MCROCFLFileTracker} with the specified original paths and digest calculator.
     *
     * @param originalPaths a map of original paths and their digests.
     * @param digestCalculator a function to calculate the digest of a path.
     */
    public MCROCFLFileTracker(Map<P, D> originalPaths, Function<P, D> digestCalculator) {
        this.originalPaths = Collections.unmodifiableMap(originalPaths);
        this.trackMap = new HashMap<>();
        this.digestCalculator = digestCalculator;
        for (Map.Entry<P, D> entry : originalPaths.entrySet()) {
            trackMap.put(entry.getKey(), new PathNode<>(entry.getKey(), entry.getValue()));
        }
    }

    private MCROCFLFileTracker(Map<P, D> originalPaths, Map<P, PathNode<P, D>> trackMap) {
        this.originalPaths = originalPaths;
        this.trackMap = trackMap;
        this.digestCalculator = null;
    }

    public void setDigestCalculator(Function<P, D> digestCalculator) {
        this.digestCalculator = digestCalculator;
    }

    /**
     * Marks a path as written. The digest will be computed on demand.
     *
     * @param path the path that was written.
     */
    public void write(P path) {
        this.write(path, null);
    }

    /**
     * Marks a path as written with the specified digest.
     *
     * @param path the path that was written.
     * @param digest the new digest of the path.
     */
    public void write(P path, D digest) {
        P sourcePath = findPath(path);
        D resolvedDigest = digest;
        if (sourcePath != null && !path.equals(sourcePath)) {
            D originalDigest = this.originalPaths.get(sourcePath);
            if (resolvedDigest == null) {
                resolvedDigest = digestCalculator.apply(sourcePath);
            }
            if (Objects.equals(resolvedDigest, originalDigest)) {
                return;
            }
            trackMap.remove(sourcePath);
        }
        trackMap.put(path, new PathNode<>(path, resolvedDigest));
    }

    /**
     * Marks a path as deleted.
     *
     * @param path the path to delete.
     */
    public void delete(P path) {
        P sourcePath = findPath(path);
        if (sourcePath == null) {
            return;
        }
        PathNode<P, D> sourceNode = trackMap.get(path);
        if (sourceNode != null && sourceNode.path.equals(path)) {
            trackMap.remove(path);
            return;
        }
        trackMap.remove(sourcePath);
    }

    /**
     * Renames a path from source to target.
     *
     * @param source the source path.
     * @param target the target path.
     */
    public void rename(P source, P target) {
        P sourcePath = findPath(source);
        if (sourcePath == null) {
            return;
        }
        P targetPath = findPath(target);
        if (targetPath != null && !target.equals(targetPath)) {
            trackMap.remove(targetPath);
        }
        D digest = trackMap.get(sourcePath).digest;
        trackMap.put(sourcePath, new PathNode<>(target, digest));
        if (!sourcePath.equals(target)) {
            trackMap.remove(target);
        }
    }

    /**
     * Returns a list of tracked paths.
     *
     * @return a list of tracked paths.
     */
    public List<P> paths() {
        return this.trackMap.values().stream()
            .map(PathNode::path)
            .toList();
    }

    /**
     * Checks if a path exists in the tracker.
     *
     * @param path the path to check.
     * @return {@code true} if the path exists, {@code false} otherwise.
     */
    public boolean exists(P path) {
        return findPath(path) != null;
    }

    // TODO javadoc & junit tests
    public boolean isAdded(P path) {
        boolean existsNow = exists(path);
        boolean existsInOriginal = this.originalPaths.containsKey(path);
        return existsNow && !existsInOriginal;
    }

    /**
     * Checks if a path is removed from the tracker.
     *
     * @param path the path to check.
     * @return {@code true} if the path is removed, {@code false} otherwise.
     */
    public boolean isRemoved(P path) {
        return !exists(path) && this.originalPaths.containsKey(path);
    }

    /**
     * Checks if a path is added or modified.
     *
     * @param path the path to check.
     * @return {@code true} if the path is added or modified, {@code false} otherwise.
     */
    public boolean isAddedOrModified(P path) {
        PathNode<P, D> node = findPathNode(path);
        if (node == null) {
            return false;
        }
        D originalDigest = originalPaths.get(path);
        if (originalDigest == null) {
            return true;
        }
        if (node.digest == null) {
            node.digest = digestCalculator.apply(node.path);
        }
        return !Objects.equals(originalDigest, node.digest);
    }

    public D getDigest(P path) {
        PathNode<P, D> node = findPathNode(path);
        if (node == null) {
            return null;
        }
        if (node.digest != null) {
            return node.digest;
        }
        node.digest = digestCalculator.apply(node.path);
        return node.digest;
    }

    /**
     * Finds the original path corresponding to the specified key path.
     *
     * @param keyPath the key path to find.
     * @return the original path if found, {@code null} otherwise.
     */
    public P findPath(P keyPath) {
        return this.trackMap.entrySet().stream()
            .filter(entry -> entry.getValue().path.equals(keyPath))
            .findFirst()
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private PathNode<P, D> findPathNode(P keyPath) {
        P sourcePath = findPath(keyPath);
        if (sourcePath == null) {
            return null;
        }
        return this.trackMap.get(sourcePath);
    }

    /**
     * Generates a list of changes based on the tracked paths.
     * <p>
     * This method identifies and categorizes the changes that have occurred to the tracked paths
     * since the initial state. The changes are categorized into three types:
     * </p>
     * <ul>
     *     <li><b>DELETED</b> - Paths that were present in the original state but are no longer present.</li>
     *     <li><b>ADDED_OR_MODIFIED</b> - Paths that have been newly added or have been modified (determined by digest
     *     changes).</li>
     *     <li><b>RENAMED</b> - Paths that have been moved from their original locations to new locations.</li>
     * </ul>
     *
     * @return a list of {@link Change} objects representing the changes.
     */
    public List<Change<P>> changes() {
        List<Change<P>> changes = new ArrayList<>();

        // DELETED
        for (P path : this.originalPaths.keySet()) {
            if (!this.trackMap.containsKey(path)) {
                // could be deleted check if it was renamed instead
                boolean moved = this.trackMap.values().stream()
                    .map(PathNode::path)
                    .anyMatch(path::equals);
                if (!moved) {
                    changes.add(new Change<>(ChangeType.DELETED, path, null));
                }
            }
        }

        // ADDED or MODIFIED
        this.trackMap.entrySet().stream()
            .filter(entry -> {
                return entry.getKey().equals(entry.getValue().path);
            })
            .map(Map.Entry::getValue)
            .filter(node -> {
                D originalDigest = this.originalPaths.get(node.path);
                if (originalDigest == null) {
                    return true;
                }
                if (node.digest == null) {
                    node.digest = digestCalculator.apply(node.path);
                }
                return !Objects.equals(originalDigest, node.digest);
            })
            .forEach(node -> {
                changes.add(new Change<>(ChangeType.ADDED_OR_MODIFIED, node.path, null));
            });

        // MOVED
        this.trackMap.entrySet().stream()
            .filter(entry -> {
                return !entry.getKey().equals(entry.getValue().path);
            })
            .forEach(entry -> {
                changes.add(new Change<>(ChangeType.RENAMED, entry.getKey(), entry.getValue().path));
            });
        return changes;
    }

    public void purge() {
        this.trackMap.clear();
    }

    /**
     * Creates a deep clone of this file tracker.
     *
     * @return a newly generated file tracker with copied state.
     */
    public MCROCFLFileTracker<P, D> deepClone() {
        return new MCROCFLFileTracker<>(
            Collections.unmodifiableMap(this.originalPaths),
            new HashMap<>(this.trackMap));
    }

    private static class PathNode<P, D> {

        private final P path;

        private D digest;

        PathNode(P path, D digest) {
            this.path = path;
            this.digest = digest;
        }

        public P path() {
            return path;
        }

    }

    /**
     * Types of changes that can be tracked.
     */
    public enum ChangeType {
        ADDED_OR_MODIFIED, DELETED, RENAMED
    }

    /**
     * Represents a change in the tracked paths.
     *
     * @param <T> the type of the paths.
     */
    public record Change<T>(ChangeType type, T source, T target) {
    }

}
