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

package org.mycore.ocfl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.niofs.MCRVersionedPath;
import org.mycore.ocfl.repository.MCROCFLHashRepositoryProvider;
import org.mycore.ocfl.repository.MCROCFLRepository;
import org.mycore.ocfl.repository.MCROCFLRepositoryBuilder;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;

import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.SizeDigestAlgorithm;
import io.ocfl.api.model.VersionInfo;

public abstract class MCROCFLTestCaseHelper {

    public static MCROCFLRepository setUp(boolean remote) throws IOException {
        String repositoryId = MCRConfiguration2.getString("MCR.Content.Manager.Repository").orElseThrow();
        MCROCFLRepositoryProvider repositoryProvider = MCROCFLRepositoryProvider.getProvider(repositoryId);
        if (!(repositoryProvider instanceof RepositoryProviderMock repositoryProviderMock)) {
            throw new MCROCFLException("Invalid provider. Should be RepositoryProviderMock, but is "
                + repositoryProvider.getClass().getSimpleName());
        }
        repositoryProviderMock.setRemote(remote);
        repositoryProviderMock.init(MCROCFLRepositoryProvider.REPOSITORY_PROPERTY_PREFIX + repositoryId);
        return repositoryProvider.getRepository();
    }

    public static void tearDown(MCROCFLRepository repository) {
        for (String objectId : repository.listObjectIds().toList()) {
            repository.purgeObject(objectId);
        }
    }

    public static ObjectVersionId writeFile(MCROCFLRepository repository, String ocflObjectId, String fileName,
        String data) {
        return repository.updateObject(ObjectVersionId.head(ocflObjectId),
            new VersionInfo().setMessage("write"), (updater) -> {
                byte[] bytes = data.getBytes();
                int size = bytes.length;
                updater
                    .writeFile(new ByteArrayInputStream(bytes), fileName)
                    .addFileFixity(fileName, new SizeDigestAlgorithm(), String.valueOf(size));
            });
    }

    public static void loadDerivate(String derivateId) throws URISyntaxException, IOException {
        URL derivateURL = MCROCFLTestCaseHelper.class.getClassLoader().getResource(derivateId);
        if (derivateURL == null) {
            throw new IllegalArgumentException("Unable to locate '" + derivateId + "' folder in resources.");
        }
        final Path sourcePath = Path.of(derivateURL.toURI());
        final MCRVersionedPath targetPath = MCRVersionedPath.head(derivateId, "/");
        Files.walkFileTree(sourcePath, new CopyFileVisitor(targetPath));
    }

    public static class RepositoryProviderMock extends MCROCFLHashRepositoryProvider {

        private boolean remote;

        public void setRemote(boolean remote) {
            this.remote = remote;
        }

        @Override
        protected MCROCFLRepository buildRepository(String id) {
            return new MCROCFLRepositoryBuilder()
                .id(id)
                .remote(this.remote)
                .defaultLayoutConfig(getExtensionConfig())
                .storage(this::configureStorage)
                .workDir(workDir)
                .buildMCR();
        }

    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path targetPath;

        private Path sourcePath = null;

        CopyFileVisitor(Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            if (sourcePath == null) {
                sourcePath = dir;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }

    }

}
