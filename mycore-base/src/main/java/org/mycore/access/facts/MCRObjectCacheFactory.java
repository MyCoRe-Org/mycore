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
package org.mycore.access.facts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This implementation creates an object cache
 * for MyCoRe objects which are retrieved during the processing of the rules.
 * 
 * It is registered as event handler to listen for updated and deleted objects.
 *  
 * @author Robert Stephan
 *
 */
public class MCRObjectCacheFactory extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRObjectCacheFactory SINGLETON = new MCRObjectCacheFactory();

    private static final int CACHE_MAX_SIZE = 100;

    private final MCRCache<MCRObjectID, MCRObject> objectCache;

    private MCRObjectCacheFactory() {
        objectCache = new MCRCache<>(CACHE_MAX_SIZE, this.getClass().getName());
        MCREventManager.instance().addEventHandler(MCREvent.ObjectType.OBJECT, this);
    }

    public static MCRObjectCacheFactory instance() {
        return SINGLETON;
    }

    public MCRObject getObject(MCRObjectID oid) {
        MCRObject obj = objectCache.get(oid);
        if (obj == null) {
            LOGGER.debug("reading object {} from metadata manager", oid);
            try {
                obj = MCRMetadataManager.retrieveMCRObject(oid);
                objectCache.put(oid, obj);
            } catch (MCRPersistenceException e) {
                LOGGER.debug("Object does not exist", e);
                return null;
            }
        } else {
            LOGGER.debug("reading object {} from cache", oid);
        }

        return obj;
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        objectCache.remove(obj.getId());
        LOGGER.debug("removing object {} from cache", obj.getId());
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        objectCache.remove(obj.getId());
        LOGGER.debug("removing object {} from cache", obj.getId());
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        objectCache.remove(obj.getId());
        LOGGER.debug("removing object {} from cache", obj.getId());
    }
}
