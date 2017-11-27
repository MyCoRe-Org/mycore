/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr.common.xml;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.solr.MCRSolrClientFactory;

public class MCRSolrXMLFunctions {

    /**
     * Deletes the given MyCoRe object from the solr index.
     * 
     * @param id MyCoRe ID
     */
    public static void delete(String id) throws SolrServerException, IOException {
        MCRSolrClientFactory.getSolrClient().deleteById(id);
    }

}
