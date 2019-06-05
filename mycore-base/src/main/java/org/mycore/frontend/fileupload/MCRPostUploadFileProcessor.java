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

package org.mycore.frontend.fileupload;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Modifies a uploaded file before it will be written to the destination Derivate.
 */
public abstract class MCRPostUploadFileProcessor {

    /**
     * Checks a file if it is processable.
     * @param path to the temp file
     * @return true if this {@link MCRPostUploadFileProcessor} can process this file
     */
    public abstract boolean isProcessable(String path);

    /**
     *
     * @param path the actual relative path in the derivate
     * @param tempFileContent the actual path to the temporary file
     * @param tempFileSupplier a supplier which creates a new temporary file which can be used for processing.
     * @return the {@link Path} of the final file, which was provided by the tempFileSupplier
     * @throws IOException if the processing failed
     */
    public abstract Path processFile(String path, Path tempFileContent, Supplier<Path> tempFileSupplier)
        throws IOException;
}
