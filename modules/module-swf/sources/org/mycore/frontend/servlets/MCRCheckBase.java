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

package org.mycore.frontend.servlets;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class is the superclass of servlets which checks the MCREditorServlet
 * output XML for metadata object and derivate objects.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */

abstract public class MCRCheckBase extends MCRServlet {
	protected static Logger logger = Logger.getLogger(MCRCheckBase.class);

	String NL = System.getProperty("file.separator");

	/**
	 * The method check the privileg of this action.
	 * 
	 * @param privs
	 *            the ArrayList of privilegs
	 * @return true if the privileg exist, else return false
	 */
	abstract public boolean hasPrivileg(ArrayList privs, String type);

	/**
	 * The method is a dummy or works with the data and return an URL with the
	 * next working step.
	 * 
	 * @param ID
	 *            the MCRObjectID of the MCRObject
	 * @return the next URL as String
	 */
	abstract public String getNextURL(MCRObjectID ID) throws Exception;

	/**
	 * The method send a message to the mail address for the MCRObjectType.
	 * 
	 * @param ID
	 *            the MCRObjectID of the MCRObject
	 */
	abstract public void sendMail(MCRObjectID ID);

	/**
	 * A method to handle IO errors.
	 * 
	 * @param jab
	 *            the MCRServletJob
	 * @param lang
	 *            the current language
	 */
	protected void errorHandlerIO(MCRServletJob job, String lang)
			throws Exception {
		String pagedir = CONFIG.getString("MCR.editor_page_dir", "");
		job.getResponse().sendRedirect(
				job.getResponse().encodeRedirectURL(
						getBaseURL() + pagedir + "editor_error_store.xml"));
	}

}
