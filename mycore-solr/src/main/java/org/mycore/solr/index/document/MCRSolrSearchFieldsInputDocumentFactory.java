/*
 * $Id$
 * $Revision: 5697 $ $Date: Apr 16, 2013 $
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

package org.mycore.solr.index.document;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.data2fields.MCRData2FieldsContent;
import org.mycore.services.fieldquery.data2fields.MCRIndexEntry;
import org.xml.sax.SAXException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrSearchFieldsInputDocumentFactory extends MCRSolrInputDocumentFactory {
    private static final String INDEX = "metadata";

    private static Logger LOGGER = Logger.getLogger(MCRSolrSearchFieldsInputDocumentFactory.class);

    private static SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public SolrInputDocument getDocument(MCRObjectID id, MCRContent content) {
        LOGGER.debug("Building SolrInputDocument for " + id);
        MCRIndexEntry indexEntry = new MCRData2FieldsContent(INDEX, content, id).buildIndexEntry();
        SolrInputDocument document = getSolrInputDocument(indexEntry);
        LOGGER.debug("Finished building SolrInputDocument for " + id);
        return document;
    }

    private SolrInputDocument getSolrInputDocument(MCRIndexEntry indexEntry) {
        SolrInputDocument document = new SolrInputDocument();
        for (MCRFieldValue field : indexEntry.getFieldValues()) {
            String name = field.getFieldName();
            String dataType = field.getField().getDataType();
            try {
                switch (dataType) {
                case "integer":
                    document.addField(name, Integer.parseInt(field.getValue()));
                    break;
                case "decimal":
                    document.addField(name, Double.parseDouble(field.getValue()));
                    break;
                case "boolean":
                    document.addField(name, Boolean.parseBoolean(field.getValue()));
                    break;
                case "date":
                    document.addField(name, date.parse(field.getValue()));
                    break;
                case "timestamp":
                    MCRISO8601Date date = new MCRISO8601Date(field.getValue());
                    document.addField(name, date.getDate());
                    break;
                default:
                    document.addField(name, field.getValue());
                    break;
                }
            } catch (NumberFormatException | ParseException e) {
                LOGGER.warn("Could not parse value of field " + name + ": " + field.getValue(), e);
            }
        }
        //overwrites any values from for loop
        document.setField("id", indexEntry.getEntryID());
        document.setField("returnId", indexEntry.getReturnID());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(indexEntry.getEntryID() + " transformed to:\n" + document.toString());
        }
        return document;
    }

    @Override
    public Iterator<SolrInputDocument> getDocuments(Map<MCRObjectID, MCRContent> contentMap) throws IOException, SAXException {
        final Iterator<Map.Entry<MCRObjectID, MCRContent>> delegate = contentMap.entrySet().iterator();
        return new Iterator<SolrInputDocument>() {

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public SolrInputDocument next() {
                Entry<MCRObjectID, MCRContent> entry = delegate.next();
                return getDocument(entry.getKey(), entry.getValue());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove() is not supported on this iterator.");
            }
        };
    }
}
