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

package org.mycore.user2;

import java.util.Optional;

import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationProvider;

/**
 * A {@link MCRUserProvider} is a {@link MCRUserInformationProvider} that looks up user information
 * of type {@link MCRUser} with {@link MCRUserManager#getUser(String)}.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.user2.MCRUserProvider
 * </code></pre>
 */
public final class MCRUserProvider implements MCRUserInformationProvider {

    @Override
    public Optional<MCRUserInformation> get(String userId) {
        return Optional.ofNullable(MCRUserManager.getUser(userId));
    }

}
