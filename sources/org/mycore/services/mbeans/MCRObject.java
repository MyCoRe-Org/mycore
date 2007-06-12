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

import org.mycore.frontend.cli.MCRObjectCommands;

public class MCRObject extends MCRPersistenceBase implements MCRObjectMBean {
    
    public MCRObject(){
    }
    
    public static void register(){
        MCRObject instance=new MCRObject();
        MCRJMXBridge.registerMe(instance, "Persistence Operations", instance.getClass().getSimpleName());
    }
    
    public synchronized boolean loadFromDirectory(String directory) {
        try {
            startTransaction();
            MCRObjectCommands.loadFromDirectory(directory);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    public synchronized boolean loadFromFile(String file) {
        try {
            startTransaction();
            MCRObjectCommands.loadFromFile(file);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    public synchronized boolean repairIndexOfObjectID(String id) {
        try {
            startTransaction();
            MCRObjectCommands.repairMetadataSearchForID(id);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    public synchronized boolean repairIndexOfObjectType(String type) {
        try {
            startTransaction();
            MCRObjectCommands.repairMetadataSearch(type);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    public synchronized boolean updateFromDirectory(String directory) {
        try {
            startTransaction();
            MCRObjectCommands.updateFromDirectory(directory);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

    public synchronized boolean updateFromFile(String file) {
        try {
            startTransaction();
            MCRObjectCommands.updateFromFile(file);
            commitTransaction();
        } catch (Throwable e) {
            e.printStackTrace();
            rollbackTransaction();
            return false;
        }
        return true;
    }

}
