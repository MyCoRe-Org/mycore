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

import java.util.List;

import org.apache.log4j.Logger;

import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRObjectCommands;

public class MCRObject extends MCRCommandWrapperMBean implements MCRObjectMBean {

    private static final Logger LOGGER = Logger.getLogger(MCRDerivate.class);

    public MCRObject() {
    }

    public static void register() {
        MCRObject instance = new MCRObject();
        MCRJMXBridge.register(instance, "Persistence Operations", instance.getName());
    }

    public synchronized boolean loadFromDirectory(String directory) {
        addCommand("load all objects from directory " + directory);
        return processCommands();
    }

    public synchronized boolean loadFromFile(String file) {
        addCommand("load object from file " + file);
        return processCommands();
    }

    public synchronized boolean repairIndexOfObjectID(String id) {
        addCommand("repair metadata search of ID " + id);
        return processCommands();
    }

    public synchronized boolean repairIndexOfObjectType(String type) {
        addCommand("repair metadata search of type " + type);
        return processCommands();
    }

    public synchronized boolean updateFromDirectory(String directory) {
        addCommand("update all objects from directory " + directory);
        return processCommands();
    }

    public synchronized boolean updateFromFile(String file) {
        addCommand("update object from file " + file);
        return processCommands();
    }

    @Override
    protected List<MCRCommand> getCommands() {
        return new MCRObjectCommands().getPossibleCommands();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected String getName() {
        return MCRObject.class.getSimpleName();
    }

}
