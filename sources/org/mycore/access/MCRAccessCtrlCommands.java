/**
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
 **/

package org.mycore.access;

import org.apache.log4j.Logger;

import org.mycore.frontend.cli.MCRAbstractCommands;
import org.mycore.frontend.cli.MCRClassificationCommands;
import org.mycore.frontend.cli.MCRCommand;

/**
 * This class provides a set of commands for the org.mycore.access package which
 * can be used by the command line interface. (creates sql tables, run queries
 * 
 * @author Arne Seifert
 */

public class MCRAccessCtrlCommands extends MCRAbstractCommands {

	/** The logger */
	public static Logger LOGGER = Logger
			.getLogger(MCRClassificationCommands.class.getName());

	/**
	 * constructor with commands.
	 */

	public MCRAccessCtrlCommands() {
		super();
		MCRCommand com = null;

		com = new MCRCommand("create accesstable",
				"org.mycore.access.MCRAccessCtrlCommands.createTables",
				"The command creates all tables for the Access Control System.");
		command.add(com);

		com = new MCRCommand("get access rule for ruleid {0}",
				"org.mycore.access.MCRAccessCtrlCommands.getRule String",
				"Returns the rule (string) for given ruleid.");
		command.add(com);

		com = new MCRCommand(
				"get ruleid for objectid {0} in pool {1}",
				"org.mycore.access.MCRAccessCtrlCommands.getRuleID String String",
				"Returns the ruleID for given object and access pool.");
		command.add(com);

	}

	/**
	 * method creates sql tables
	 */
	public static void createTables() {
		MCRAccessCtrlStore mcr_accessctrl = new MCRAccessCtrlStore();
		mcr_accessctrl.createTable();
	}

	/**
	 * This method returns the rule as string for a given id
	 * 
	 * @param ruleID
	 *            internal database ruleid
	 * @return string with rule definition
	 */
	public static void getRule(String ruleID) {
		MCRAccessCtrlStore mcr_accessctrl = new MCRAccessCtrlStore();
		LOGGER.info(mcr_accessctrl.getRule(ruleID));
		//return mcr_accessctrl.getRule(ruleID);
	}

	/**
	 * This method returns the ruleid as string for a given object and
	 * accesspool
	 * 
	 * @param objID
	 *            identificator for object, acPool name of accesspool
	 * @return string with ruleid
	 */
	public static void getRuleID(String objID, String acPool) {
		MCRAccessCtrlStore mcr_accessctrl = new MCRAccessCtrlStore();
		LOGGER.info(mcr_accessctrl.getRuleID(objID, acPool));
		//return mcr_accessctrl.getRuleID(objID, acPool);
	}
}