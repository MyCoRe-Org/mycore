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

package org.mycore.resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.mycore.common.config.MCRConfigurationDir;

public final class MCRFileSystemResourceHelper {

    public static Path getConfigDirTestBasePath(Class<?> testClass) {
        File configDir = Objects.requireNonNull(MCRConfigurationDir.getConfigurationDirectory());
        return configDir.toPath().resolve("test").resolve(testClass.getSimpleName());
    }

    public static Path getConfigDirResourcesTestBasePath(Class<?> testClass) {
        File configDir = Objects.requireNonNull(MCRConfigurationDir.getConfigurationDirectory());
        return configDir.toPath().resolve("resources").resolve("test").resolve(testClass.getSimpleName());
    }

    public static Path touchFiles(Path basePath, Path... relativeFilePaths) throws IOException {

        Files.createDirectories(basePath);
        boolean baseDirCreated = Files.exists(basePath);
        assert baseDirCreated;

        for (Path relativeFilePath : relativeFilePaths) {

            Path absoluteFilePath = basePath.resolve(relativeFilePath);

            Path parentDirPath = Files.createDirectories(absoluteFilePath.getParent());
            boolean parentDirCreated = Files.exists(parentDirPath);
            assert parentDirCreated;

            Path filePath = Files.createFile(absoluteFilePath);
            boolean fileCreated = Files.exists(filePath);
            assert fileCreated;

        }

        return basePath;

    }

}
