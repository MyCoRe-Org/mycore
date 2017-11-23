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

package org.mycore.datamodel.common;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * This class manages all operations of the XMLTables for operations of an
 * object or derivate.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRXMLMetadataEventHandler extends MCREventHandlerBase {

    static MCRXMLMetadataManager metaDataManager = MCRXMLMetadataManager.instance();

    /**
     * This method add the data to SQL table of XML data via MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        handleStoreEvent(evt, obj);
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleStoreEvent(evt, obj);
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        handleStoreEvent(evt, obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleStoreEvent(evt, obj);
    }

    /**
     * This method add the data to SQL table of XML data via MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    @Override
    protected final void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        handleStoreEvent(evt, der);
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleStoreEvent(evt, der);
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLMetadataManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        handleStoreEvent(evt, der);
    }

    private void handleStoreEvent(MCREvent evt, MCRBase obj) {
        String eventType = evt.getEventType();
        MCRObjectID id = obj.getId();
        try {
            switch (eventType) {
                case MCREvent.REPAIR_EVENT:
                case MCREvent.UPDATE_EVENT:
                case MCREvent.CREATE_EVENT:
                    MCRBaseContent content = new MCRBaseContent(obj);
                    Date modified = obj.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
                    switch (eventType) {
                        case MCREvent.REPAIR_EVENT:
                            MCRContent retrieveContent = metaDataManager.retrieveContent(id);
                            if (isUptodate(retrieveContent, content)) {
                                return;
                            }
                        case MCREvent.UPDATE_EVENT:
                            metaDataManager.update(id, content, modified);
                            break;
                        case MCREvent.CREATE_EVENT:
                            metaDataManager.create(id, content, modified);
                            break;
                    }
                    evt.put("content", content);
                    break;
                case MCREvent.DELETE_EVENT:
                    metaDataManager.delete(id);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid event type " + eventType + " for object " + id);
            }
        } catch (IOException e) {
            throw new MCRPersistenceException("Error while handling '" + eventType + "' event of '" + id + "'", e);
        }
    }

    private boolean isUptodate(MCRContent retrieveContent, MCRBaseContent content) throws IOException {
        return retrieveContent.lastModified() > content.lastModified()
            || Arrays.equals(retrieveContent.asByteArray(), content.asByteArray());
    }

}
