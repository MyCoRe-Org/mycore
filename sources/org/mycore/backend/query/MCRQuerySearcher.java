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

package org.mycore.backend.query;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
//import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcherBase;

public abstract class MCRQuerySearcher extends MCRSearcherBase{
    //public abstract MCRResults runQuery(String query);

    public static Logger logger = Logger.getLogger(MCRQuerySearcher.class.getName());

    protected static String SQLQueryTable = MCRConfiguration.instance().getString("MCR.QueryTableName", "MCRQuery");

    protected static String querytypes = MCRConfiguration.instance().getString("MCR.QueryTypes", "document,author");

    static private MCRQuerySearcher implementation;

    public static MCRQuerySearcher getInstance() {
        try {
            if (implementation == null) {
                implementation = (MCRQuerySearcher) MCRConfiguration.instance().getSingleInstanceOf("MCR.QuerySearcher_class_name", "org.mycore.backend.hibernate.MCRHIBSearcher");
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return implementation;
    }

}
