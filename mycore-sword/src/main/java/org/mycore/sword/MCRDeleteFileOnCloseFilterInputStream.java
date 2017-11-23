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

package org.mycore.sword;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MCRDeleteFileOnCloseFilterInputStream extends FilterInputStream {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path fileToDelete;

    public MCRDeleteFileOnCloseFilterInputStream(InputStream source, Path fileToDelete) {
        super(source);
        this.fileToDelete = fileToDelete;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } catch (IOException e) {
            throw e;
        } finally {
            LOGGER.info("Delete File : {}", fileToDelete);
            Files.delete(fileToDelete);
        }
    }
}
