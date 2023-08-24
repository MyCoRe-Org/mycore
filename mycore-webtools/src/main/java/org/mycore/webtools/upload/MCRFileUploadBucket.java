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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.datamodel.niofs.utils.MCRRecursiveDeleter;
import org.mycore.webtools.upload.exception.MCRUploadServerException;

/**
 * A MCRFileUploadBucket is a temporary directory for file uploads. It is created on demand and deleted when the session
 * is closed or the application is shut down or the bucket is closed.
 * The bucket is identified by a bucketID. It also contains a root directory, where the uploaded files are stored,
 * the parameters and the upload handler used for the upload.
 *
 */
public class MCRFileUploadBucket implements MCRSessionListener, MCRShutdownHandler.Closeable {

    private static final ConcurrentHashMap<String, MCRFileUploadBucket> BUCKET_MAP = new ConcurrentHashMap<>();

    private final String bucketID;

    private Path root;

    private String sessionID;

    private final Map<String, List<String>> parameters;

    private final MCRUploadHandler uploadHandler;

    /**
     *
     * @param bucketID of the bucket
     */
    private MCRFileUploadBucket(String bucketID, Map<String, List<String>> parameters, MCRUploadHandler uploadHandler)
        throws MCRUploadServerException {
        this.bucketID = bucketID;
        this.parameters = parameters;
        this.uploadHandler = uploadHandler;
        sessionID = MCRSessionMgr.getCurrentSessionID();

        try {
            root = Files.createTempDirectory("mycore_" + bucketID);
        } catch (IOException e) {
            throw new MCRUploadServerException("component.webtools.upload.temp.create.failed", e);
        }

        MCRSessionMgr.addSessionListener(this);
        MCRShutdownHandler.getInstance().addCloseable(this);
    }

    public static MCRFileUploadBucket getBucket(String bucketID) {
        return BUCKET_MAP.get(bucketID);
    }

    public static synchronized MCRFileUploadBucket createBucket(String bucketID,
        Map<String, List<String>> parameters,
        MCRUploadHandler uploadHandler) throws MCRUploadServerException {
        try {
            return BUCKET_MAP.computeIfAbsent(bucketID, (id) -> {
                try {
                    return new MCRFileUploadBucket(id, parameters, uploadHandler);
                } catch (MCRUploadServerException e) {
                    throw new MCRException(e);
                }
            });
        } catch (MCRException e) {
            if (e.getCause() instanceof MCRUploadServerException use) {
                throw use;
            }
            throw e;
        }
    }

    public static synchronized void releaseBucket(String bucketID) throws MCRUploadServerException {
        if (BUCKET_MAP.containsKey(bucketID)) {
            final MCRFileUploadBucket bucket = BUCKET_MAP.get(bucketID);
            if (Files.exists(bucket.root)) {
                try {
                    Files.walkFileTree(bucket.root, MCRRecursiveDeleter.instance());
                } catch (IOException e) {
                    throw new MCRUploadServerException("component.webtools.upload.temp.delete.failed", e);
                }
            }
            BUCKET_MAP.remove(bucketID);
        }
    }

    public String getBucketID() {
        return bucketID;
    }

    public Map<String, List<String>> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public MCRUploadHandler getUploadHandler() {
        return uploadHandler;
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        if (event.getType().equals(MCRSessionEvent.Type.destroyed)) {
            final String sessionID = event.getSession().getID();
            if (sessionID.equals(this.sessionID)) {
                close();
            }
        }
    }

    @Override
    public void close() {
        try {
            releaseBucket(this.bucketID);
        } catch (MCRUploadServerException e) {
            throw new MCRException("Error while releasing bucket on close", e);
        }
    }
}
