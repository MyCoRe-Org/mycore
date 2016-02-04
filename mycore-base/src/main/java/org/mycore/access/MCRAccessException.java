package org.mycore.access;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRCatchException;

public class MCRAccessException extends MCRCatchException {

    private static final long serialVersionUID = 6494399676882465653L;

    private Optional<String> action;

    private Optional<String> id;

    private Optional<String> permission;

    public static MCRAccessException missingPrivilege(String action, String... privilege) {
        return new MCRAccessException(Optional.ofNullable(action), null, null, privilege);
    }

    public static MCRAccessException missingPermission(String action, String id, String permission) {
        return new MCRAccessException(Optional.ofNullable(action), id, permission);
    }

    private MCRAccessException(Optional<String> action, String id, String permission, String... privilege) {
        super(getMessage(action, id, permission, privilege));
        this.action = action;
        this.id = Optional.ofNullable(id);
        this.permission = Optional.ofNullable(permission);
    }

    private static String getMessage(Optional<String> action, String oid, String permission, String... privilege) {
        StringBuilder sb = new StringBuilder();
        switch (privilege.length) {
            case 0:
                //no privilige but permission was missing
                sb.append("You do not have the permission '").append(permission).append("' on '").append(oid)
                    .append('\'');
                break;
            case 1:
                sb.append("You do not have the privilege '").append(privilege[0]).append('\'');
                break;
            default:
                sb.append(
                    Stream.of(privilege).collect(
                        Collectors.joining("', '", "You do not have any of the required privileges ( '", ")")));
                break;
        }
        sb.append(
            action.map(s -> " to perfom: " + s).orElse("."));
        return sb.toString();
    }

    public Optional<String> getAction() {
        return action;
    }

    public Optional<String> getId() {
        return id;
    }

    public Optional<String> getPermission() {
        return permission;
    }

}
