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

package org.mycore.webtools.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;

/**
 * MCR.Upload.TempDirectory.Delete.Failed = Dass Löschen eines temporären Ordner ist gescheitert!
 * MCR.Upload.TempDirectory.Create.Failed = Das Erstellen eines temporären Ordner ist gescheitert!
 * MCR.Upload.UploadHandlerNotSupported = Upload Handler werden aktuell nicht unterstützt!
 *
 */
public class MCRFileUploadBucket {

    private static final ConcurrentHashMap<String, MCRFileUploadBucket> bucketMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, List<MCRFileUploadBucket>> sessionIDBucketMap = new ConcurrentHashMap<>();

    private String bucketID;

    private String objectID;

    private Path root;

    /**
     *
     * @param bucketID of the bucket
     * @param objectID the id of the mycore metadata object or derivate id
     */
    private MCRFileUploadBucket(String bucketID, String objectID) {
        this.bucketID = bucketID;
        this.objectID = objectID;

        try {
            root = Files.createTempDirectory("mycore_" + bucketID);
        } catch (IOException e) {
            throw new MCRUploadException("MCR.Upload.TempDirectory.Create.Failed", e);
        }
    }

    public static MCRFileUploadBucket getBucket(String bucketID) {
        return bucketMap.get(bucketID);
    }

    public synchronized static MCRFileUploadBucket getOrCreateBucket(String bucketID, String objectID) {
        final MCRFileUploadBucket mcrFileUploadBucket = bucketMap
            .computeIfAbsent(bucketID, (id) -> new MCRFileUploadBucket(bucketID, objectID));
        final List<MCRFileUploadBucket> list = sessionIDBucketMap
            .computeIfAbsent(bucketID, (id) -> new ArrayList<>());
        list.add(mcrFileUploadBucket);

        return mcrFileUploadBucket;
    }

    public static synchronized void releaseBucket(String bucketID) {
        if (bucketMap.containsKey(bucketID)) {
            final MCRFileUploadBucket bucket = bucketMap.get(bucketID);
            if (Files.exists(bucket.root)) {
                try {
                    Files.walkFileTree(bucket.root, MCRRecursiveDeleter.instance());
                } catch (IOException e) {
                    throw new MCRUploadException("MCR.Upload.TempDirectory.Delete.Failed", e);
                }
            }
            bucketMap.remove(bucketID);
        }
    }

    public static void cleanBuckets() {
        sessionIDBucketMap.forEach((sessionID, buckets) -> {
            if (MCRSessionMgr.getSession(sessionID) == null) {
                buckets.stream().map(MCRFileUploadBucket::getBucketID).forEach(MCRFileUploadBucket::releaseBucket);
            }
        });
    }

    public String getBucketID() {
        return bucketID;
    }

    public String getObjectID() {
        return objectID;
    }

    public Path getRoot() {
        return root;
    }
}
