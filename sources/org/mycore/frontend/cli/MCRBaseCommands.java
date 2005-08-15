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

package org.mycore.frontend.cli;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.jdom.input.SAXBuilder;

import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Provides static methods that implement commands for the MyCoRe command line
 * interface.
 * 
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 */
public class MCRBaseCommands extends MCRAbstractCommands {
	private static Logger logger = Logger.getLogger(MCRBaseCommands.class
			.getName());

	/**
	 * The constructor.
	 */
	public MCRBaseCommands() {
		super();
		MCRCommand com = null;

		com = new MCRCommand(
				"create database for {0}",
				"org.mycore.frontend.cli.MCRBaseCommands.createDataBase String",
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
	public static boolean createDataBase(String mcr_type) {
		// Read config file
		String conf_filename = CONFIG.getString("MCR.persistence_config_"
				+ mcr_type);
		if (!conf_filename.endsWith(".xml"))
			throw new MCRException("Configuration " + mcr_type
					+ " does not end with .xml");

		logger.info("Reading file " + conf_filename + " ...");
		InputStream conf_file = MCRBaseCommands.class.getResourceAsStream("/"
				+ conf_filename);
		if (conf_file == null)
			throw new MCRException("Can't read configuration file "
					+ conf_filename);

		org.jdom.Document confdoc = null;
		try {
			confdoc = new SAXBuilder().build(conf_file);
		} catch (Exception ex) {
			throw new MCRException("Can't parse configuration file "
					+ conf_file);
		}
		// create the database

		if (mcr_type.equals("derivate")) {
			MCRDerivate der = new MCRDerivate();
			der.createDataBase(mcr_type, confdoc);
		} else {
			MCRObject obj = new MCRObject();
			obj.createDataBase(mcr_type, confdoc);
		}
		return true;
	}
}
