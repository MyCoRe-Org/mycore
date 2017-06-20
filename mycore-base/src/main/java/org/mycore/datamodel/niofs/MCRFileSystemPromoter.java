/*
 * $Id$
 * $Revision: 5697 $ $Date: Jul 22, 2014 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.niofs;

import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * This {@link AutoExecutable} checks if the {@link FileSystem} implementations are available.
 * 
 * There is a documented "feature" in OpenJDK 8 that only {@link FileSystemProvider} available to the system {@link ClassLoader} are available.
 * We try to fix (a.k.a. hack) it right.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileSystemPromoter implements AutoExecutable {

    private static Logger LOGGER = LogManager.getLogger(MCRFileSystemPromoter.class);

    /**
     * 
     */
    public MCRFileSystemPromoter() {
    }

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
     * @see org.mycore.common.events.MCRStartupHandler.AutoExecutable#startUp(javax.servlet.ServletContext)
     */
    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            HashSet<String> installedSchemes = FileSystemProvider.installedProviders()
                .stream()
                .map(FileSystemProvider::getScheme)
                .collect(Collectors.toCollection(HashSet::new));
            ServiceLoader<FileSystemProvider> sl = ServiceLoader.load(FileSystemProvider.class, getClass()
                .getClassLoader());
            promoteFileSystemProvider(StreamSupport.stream(sl.spliterator(), false)
                .filter(p -> !installedSchemes.contains(p.getScheme()))
                .collect(Collectors.toCollection(LinkedList::new)));
        }
    }

    private void promoteFileSystemProvider(List<FileSystemProvider> detectedProviders) {
        if (detectedProviders.isEmpty()) {
            return;
        }
        for (FileSystemProvider provider : detectedProviders) {
            LOGGER.info("Promoting filesystem " + provider.getScheme() + ": " + provider.getClass().getCanonicalName());
            MCRPaths.addFileSystemProvider(provider);
        }
    }

}
