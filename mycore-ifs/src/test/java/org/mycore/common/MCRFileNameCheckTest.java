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

package org.mycore.common;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.access.MCRAccessException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRFileNameCheckTest extends MCRIFSTest {

    private MCRDerivate derivate;

    @BeforeEach
    public void setup() throws MCRAccessException {
        MCRObject root = createObject();
        derivate = createDerivate(root.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @Test
    public void checkIllegalWindowsFileName() throws IOException {
        final MCRPath aux = MCRPath.getPath(derivate.toString(), "aux");
        assertThrowsExactly(IOException.class, () -> Files.createFile(aux));
    }

    @Test
    public void checkIllegalFileName() throws IOException {
        final MCRPath info = MCRPath.getPath(derivate.toString(), "info@mycore.de");
        assertThrowsExactly(IOException.class, () -> Files.createFile(info));
    }

    @Test
    public void checkIllegalDirectoryName() throws IOException {
        final MCRPath dirName = MCRPath.getPath(derivate.toString(), "Nur ein \"Test\"");
        assertThrowsExactly(IOException.class, () -> Files.createDirectory(dirName));
    }

}
