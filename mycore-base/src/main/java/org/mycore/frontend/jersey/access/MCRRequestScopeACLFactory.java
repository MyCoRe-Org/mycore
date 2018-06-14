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

package org.mycore.frontend.jersey.access;

import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSystemUserInformation;

public class MCRRequestScopeACLFactory implements Factory<MCRRequestScopeACL> {

    public static final ACLBinder BINDER = new ACLBinder();

    public static Binder getBinder() {
        return BINDER;
    }

    @Override
    public void dispose(MCRRequestScopeACL mcrRequestScopeACL) {
        LogManager.getLogger().debug("Disposed...");
    }

    @Override
    public MCRRequestScopeACL provide() {
        return new MCRRequestScopeACLImpl();
    }

    private static class MCRRequestScopeACLImpl implements MCRRequestScopeACL {
        public static final MCRSystemUserInformation GUEST = MCRSystemUserInformation.getGuestInstance();

        private boolean isPrivate;

        public MCRRequestScopeACLImpl() {
            LogManager.getLogger().debug("Constructor called");
            this.isPrivate = false;
        }

        @Override
        public boolean checkPermission(String privilege) {
            if (!isPrivate()) {
                MCRAccessManager.checkPermission(GUEST, () -> MCRAccessManager.checkPermission(privilege))
                    .thenAccept(b -> {
                        if (!b) {
                            isPrivate = true; //not for guest user
                        }
                    }).join();
            }
            if (isPrivate()) {
                return MCRAccessManager.checkPermission(privilege);
            }
            return true;
        }

        @Override
        public boolean checkPermission(String id, String permission) {
            if (!isPrivate()) {
                MCRAccessManager.checkPermission(GUEST, () -> MCRAccessManager.checkPermission(id, permission))
                    .thenAccept(b -> {
                        if (!b) {
                            isPrivate = true;
                            LogManager.getLogger().debug("response is private");
                        }
                    }).join();
            }
            if (isPrivate()) {
                return MCRAccessManager.checkPermission(id, permission);
            }
            return true;
        }

        @Override
        public boolean isPrivate() {
            LogManager.getLogger().debug("isPrivate={}", isPrivate);
            return isPrivate;
        }
    }

    private static class ACLBinder extends AbstractBinder {
        @Override
        protected void configure() {
            bindFactory(MCRRequestScopeACLFactory.class)
                .proxy(true)
                .proxyForSameScope(false)
                .to(MCRRequestScopeACL.class)
                .in(RequestScoped.class);
        }

    }
}
