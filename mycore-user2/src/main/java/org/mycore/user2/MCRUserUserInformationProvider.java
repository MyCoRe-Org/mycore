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

package org.mycore.user2;

import java.util.Optional;

import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationProvider;

/**
 * A {@link MCRUserUserInformationProvider} is a {@link MCRUserInformationProvider} that has the scheme
 * {@link MCRUserUserInformationProvider#SCHEMA} and looks up user information with 
 * {@link MCRUserManager#getUser(String)}.
 */
public final class MCRUserUserInformationProvider implements MCRUserInformationProvider {

    public static final String SCHEMA = "user";

    @Override
    public String getSchema() {
        return SCHEMA;
    }

    @Override
    public Optional<MCRUserInformation> get(String userId) {
        return Optional.ofNullable(MCRUserManager.getUser(userId));
    }

}
