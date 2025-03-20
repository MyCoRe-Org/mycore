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

package org.mycore.ocfl.niofs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            P path = entry.getKey();
            trackMap.put(path, new PathNode<>(path, path, entry.getValue()));
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
        P keyFound = findPath(path);
        if (keyFound != null) {
            PathNode<P, D> node = trackMap.get(keyFound);
            boolean isOriginalNode = originalPaths.containsKey(node.originalPath());
            D origDigest = isOriginalNode ? originalPaths.get(node.originalPath()) : null;
            if (isOriginalNode && Objects.equals(digest, origDigest)) {
                // Content now matches original: update digest.
                node.digest = digest;
            } else if (isOriginalNode) {
                // Digest differs: break the rename link.
                trackMap.remove(keyFound);
                trackMap.put(path, new PathNode<>(path, path, digest));
            } else {
                // Node is "new": try to recover an original identity if possible.
                findCandidateForDigest(digest).ifPresent(candidate -> node.originalPath = candidate);
                node.digest = digest;
                node.currentPath = path;
            }
        } else {
            // No node exists: create one, recovering original identity if possible.
            P newOriginal = findCandidateForDigest(digest).orElse(path);
            trackMap.put(path, new PathNode<>(newOriginal, path, digest));
        }
    }

    /**
     * Marks a path as deleted.
     *
     * @param path the path to delete.
     */
    public void delete(P path) {
        P originalKey = findPath(path);
        if (originalKey != null) {
            trackMap.remove(originalKey);
        }
    }

    /**
     * Renames a path from source to target.
     *
     * @param source the source path.
     * @param target the target path.
     */
    public void rename(P source, P target) {
        P key = findPath(source);
        if (key == null) {
            return;
        }
        // Remove any conflicting node with the target current path.
        P conflict = findPath(target);
        if (conflict != null && !conflict.equals(key)) {
            trackMap.remove(conflict);
        }
        PathNode<P, D> node = trackMap.get(key);
        node.currentPath = target;
    }

    /**
     * Returns a list of tracked paths.
     *
     * @return a list of tracked paths.
     */
    public List<P> paths() {
        return this.trackMap.values().stream()
            .map(PathNode::currentPath)
            .toList();
    }

    /**
     * Returns a list of the original paths.
     *
     * @return a list of original paths.
     */
    public List<P> originalPaths() {
        return new ArrayList<>(originalPaths.keySet());
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

    /**
     * Checks if a path has been newly added to the tracker.
     * <p>
     * A path is considered added if it exists in the current state of the tracker but
     * was not part of the original tracked paths.
     * </p>
     *
     * @param path the path to check.
     * @return {@code true} if the path was added, {@code false} otherwise.
     */
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
        D origDigest = originalPaths.get(path);
        if (origDigest == null) {
            return true;
        }
        calculateDigestIfNecessary(node);
        return !Objects.equals(origDigest, node.digest);
    }

    public D getDigest(P path) {
        PathNode<P, D> node = findPathNode(path);
        if (node == null) {
            return null;
        }
        return calculateDigestIfNecessary(node);
    }

    /**
     * Finds the original path corresponding to the specified key path.
     *
     * @param keyPath the key path to find.
     * @return the original path if found, {@code null} otherwise.
     */
    public P findPath(P keyPath) {
        return this.trackMap.entrySet().stream()
            .filter(entry -> entry.getValue().currentPath().equals(keyPath))
            .findFirst()
            .map(Map.Entry::getKey)
            .orElse(null);
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

        // Process original files by checking the stored original identity.
        for (P orig : originalPaths.keySet()) {
            // Search among all nodes for one whose stored originalPath equals the expected original.
            Optional<PathNode<P, D>> optNode = trackMap.values().stream()
                .filter(node -> node.originalPath().equals(orig))
                .findFirst();
            if (optNode.isEmpty()) {
                // If no node is found with the original identity, check if any node’s current path covers it.
                boolean covered = trackMap.values().stream()
                    .anyMatch(n -> n.currentPath().equals(orig));
                if (!covered) {
                    changes.add(new Change<>(ChangeType.DELETED, orig, null));
                }
            } else {
                PathNode<P, D> node = optNode.get();
                if (!node.currentPath().equals(orig)) {
                    changes.add(new Change<>(ChangeType.RENAMED, orig, node.currentPath()));
                } else {
                    // The file exists in its original location; check for modifications.
                    D origDigest = originalPaths.get(orig);
                    calculateDigestIfNecessary(node);
                    if (!Objects.equals(origDigest, node.digest)) {
                        changes.add(new Change<>(ChangeType.ADDED_OR_MODIFIED, orig, null));
                    }
                }
            }
        }

        // Process new files: nodes whose stored original identity isn’t in the original set.
        trackMap.values().stream()
            .filter(n -> !originalPaths.containsKey(n.originalPath()))
            .forEach(n -> changes.add(new Change<>(ChangeType.ADDED_OR_MODIFIED, n.currentPath(), null)));
        return changes;
    }

    public void purge() {
        this.trackMap.clear();
    }

    /**
     * Creates a deep clone of this file tracker. Be aware that the {@link #digestCalculator} of the clone will be null.
     *
     * @return a newly generated file tracker with copied state.
     */
    public MCROCFLFileTracker<P, D> deepClone() {
        Map<P, PathNode<P, D>> newTrackMap = new HashMap<>();
        for (Map.Entry<P, PathNode<P, D>> entry : this.trackMap.entrySet()) {
            PathNode<P, D> node = entry.getValue();
            newTrackMap.put(entry.getKey(), new PathNode<>(node.originalPath(), node.currentPath(), node.digest));
        }
        return new MCROCFLFileTracker<>(Collections.unmodifiableMap(this.originalPaths), newTrackMap);
    }

    private PathNode<P, D> findPathNode(P keyPath) {
        P sourcePath = findPath(keyPath);
        if (sourcePath == null) {
            return null;
        }
        return this.trackMap.get(sourcePath);
    }

    private Optional<P> findCandidateForDigest(D digest) {
        return this.originalPaths.entrySet().stream()
            .filter(e -> Objects.equals(e.getValue(), digest))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    private D calculateDigestIfNecessary(PathNode<P, D> node) {
        if (node.digest == null) {
            node.digest = this.digestCalculator.apply(node.currentPath);
        }
        return node.digest;
    }

    private static class PathNode<P, D> {

        private P originalPath;

        private P currentPath;

        private D digest;

        PathNode(P originalPath, P currentPath, D digest) {
            this.originalPath = originalPath;
            this.currentPath = currentPath;
            this.digest = digest;
        }

        public P currentPath() {
            return currentPath;
        }

        public P originalPath() {
            return originalPath;
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
