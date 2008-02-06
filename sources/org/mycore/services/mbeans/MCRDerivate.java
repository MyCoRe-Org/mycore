/**
 * 
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

import java.util.List;

import org.apache.log4j.Logger;

import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRDerivateCommands;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 * 
 */
public class MCRDerivate extends MCRCommandWrapperMBean implements MCRDerivateMBean {

    private static final Logger LOGGER = Logger.getLogger(MCRDerivate.class);

    public static void register() {
        MCRDerivate instance = new MCRDerivate();
        MCRJMXBridge.register(instance, "Persistence Operations", instance.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#deleteDerivate(java.lang.String)
     */
    public synchronized boolean deleteDerivate(String id) {
        addCommand("delete derivate " + id);
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#loadDerivateFromFile(java.lang.String)
     */
    public synchronized boolean loadDerivateFromFile(String file) {
        addCommand("load derivate from file " + file);
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#loadDerivatesFromDirectory(java.lang.String)
     */
    public synchronized boolean loadDerivatesFromDirectory(String directory) {
        addCommand("load all derivates from directory " + directory);
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#repairIndexOfDerivateID(java.lang.String)
     */
    public synchronized boolean repairIndexOfDerivateID(String id) {
        addCommand("repair derivate search of ID " + id);
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#repairIndexOfDerivates()
     */
    public synchronized boolean repairIndexOfDerivates() {
        addCommand("repair derivate search of type derivate");
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#updateDerivateFromFile(java.lang.String)
     */
    public synchronized boolean updateDerivateFromFile(String file) {
        addCommand("update derivate from file " + file);
        return processCommands();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.mbeans.MCRDerivateMBean#updateDerivatesFromDirectory(java.lang.String)
     */
    public synchronized boolean updateDerivatesFromDirectory(String directory) {
        addCommand("update all derivates from directory " + directory);
        return processCommands();
    }

    @Override
    protected List<MCRCommand> getCommands() {
        return new MCRDerivateCommands().getPossibleCommands();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getName() {
        return MCRDerivate.class.getSimpleName();
    }

}
