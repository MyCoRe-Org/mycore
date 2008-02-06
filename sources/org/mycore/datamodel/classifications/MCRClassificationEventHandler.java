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

package org.mycore.datamodel.classifications;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;

/**
 * This class manages all operations of the classifications for operations of an
 * object or derivate.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRClassificationEventHandler extends MCREventHandlerBase {

    static MCRClassificationManager CM = MCRClassificationManager.instance();

    /**
     * This method add the data to SQL table of classification data via MCRClassificationManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationCreated(MCREvent evt, MCRClassification obj) {
        // store in SQL index tables
        CM.createClassificationItem(obj);
        CM.createCategoryItems(obj.getCategories());
    }

    /**
     * This method update the data to SQL table of classification data via
     * MCRClassificationManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationUpdated(MCREvent evt, MCRClassification obj) {
        // SQL index delete
        CM.deleteClassificationItem(obj.getId());
        // store in SQL tables
        CM.createClassificationItem(obj);
        CM.createCategoryItems(obj.getCategories());        
    }

    /**
     * This method delete the classification data from SQL table data via
     * MCRClassificationManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationDeleted(MCREvent evt, MCRClassification obj) {
        // SQL index delete
        CM.deleteClassificationItem(obj.getId());
    }

    /**
     * This method repair the classification data from SQL table data via
     * MCRClassificationManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected final void handleClassificationRepaired(MCREvent evt, MCRClassification obj) {
        // SQL index delete
        CM.deleteClassificationItem(obj.getId());
        // store in SQL tables
        CM.createClassificationItem(obj);
        CM.createCategoryItems(obj.getCategories());        
    }

}
