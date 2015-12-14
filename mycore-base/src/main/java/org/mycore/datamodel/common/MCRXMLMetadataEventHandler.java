/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.common;

import java.io.IOException;

import org.mycore.common.MCRPersistenceException;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

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

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleObjectRepaired(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected final void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        handleStoreEvent(evt, obj);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handleDerivateRepaired(org.mycore.common.events.MCREvent, org.mycore.datamodel.metadata.MCRDerivate)
     */
    @Override
    protected final void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        handleStoreEvent(evt, der);
    }

    private void handleStoreEvent(MCREvent evt, MCRBase obj) {
        try {
            MCRBaseContent content = new MCRBaseContent(obj);
            if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                metaDataManager.update(obj.getId(), content, obj.getService().getDate("modifydate"));
            } else if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                metaDataManager.create(obj.getId(), content, obj.getService().getDate("modifydate"));
            } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                metaDataManager.delete(obj.getId());
            }
            evt.put("content", content);
        } catch (IOException e) {
            throw new MCRPersistenceException("Error while handling event.", e);
        }
    }

}
