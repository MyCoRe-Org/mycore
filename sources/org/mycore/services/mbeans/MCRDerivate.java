/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.mbeans;

import org.mycore.frontend.cli.MCRDerivateCommands;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class MCRDerivate extends MCRPersistenceBase implements MCRDerivateMBean {

    public static void register(){
        MCRDerivate instance=new MCRDerivate();
        MCRJMXBridge.registerMe(instance, "Persistence Operations", instance.getClass().getSimpleName());
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#deleteDerivate(java.lang.String)
     */
    public synchronized boolean deleteDerivate(String id) {
        try {
            startTransaction();
            MCRDerivateCommands.delete(id);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#loadDerivateFromFile(java.lang.String)
     */
    public synchronized boolean loadDerivateFromFile(String file) {
        try {
            startTransaction();
            MCRDerivateCommands.loadFromFile(file);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#loadDerivatesFromDirectory(java.lang.String)
     */
    public synchronized boolean loadDerivatesFromDirectory(String directory) {
        try {
            startTransaction();
            MCRDerivateCommands.loadFromDirectory(directory);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#repairIndexOfDerivateID(java.lang.String)
     */
    public synchronized boolean repairIndexOfDerivateID(String id) {
        try {
            startTransaction();
            MCRDerivateCommands.repairDerivateSearchForID(id);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#repairIndexOfDerivates()
     */
    public synchronized boolean repairIndexOfDerivates() {
        try {
            startTransaction();
            MCRDerivateCommands.repairDerivateSearch();
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#updateDerivateFromFile(java.lang.String)
     */
    public synchronized boolean updateDerivateFromFile(String file) {
        try {
            startTransaction();
            MCRDerivateCommands.updateFromFile(file);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.mycore.services.mbeans.MCRDerivateMBean#updateDerivatesFromDirectory(java.lang.String)
     */
    public synchronized boolean updateDerivatesFromDirectory(String directory) {
        try {
            startTransaction();
            MCRDerivateCommands.updateFromDirectory(directory);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

}
