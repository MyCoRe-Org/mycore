/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 18, 2013 $
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

package org.mycore.solr.index.handlers;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.MCRSolrIndexHandler;
import org.mycore.solr.index.handlers.content.MCRSolrMCRContentIndexHandler;
import org.mycore.solr.index.handlers.content.MCRSolrMCRContentMapIndexHandler;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrLazyInputDocumentHandlerFactory extends MCRSolrIndexHandlerFactory {

    @Override
    public MCRSolrIndexHandler getIndexHandler(MCRContent content, MCRObjectID id) {
        return new MCRSolrMCRContentIndexHandler(id, content);
    }

    /* (non-Javadoc)
     * @see org.mycore.solr.index.handlers.MCRSolrIndexHandlerFactory#getIndexHandler(java.util.Map)
     */
    @Override
    public MCRSolrIndexHandler getIndexHandler(Map<MCRObjectID, MCRContent> contentMap) {
        //contentMap is reused in different threads
        HashMap<MCRObjectID, MCRContent> copyMap = new HashMap<>(contentMap);
        return new MCRSolrMCRContentMapIndexHandler(copyMap);
    }

}
