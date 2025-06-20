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

package org.mycore.access;

import java.io.Serial;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRCatchException;

public sealed abstract class MCRAccessException extends MCRCatchException
    permits MCRMissingPermissionException, MCRMissingPrivilegeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String action;

    private final String id;

    private final String permission;

    protected MCRAccessException(String action, String id, String permission, String... privilege) {
        super(getMessage(action, id, permission, privilege));
        this.action = action;
        this.id = id;
        this.permission = permission;
    }

    private static String getMessage(String action, String oid, String permission, String... privilege) {
        StringBuilder sb = new StringBuilder();
        switch (privilege.length) {
            case 0 ->
                //no privilege but permission was missing
                sb.append("You do not have the permission '").append(permission).append("' on '").append(oid)
                    .append('\'');
            case 1 -> sb.append("You do not have the privilege '").append(privilege[0]).append('\'');
            default -> sb.append(
                Stream.of(privilege).collect(
                    Collectors.joining("', '", "You do not have any of the required privileges ('", "')")));
        }
        sb.append(action != null ? (" to perform: " + action) : ".");
        return sb.toString();
    }

    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<String> getPermission() {
        return Optional.ofNullable(permission);
    }

}
