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

package org.mycore.common;

import org.junit.Test;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.util.concurrent.MCRFixedUserCallable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MCRIFSCopyTest extends MCRIFSTest {

    private MCRObject root;

    private MCRDerivate derivate;

    @Test
    public void sync() throws Exception {
        create();
        copy("/anpassbar.jpg", "anpassbar.jpg", derivate);
        copy("/nachhaltig.jpg", "nachhaltig.jpg", derivate);
        copy("/vielseitig.jpg", "vielseitig.jpg", derivate);
        assertEquals("the derivate should contain three files", 3,
            Files.list(MCRPath.getPath(derivate.getId().toString(), "/")).count());
    }

    @Test
    public void async() throws Exception {
        // create derivate
        create();
        MCRSessionMgr.getCurrentSession().commitTransaction();

        // execute threads
        MCRSystemUserInformation systemUser = MCRSystemUserInformation.getSystemUserInstance();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Future<Exception> future1 = executorService
            .submit(new MCRFixedUserCallable<>(new CopyTask("anpassbar.jpg", derivate), systemUser));
        Future<Exception> future2 = executorService
            .submit(new MCRFixedUserCallable<>(new CopyTask("nachhaltig.jpg", derivate), systemUser));
        Future<Exception> future3 = executorService
            .submit(new MCRFixedUserCallable<>(new CopyTask("vielseitig.jpg", derivate), systemUser));
        assertNull(future1.get(5, TimeUnit.SECONDS));
        assertNull(future2.get(5, TimeUnit.SECONDS));
        assertNull(future3.get(5, TimeUnit.SECONDS));
        assertEquals("the derivate should contain three files", 3,
            Files.list(MCRPath.getPath(derivate.getId().toString(), "/")).count());

        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }

    public void create() throws Exception {
        root = createObject();
        derivate = createDerivate(root.getId());
        MCRMetadataManager.create(root);
        MCRMetadataManager.create(derivate);
    }

    @Override
    public void tearDown() throws Exception {
        MCRMetadataManager.delete(derivate);
        MCRMetadataManager.delete(root);
        super.tearDown();
    }

    public static void copy(String from, String to, MCRDerivate derivate) throws IOException {
        try (InputStream fileInputStream = MCRIFSTest.class.getResourceAsStream(from)) {
            assertNotNull("cannot find file " + from, fileInputStream);
            Files.copy(fileInputStream, MCRPath.getPath(derivate.toString(), to));
        }
    }

    private static class CopyTask implements Callable<Exception> {

        private String fileName;

        private MCRDerivate derivate;

        CopyTask(String fileName, MCRDerivate derivate) {
            this.fileName = fileName;
            this.derivate = derivate;
        }

        @Override
        public Exception call() throws Exception {
            try {
                MCRIFSCopyTest.copy("/" + fileName, fileName, derivate);
                return null;
            } catch (Exception exc) {
                return exc;
            }
        }

    }

}
