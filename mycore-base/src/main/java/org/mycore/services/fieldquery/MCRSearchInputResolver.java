/*
 * 
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

package org.mycore.services.fieldquery;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.log4j.Logger;
import org.jdom2.transform.JDOMSource;

/**
 * Implements the searchInput:[ID] resolver that returns the
 * data previously entered in editor form for search servlet.
 * This is used for the "refine search" button in search masks
 * and query results. It is a replacement for "MCRSearchServlet?mode=load".  
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRSearchInputResolver implements URIResolver {

    private final static Logger LOGGER = Logger.getLogger(MCRSearchInputResolver.class);

    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(":") + 1);
        LOGGER.debug("Reading cached query data from ID " + id);

        MCRCachedQueryData qd = null;
        try {
            qd = MCRCachedQueryData.getData(id);
        } catch (Exception ex) {
            throw new TransformerException(ex);
        }

        if (qd == null) {
            String msg = "Result list is not in cache any more, please re-run query";
            LOGGER.debug(msg);
            throw new TransformerException(msg);
        }

        return new JDOMSource(qd.getInput());
    }

}
