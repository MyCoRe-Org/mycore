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
 * {@link MCRSystemUserInformationProvider} is an implementation of {@link MCRUserInformationProvider} that that looks
 * up user information of type {@link MCRSystemUserInformation} as defined in {@link MCRSystemUserInformation}.
 * <p>
 * Possible user IDs are:
 * <ul>
 * <li> {@link MCRSystemUserInformationProvider#GUEST}
 * <li >{@link MCRSystemUserInformationProvider#JANITOR}
 * <li> {@link MCRSystemUserInformationProvider#SYSTEM_USER}
 * <li >{@link MCRSystemUserInformationProvider#SUPER_USER}
 * </ul>
 * No configuration options are available, if configured automatically.
 * <p>
 * Example:
 * <pre>
 * [...].Class=org.mycore.common.MCRSystemUserInformationProvider
 * </pre>
 */
public final class MCRSystemUserInformationProvider implements MCRUserInformationProvider {

    public static final String GUEST = "GUEST";

    public static final String JANITOR = "JANITOR";

    public static final String SYSTEM_USER = "SYSTEM_USER";

    public static final String SUPER_USER = "SUPER_USER";

    @Override
    public Optional<MCRUserInformation> get(String userId) {
        return switch (userId) {
            case GUEST -> Optional.of(MCRSystemUserInformation.getGuestInstance());
            case JANITOR -> Optional.of(MCRSystemUserInformation.getJanitorInstance());
            case SYSTEM_USER -> Optional.of(MCRSystemUserInformation.getSystemUserInstance());
            case SUPER_USER -> Optional.of(MCRSystemUserInformation.getSuperUserInstance());
            default -> Optional.empty();
        };
    }

}
