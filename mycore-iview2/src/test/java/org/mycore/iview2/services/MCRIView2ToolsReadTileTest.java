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
package org.mycore.iview2.services;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRIView2ToolsReadTileTest {

    @TempDir
    Path temporaryFolder;

    @Test
    public void testReadTileResetsReaderOnError() throws Exception {
        Path iviewFileRoot = Files.createDirectory(temporaryFolder.resolve("iview2"));
        Path brokenTile = iviewFileRoot.resolve("0/0/0.jpg");
        Files.createDirectories(brokenTile.getParent());
        Files.write(brokenTile, "this is not a JPEG file".getBytes(StandardCharsets.UTF_8));

        ImageReader imageReader = MCRIView2Tools.getTileImageReader();
        try {
            assertThrows(IOException.class, () -> MCRIView2Tools.readTile(iviewFileRoot, imageReader, 0, 0, 0));
            assertNull(imageReader.getInput(), "ImageReader still holds the input of the failed tile.");
        } finally {
            imageReader.dispose();
        }
    }

}
