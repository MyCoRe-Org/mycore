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

package org.mycore.access;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.mycore.common.MCRUserInformation;

import javax.inject.Singleton;

/**
 * Can be used to write Tests against the {@link MCRAccessManager}.
 * Just add MCR.Access.Class with MCRAccessMock.class.getName() to the test-properties in the overwritten
 * {@link org.mycore.common.MCRTestCase} getTestProperties() method.
 */
@Singleton
public class MCRAccessMock implements MCRAccessInterface {

    private static final List<MCRCheckPermissionCall> calls = new LinkedList<>();
    private static boolean checkPermissionReturn = true;


    public static List<MCRCheckPermissionCall> getCheckPermissionCalls() {
        return Collections.unmodifiableList(calls);
    }

    public static void clearCheckPermissionCallsList(){
        calls.clear();
    }

    public static boolean getMethodResult() {
        return checkPermissionReturn;
    }

    public static void setMethodResult(boolean methodResult) {
        checkPermissionReturn = methodResult;
    }


    @Override
    public boolean checkPermission(String permission) {
        calls.add(new MCRCheckPermissionCall(null, permission));
        return checkPermissionReturn;    }

    @Override
    public boolean checkPermission(String id, String permission) {
        calls.add(new MCRCheckPermissionCall(id, permission));
        return checkPermissionReturn;
    }

    @Override
    public boolean checkPermissionForUser(String permission, MCRUserInformation userInfo) {
        return checkPermission(permission);
    }

    @Override
    public boolean checkPermission(String id, String permission, MCRUserInformation userInfo) {
        return checkPermission(id, permission);
    }

    public static class MCRCheckPermissionCall {

        private final String id;

        private final String permission;

        private MCRCheckPermissionCall(String id, String permission) {
            this.id = id;
            this.permission = Objects.requireNonNull(permission);
        }

        public String getId() {
            return id;
        }

        public String getPermission() {
            return permission;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MCRCheckPermissionCall that = (MCRCheckPermissionCall) o;
            return Objects.equals(id, that.id) && permission.equals(that.permission);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, permission);
        }
    }
}
