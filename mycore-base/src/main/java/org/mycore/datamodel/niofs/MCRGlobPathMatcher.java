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

package org.mycore.datamodel.niofs;

import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.regex.Pattern;

import org.mycore.common.function.MCRFunctions;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRGlobPathMatcher extends MCRPathMatcher {
    /**
     * A {@link PathMatcher} that accepts 'glob' syntax
     * @param globPattern pattern in {@link FileSystem#getPathMatcher(String) 'glob' syntax}
     */
    public MCRGlobPathMatcher(final String globPattern) {
        super(globPattern);
    }

    @Override
    protected Pattern getPattern(final String globPattern) {
        return Optional.of(globPattern)
            .map(MCRFunctions::convertGlobToRegex)
            .map(Pattern::compile)
            .get();
    }

}
