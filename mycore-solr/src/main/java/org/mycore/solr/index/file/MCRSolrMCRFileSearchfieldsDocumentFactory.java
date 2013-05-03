/*
 * $Id$
 * $Revision: 5697 $ $Date: May 3, 2013 $
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

package org.mycore.solr.index.file;

import java.io.IOException;

import org.apache.solr.common.SolrInputDocument;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsFile;
import org.mycore.services.fieldquery.data2fields.MCRIndexEntry;
import org.mycore.solr.index.document.MCRSolrSearchFieldsInputDocumentFactory;

/**
 * Uses searchfields defined for index "content" to build a {@link SolrInputDocument}.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrMCRFileSearchfieldsDocumentFactory extends MCRSolrMCRFileDocumentFactory {

    @Override
    public SolrInputDocument getDocument(MCRFile input) throws IOException {
        MCRData2FieldsFile data2FieldsFile = new MCRData2FieldsFile("content", input);
        MCRIndexEntry indexEntry = data2FieldsFile.buildIndexEntry();
        SolrInputDocument doc = MCRSolrSearchFieldsInputDocumentFactory.getSolrInputDocument(indexEntry);
        return doc;
    }

}
