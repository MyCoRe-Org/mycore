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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRPathMatcher implements PathMatcher {

    private final Pattern pattern;

    public MCRPathMatcher(final String regex) {
        pattern = getPattern(regex);
    }

    @Override
    public boolean matches(final Path path) {
        return pattern.matcher(path.toString()).matches();
    }

    protected Pattern getPattern(final String pattern) {
        return Pattern.compile(pattern);
    }

}
