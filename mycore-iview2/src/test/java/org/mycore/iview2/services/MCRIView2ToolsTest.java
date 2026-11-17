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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.MCRTestCase;

public class MCRIView2ToolsTest extends MCRTestCase {

    private static final String ENTRY_NAME = "imageinfo.xml";

    private static final String ENTRY_CONTENT = "<imageinfo />";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetFileSystemAllowsConcurrentAccess() throws Exception {
        Path iviewFile = temporaryFolder.newFile("test.iview2").toPath();
        try (OutputStream out = Files.newOutputStream(iviewFile); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(ENTRY_NAME));
            zip.write(ENTRY_CONTENT.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (FileSystem first = MCRIView2Tools.getFileSystem(iviewFile)) {
            Future<String> concurrentRead = executor.submit(() -> {
                try (FileSystem second = MCRIView2Tools.getFileSystem(iviewFile)) {
                    return Files.readString(second.getPath("/", ENTRY_NAME));
                }
            });
            try {
                Assert.assertEquals(ENTRY_CONTENT, concurrentRead.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                Assert.fail("Second reader is blocked while the first reader keeps the .iview2 file open.");
            }
            Assert.assertEquals(ENTRY_CONTENT, Files.readString(first.getPath("/", ENTRY_NAME)));
        } finally {
            executor.shutdownNow();
        }
    }

}
