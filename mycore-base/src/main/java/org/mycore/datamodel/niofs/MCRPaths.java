/*
 * $Id$
 * $Revision: 5697 $ $Date: Jul 23, 2014 $
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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
final class MCRPaths {

    static final String DEFAULT_SCHEME_PROPERTY = "MCR.NIO.DefaultScheme";

    private MCRPaths() {
    }

    static URI getURI(String owner, String path) throws URISyntaxException {
        String scheme = MCRConfiguration.instance().getString(DEFAULT_SCHEME_PROPERTY, "ifs");
        return getURI(scheme, owner, path);
    }

    static URI getURI(String scheme, String owner, String path) throws URISyntaxException {
        String aPath = Objects.requireNonNull(path, "No 'path' specified.").charAt(0) == MCRAbstractFileSystem.SEPARATOR ? path
            : (MCRAbstractFileSystem.SEPARATOR_STRING + path);
        String finalPath = MCRAbstractFileSystem.SEPARATOR_STRING
            + Objects.requireNonNull(owner, "No 'owner' specified.") + ":" + aPath;
        return new URI(Objects.requireNonNull(scheme, "No 'scheme' specified."), null, finalPath, null);
    }

    static Path getPath(String owner, String path) {
        String scheme = MCRConfiguration.instance().getString(DEFAULT_SCHEME_PROPERTY, "ifs");
        return getPath(scheme, owner, path);
    }

    static Path getPath(String scheme, String owner, String path) {
        try {
            URI uri = getURI(scheme, owner, path);
            Logger.getLogger(MCRPaths.class).info("Generated path URI:" + uri);
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            throw new InvalidPathException(path, "URI syntax error (" + e.getMessage() + ") for path");
        }
    }
}
