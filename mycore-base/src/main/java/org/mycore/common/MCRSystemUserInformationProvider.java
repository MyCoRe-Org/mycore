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

package org.mycore.common;

import java.util.Optional;

/**
 * A {@link MCRSystemUserInformationProvider} is a {@link MCRUserInformationProvider} that looks up user information
 * of type {@link MCRSystemUserInformation} as defined in {@link MCRSystemUserInformation}.
 * <p>
 * Possible user IDs are:
 * <ul>
 * <li> {@link MCRSystemUserInformation#GUEST}
 * <li> {@link MCRSystemUserInformation#JANITOR}
 * <li> {@link MCRSystemUserInformation#SYSTEM_USER}
 * <li> {@link MCRSystemUserInformation#SUPER_USER}
 * </ul>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.common.MCRSystemUserInformationProvider
 * </code></pre>
 */
public final class MCRSystemUserInformationProvider implements MCRUserInformationProvider {

    @Override
    public Optional<MCRUserInformation> get(String userId) {
        try {
            return Optional.of(MCRSystemUserInformation.valueOf(userId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
