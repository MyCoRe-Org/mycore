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

import org.mycore.common.MCRCache;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRAccessCacheManager implements MCRSessionListener {
    private static final int CAPACITY = MCRConfiguration2.getOrThrow("MCR.Access.Cache.Size", Integer::valueOf);

    private static String key = MCRAccessCacheManager.class.getCanonicalName();

    ThreadLocal<MCRCache<MCRPermissionHandle, Boolean>> accessCache = ThreadLocal.withInitial(() -> {
        //this is only called for every session that was created before this class could attach to session events
        MCRSession session = MCRSessionMgr.getCurrentSession();
        @SuppressWarnings("unchecked")
        MCRCache<MCRPermissionHandle, Boolean> cache = (MCRCache<MCRPermissionHandle, Boolean>) session.get(key);
        if (cache == null) {
            cache = createCache(session);
            session.put(key, cache);
        }
        return cache;
    });

    @Override
    @SuppressWarnings("unchecked")
    public void sessionEvent(MCRSessionEvent event) {
        MCRCache<MCRPermissionHandle, Boolean> cache;
        MCRSession session = event.getSession();
        switch (event.getType()) {
            case created:
            case activated:
                break;
            case passivated:
                accessCache.remove();
                break;

            case destroyed:
                cache = getCacheFromSession(session);
                if (cache != null) {
                    cache.close();
                }
                break;
            default:
                break;
        }
    }

    private MCRCache<MCRPermissionHandle, Boolean> getCacheFromSession(MCRSession session) {
        return (MCRCache<MCRPermissionHandle, Boolean>) session.get(key);
    }

    private MCRCache<MCRPermissionHandle, Boolean> createCache(MCRSession session) {
        return new MCRCache<>(CAPACITY, "Access rights in MCRSession " + session.getID());
    }

    MCRAccessCacheManager() {
        //init for current user done
        MCRSessionMgr.addSessionListener(this);
    }

    public Boolean isPermitted(String id, String permission) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = accessCache.get();
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        return permissionCache.getIfUpToDate(handle, currentSession.getLoginTime());
    }

    public void cachePermission(String id, String permission, boolean permitted) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        accessCache.get().put(handle, permitted);
    }

    public void removePermission(String id, String permission) {
        MCRPermissionHandle handle = new MCRPermissionHandle(id, permission);
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = accessCache.get();
        permissionCache.remove(handle);
    }

    public void removePermission(String... ids) {
        MCRCache<MCRPermissionHandle, Boolean> permissionCache = accessCache.get();
        removePermissionFromCache(permissionCache, Stream.of(ids).collect(Collectors.toSet()));
    }

    private void removePermissionFromCache(MCRCache<MCRPermissionHandle, Boolean> permissionCache, Set<String> ids) {
        final List<MCRPermissionHandle> handlesToRemove = permissionCache.keys()
            .stream()
            .filter(hdl-> hdl.getId()!=null)
            .filter(hdl -> ids.contains(hdl.getId()))
            .collect(Collectors.toList());
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

}
