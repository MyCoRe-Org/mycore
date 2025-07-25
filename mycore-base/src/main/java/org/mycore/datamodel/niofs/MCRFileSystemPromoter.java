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

package org.mycore.datamodel.niofs;

import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import jakarta.servlet.ServletContext;

/**
 * This {@link AutoExecutable} checks if the {@link FileSystem} implementations are available.
 * <p>
 * There is a documented "feature" in OpenJDK 8 that only {@link FileSystemProvider}
 * available to the system {@link ClassLoader} are available.
 * We try to fix (a.k.a. hack) it right.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileSystemPromoter implements AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getName()
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#getPriority()
     */
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(jakarta.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            Set<String> installedSchemes = FileSystemProvider.installedProviders()
                .stream()
                .map(FileSystemProvider::getScheme)
                .collect(Collectors.toCollection(HashSet::new));
            ServiceLoader<FileSystemProvider> sl = ServiceLoader.load(FileSystemProvider.class, Thread.currentThread()
                .getContextClassLoader());
            promoteFileSystemProvider(StreamSupport.stream(sl.spliterator(), false)
                .filter(p -> !installedSchemes.contains(p.getScheme()))
                .collect(Collectors.toCollection(ArrayList::new)));
        }
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    //Method is used. Warning is result of a bug in PMD
    private void promoteFileSystemProvider(List<FileSystemProvider> detectedProviders) {
        if (detectedProviders.isEmpty()) {
            return;
        }
        for (FileSystemProvider provider : detectedProviders) {
            LOGGER.info("Promoting filesystem {}: {}",
                provider::getScheme, () -> provider.getClass().getCanonicalName());
            MCRPaths.addFileSystemProvider(provider);
        }
    }

}
