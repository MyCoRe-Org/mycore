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

package org.mycore.frontend.indexbrowser;

import java.util.StringTokenizer;

import org.mycore.common.MCRConfiguration;

/**
 * @author Frank Lützenkirchen
 */
class MCRIndexConfiguration {
	String table;

	boolean distinct;

	String browseField;

	String fields;

	String[] extraFields;

	String order;

	String filter;

	String style;

	int maxPerPage;

	MCRIndexConfiguration(String ID) {
		MCRConfiguration config = MCRConfiguration.instance();
		String prefix = "MCR.IndexBrowser." + ID + ".";

		table = config.getString(prefix + "Table");
		distinct = config.getBoolean(prefix + "Distinct", true);
		browseField = config.getString(prefix + "FieldToBrowse");
		filter = config.getString(prefix + "FilterCondition", null);
		maxPerPage = config.getInt(prefix + "MaxPerPage");
		style = config.getString(prefix + "Style");
		fields = config.getString(prefix + "ExtraOutputFields", null);
		order = config.getString(prefix + "Order", "asc");
		buildFieldList(fields);
	}

	void buildFieldList(String fields) {
		if ((fields == null) || (fields.trim().length() == 0))
			extraFields = new String[0];

		StringTokenizer st = new StringTokenizer(fields, " ,");
		extraFields = new String[st.countTokens()];
		for (int i = 0; i < extraFields.length; i++)
			extraFields[i] = st.nextToken();
	}
}
