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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.webtools.upload.exception.MCRInvalidFileException;
import org.mycore.webtools.upload.exception.MCRInvalidUploadParameterException;
import org.mycore.webtools.upload.exception.MCRMissingParameterException;
import org.mycore.webtools.upload.exception.MCRUploadForbiddenException;
import org.mycore.webtools.upload.exception.MCRUploadServerException;

/**
 * Handles uploaded {@link MCRFileUploadBucket}s
 */
public interface MCRUploadHandler {

    /**
     * Checks if the upload is allowed for the current user and if the parameters are valid
     * @param parameters the parameters that were passed to the upload, eg. the {@link MCRObjectID}
     * @throws MCRUploadForbiddenException if the upload is not allowed
     * @throws MCRUploadServerException  if something went wrong serverside while begin
     */
    String begin(Map<String, List<String>> parameters)
        throws MCRUploadForbiddenException, MCRUploadServerException,
        MCRMissingParameterException, MCRInvalidUploadParameterException;

    /**
     * Traverses the given {@link MCRFileUploadBucket} and creates or updates the corresponding
     * {@link org.mycore.datamodel.metadata.MCRObject}
     * 
     * {@link MCRMetaClassification} that should be assigned to the new
     * {@link org.mycore.datamodel.metadata.MCRDerivate}
     * @param bucket the {@link MCRFileUploadBucket} to traverse
     * @throws MCRUploadServerException  if something went wrong serverside while commiting
     */
    URI commit(MCRFileUploadBucket bucket) throws MCRUploadServerException;

    /**
     * Validates if the file name and size
     * @param name the file name
     * @param size the size of the file
     * @throws MCRUploadForbiddenException if the user is not allowed to upload the file
     * @throws MCRInvalidFileException if the file bad
     * @throws MCRUploadServerException if something went wrong serverside while uploading
     */
    void validateFileMetadata(String name, long size)
        throws MCRUploadForbiddenException, MCRInvalidFileException, MCRUploadServerException;
}
