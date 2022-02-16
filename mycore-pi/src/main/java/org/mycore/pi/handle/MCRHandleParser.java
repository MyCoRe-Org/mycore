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

package org.mycore.pi.handle;

import java.util.Optional;

import org.mycore.pi.MCRPIParser;

public class MCRHandleParser implements MCRPIParser<MCRHandle> {

    public static final String DIRECTORY_INDICATOR = "10.";

    @Override
    public Optional<MCRHandle> parse(String handle) {
        String[] handleParts = handle.split("/", 2);

        if (handleParts.length != 2) {
            return Optional.empty();
        }

        String prefix = handleParts[0];
        String suffix = handleParts[1];

        if (suffix.length() == 0) {
            return Optional.empty();
        }

        return Optional.of(new MCRHandle(prefix, suffix));
    }
}
