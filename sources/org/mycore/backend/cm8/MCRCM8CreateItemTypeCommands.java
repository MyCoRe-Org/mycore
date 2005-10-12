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

package org.mycore.backend.cm8;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;
import org.mycore.common.MCRException;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRAbstractCommands;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRCM8CreateItemTypeCommands extends MCRAbstractCommands {
    private static Logger logger = Logger.getLogger(MCRCM8CreateItemTypeCommands.class.getName());

    /**
     * The constructor.
     */
    public MCRCM8CreateItemTypeCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("create database for {0}", "org.mycore.backend.cm8.MCRCM8CreateItemTypeCommands.createDataBase String", "The command create the search store for the given MCRObjectID type.");
        command.add(com);
    }

    /**
     * Create a new data base file for the MCRObjectID type.
     * 
     * @param mcr_type
     *            the MCRObjectID type
     * @return true if all is okay, else false
     */
    public static boolean createDataBase(String mcr_type) {
        // Read config file
        String conf_filename = CONFIG.getString("MCR.persistence_config_" + mcr_type);

        if (!conf_filename.endsWith(".xml")) {
            throw new MCRException("Configuration " + mcr_type + " does not end with .xml");
        }

        logger.info("Reading file " + conf_filename + " ...");

        InputStream conf_file = MCRCM8CreateItemTypeCommands.class.getResourceAsStream("/" + conf_filename);

        if (conf_file == null) {
            throw new MCRException("Can't read configuration file " + conf_filename);
        }

        org.jdom.Document confdoc = null;

        try {
            confdoc = new SAXBuilder().build(conf_file);
        } catch (Exception ex) {
            throw new MCRException("Can't parse configuration file " + conf_file);
        }

        // create MCRObject item types for CM8
        if (!mcr_type.equals("derivate")) {
            MCRCM8ItemType.create(mcr_type, confdoc);
        } else {
            logger.warn("The command does not support MCRDerivate.");
            return false;
        }

        return true;
    }
}
