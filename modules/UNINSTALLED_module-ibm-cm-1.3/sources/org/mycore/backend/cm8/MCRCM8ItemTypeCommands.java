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
import org.mycore.backend.cm8.datatypes.MCRCM8ItemType;
import org.mycore.common.MCRException;
import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRCM8ItemTypeCommands extends MCRAbstractCommands {
    private static final Logger logger = Logger.getLogger(MCRCM8ItemTypeCommands.class.getName());

    /**
     * The constructor.
     */
    public MCRCM8ItemTypeCommands() {
        super();

        MCRCommand com = null;

        com = new MCRCommand("create itemtype for {0}",
                "org.mycore.backend.cm8.MCRCM8ItemTypeCommands.createDataBase String",
                "The command create the search store for the given MCRObjectID type.");
        command.add(com);
    }

    /**
     * Create a new data base file for the MCRObjectID type.
     * 
     * @param mcr_type
     *            the MCRObjectID type
     * @return true if all is okay, else false
     */
    public static boolean createDataBase(final String mcr_type) {
        // Read config file
        final String conf_filename = CONFIG.getString("MCR.persistence_config_" + mcr_type);

        logger.info("Reading file " + conf_filename + " ...");

        final InputStream conf_file = MCRCM8ItemTypeCommands.class.getResourceAsStream("/" + conf_filename);

        if (conf_file == null) {
            throw new MCRException("Can't read configuration file " + conf_filename);
        }

        org.jdom.Document confdoc = null;

        try {
            confdoc = new SAXBuilder().build(conf_file);
        } catch (final Exception ex) {
            throw new MCRException("Can't parse configuration file " + conf_file, ex);
        }

        // create MCRObject item types for CM8
        MCRCM8ItemType.create(mcr_type, confdoc);

        return true;
    }
}
