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

package org.mycore.common.xml;

import org.mycore.common.MCRConfiguration;

/**
 * @author Thomas Scheffler (yagee)
 * 
 */
public class MCRXMLFunctions {

	static MCRConfiguration CONFIG = MCRConfiguration.instance();

	private static final String HOST_PREFIX = "MCR.remoteaccess_";

	private static final String QUERY_SUFFIX = "_query_servlet";

	private static final String IFS_SUFFIX = "_ifs_servlet";

	private static final String HOST_SUFFIX = "_host";

	private static final String PORT_SUFFIX = "_host";

	private static final String PROTOCOLL_SUFFIX = "_protocol";

	private static final String DEFAULT_PORT = "80";

	/**
	 * returns the given String trimmed
	 * 
	 * @param arg0
	 *            String to be trimmed
	 * @return trimmed copy of arg0
	 * @see java.lang.String#trim()
	 */
	public static String trim(String arg0) {
		return arg0.trim();
	}

	/**
	 * returns the QueryServlet-Link of the given hostAlias
	 * 
	 * @param hostAlias
	 *            remote alias
	 * @return QueryServlet-Link
	 */
	public static String getQueryServlet(String hostAlias) {
		return getBaseLink(hostAlias).append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(QUERY_SUFFIX).toString())).toString();
	}

	/**
	 * returns the FileNodeServlet-Link of the given hostAlias
	 * 
	 * @param hostAlias
	 *            remote alias
	 * @return FileNodeServlet-Link
	 */
	public static String getIFSServlet(String hostAlias) {
		return getBaseLink(hostAlias).append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(IFS_SUFFIX).toString())).toString();
	}

	private static StringBuffer getBaseLink(String hostAlias) {
		StringBuffer returns = new StringBuffer();
		returns.append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(PROTOCOLL_SUFFIX).toString(), "http")).append("://").append(
				CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(HOST_SUFFIX).toString()));
		String port = CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(PORT_SUFFIX).toString(), DEFAULT_PORT);
		if (port.equals(DEFAULT_PORT)) {
			returns.append(":").append(CONFIG.getString(new StringBuffer(HOST_PREFIX).append(hostAlias).append(PORT_SUFFIX).toString(), "80"));
		}
		return returns;
	}

}
