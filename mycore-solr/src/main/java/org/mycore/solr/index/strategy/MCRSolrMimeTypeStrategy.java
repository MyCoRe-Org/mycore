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

package org.mycore.solr.index.strategy;

import static org.mycore.solr.MCRSolrConstants.CONFIG_PREFIX;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLFunctions;

/**
 * Strategy that depends on a files mime type. By default images are
 * ignored. You can use the MCR.Module-solr.MimeTypeStrategy.Pattern property to
 * set an application specific pattern. Be aware that this is the ignore
 * pattern, the {@link #check(Path, BasicFileAttributes)} method will return false if it
 * matches.
 * 
 * @author Matthias Eichner
 * @author Thomas Scheffler (yagee)
 */
public class MCRSolrMimeTypeStrategy implements MCRSolrFileStrategy {

    private static final Pattern IGNORE_PATTERN;

    static {
        String acceptPattern = MCRConfiguration.instance().getString(CONFIG_PREFIX + "MimeTypeStrategy.Pattern");
        IGNORE_PATTERN = Pattern.compile(acceptPattern);
    }

    @Override
    public boolean check(Path file, BasicFileAttributes attrs) {
        String mimeType = MCRXMLFunctions.getMimeType(file.getFileName().toString());
        return !IGNORE_PATTERN.matcher(mimeType).matches();
    }

}
