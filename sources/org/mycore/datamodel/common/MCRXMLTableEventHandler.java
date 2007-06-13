/*
 * $RCSfile$
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

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.classifications.MCRClassification;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class manages all operations of the XMLTables for operations of an
 * object or derivate.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRXMLTableEventHandler extends MCREventHandlerBase {

    static MCRXMLTableManager mcr_xmltable = MCRXMLTableManager.instance();

    /**
     * This method add the data to SQL table of XML data via MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationCreated(MCREvent evt, MCRClassification obj) {
        org.jdom.Document doc = obj.createXML();
        mcr_xmltable.create(new MCRObjectID(obj.getId()), doc);
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationUpdated(MCREvent evt, MCRClassification obj) {
        mcr_xmltable.delete(new MCRObjectID(obj.getId()));
        org.jdom.Document doc = obj.createXML();
        mcr_xmltable.create(new MCRObjectID(obj.getId()), doc);
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationDeleted(MCREvent evt, MCRClassification obj) {
        mcr_xmltable.delete(new MCRObjectID(obj.getId()));
    }

    /**
     * This method add the data to SQL table of XML data via MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        org.jdom.Document doc = obj.createXML();
        mcr_xmltable.create(obj.getId(), doc);
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        mcr_xmltable.update(obj.getId(), obj.createXML());
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        mcr_xmltable.delete(obj.getId());
    }

    /**
     * This method add the data to SQL table of XML data via MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected final void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        org.jdom.Document doc = der.createXML();
        mcr_xmltable.create(der.getId(), doc);
    }

    /**
     * This method update the data to SQL table of XML data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRObject that caused the event
     */
    protected final void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        mcr_xmltable.update(der.getId(), der.createXML());
    }

    /**
     * This method delete the XML data from SQL table data via
     * MCRXMLTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRObject that caused the event
     */
    protected final void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        mcr_xmltable.delete(der.getId());
    }

}
