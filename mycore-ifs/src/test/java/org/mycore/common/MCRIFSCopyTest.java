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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;

public class MCRIFSCopyTest extends MCRIFSTest {

    private MCRObject root;

    private MCRDerivate derivate;

    @Test
    public void sync() throws Exception {
        copy("/anpassbar.jpg", "anpassbar.jpg", derivate);
        copy("/nachhaltig.jpg", "nachhaltig.jpg", derivate);
        copy("/vielseitig.jpg", "vielseitig.jpg", derivate);
        try (Stream<Path> streamPath = Files.list(MCRPath.getPath(derivate.getId().toString(), "/"))) {
            assertEquals(3, streamPath.count(), "the derivate should contain three files");
        }
    }

    @Test
    public void async() throws Exception {
        MCRSessionMgr.getCurrentSession();
        MCRTransactionManager.commitTransactions();

        // execute threads
        MCRSystemUserInformation systemUser = MCRSystemUserInformation.SYSTEM_USER;
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture.allOf(
            CompletableFuture.runAsync(copy("anpassbar.jpg", derivate), executorService),
            CompletableFuture.runAsync(copy("nachhaltig.jpg", derivate), executorService),
            CompletableFuture.runAsync(copy("vielseitig.jpg", derivate), executorService))
            .get(5, TimeUnit.SECONDS);
        try (Stream<Path> streamPath = Files.list(MCRPath.getPath(derivate.getId().toString(), "/"))) {
            assertEquals(3, streamPath.count(), "the derivate should contain three files");
        }

        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    private void throwException(Exception e) throws Exception {
        if (e != null) {
            throw e;
        }
    }

    @BeforeEach
    public void create() throws Exception {
        root = createObject();
        derivate = createDerivate(root.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @AfterEach
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(root);
    }

    public static void copy(String from, String to, MCRDerivate derivate) throws IOException {
        try (InputStream fileInputStream = MCRIFSTest.class.getResourceAsStream(from)) {
            assertNotNull(fileInputStream, "cannot find file " + from);
            Files.copy(fileInputStream, MCRPath.getPath(derivate.toString(), to));
        }
    }

    public static Runnable copy(String fileName, MCRDerivate derivate) throws UncheckedIOException {
        return () -> {
            try {
                copy("/" + fileName, fileName, derivate);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

}
