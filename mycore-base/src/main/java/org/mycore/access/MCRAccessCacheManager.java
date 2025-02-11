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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRScopedSession;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;

/**
 * @author Thomas Scheffler (yagee)
 */
class MCRAccessCacheManager {

    private static final int CAPACITY = MCRConfiguration2.getOrThrow("MCR.Access.Cache.Size", Integer::valueOf);

    private static final String SESSION_PERMISSION_KEY = MCRAccessCacheManager.class.getCanonicalName();

    @SuppressWarnings("unchecked")
    private MCRCache<MCRPermissionHandle, Boolean> getSessionPermissionCache() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        return (MCRCache<MCRPermissionHandle, Boolean>) session.computeIfAbsent(
            SESSION_PERMISSION_KEY, k -> createCache(session));
    }

    @SuppressWarnings("unchecked")
    private MCRCache<MCRPermissionHandle, Boolean> getCacheFromSession(MCRSession session) {
        return (MCRCache<MCRPermissionHandle, Boolean>) session.get(SESSION_PERMISSION_KEY);
    }

    private MCRCache<MCRPermissionHandle, Boolean> createCache(MCRSession session) {
        Object scopedSessionHint = session.get(MCRScopedSession.SCOPED_HINT);
        String suffix = scopedSessionHint == null ? session.getID() : session.getID() + ",scope=" + UUID.randomUUID();
        return new MCRCache<>(CAPACITY, "Access rights,MCRSession=" + suffix);
    }

    public Boolean isPermitted(String id, String permission) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = getSessionPermissionCache();
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        return permissionCache.getIfUpToDate(handle, currentSession.getLoginTime());
    }

    public void cachePermission(String id, String permission, boolean permitted) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        getSessionPermissionCache().put(handle, permitted);
    }

    public void removePermission(String id, String permission) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = getSessionPermissionCache();
        permissionCache.remove(handle);
    }

    public void removePermission(String... ids) {
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = getSessionPermissionCache();
        removePermissionFromCache(permissionCache, Stream.of(ids).collect(Collectors.toSet()));
    }

    private void removePermissionFromCache(MCRCache<MCRPermissionHandle, Boolean> permissionCache, Set<String> ids) {
        final List<MCRPermissionHandle> handlesToRemove = permissionCache.keys()
            .stream()
            .filter(hdl -> hdl.id() != null)
            .filter(hdl -> ids.contains(hdl.id()))
            .toList();
        handlesToRemove.forEach(permissionCache::remove);
    }

    public void removePermissionFromAllCachesById(String... ids) {
        final Set<String> idSet = Stream.of(ids).collect(Collectors.toSet());
        MCRSessionMgr.getAllSessions().forEach((sessionId, mcrSession) -> {
            final MCRCache<MCRPermissionHandle, Boolean> cache = getCacheFromSession(mcrSession);
            if (cache != null) {
                removePermissionFromCache(cache, idSet);
            }
        });
    }

    private record MCRPermissionHandle(String id, String permission) {
        private MCRPermissionHandle {
            permission = permission.intern();
        }
    }

}
