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

package org.mycore.backend.lucene;

import org.mycore.common.*;
import org.mycore.datamodel.metadata.*;
import org.mycore.common.xml.*;

import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.transform.Templates;
import javax.xml.transform.sax.TransformerHandler;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;

/**
 * This class is the searchstore implemented with Lucene
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 */
public final class MCRLUCENESearchStore implements
        MCRObjectSearchStoreInterface {
    static final private Logger logger = Logger
            .getLogger(MCRLUCENESearchStore.class.getName());

    static final private MCRConfiguration config = MCRConfiguration.instance();

    static String indexDir = "";

    static boolean first = true;

    //TODO: read from property file
    static String dateformat = "yyyy-MM-dd";

    /** Reads properties from configuration file when class first used */
    static {
        MCRConfiguration config = MCRConfiguration.instance();

        indexDir = config.getString("MCR.persistence_lucene_searchindexdir");
        logger.info("MCR.persistence_lucene_searchindexdir: " + indexDir);
        String lockDir = config.getString("MCR.persistence_lucene_lockdir", "");
        logger.info("MCR.persistence_lucene_lockdir: " + lockDir);
        File file = new File(lockDir);

        if (!file.exists()) {
            logger.info("Lock Directory for Lucene doesn't exist: \"" + lockDir
                    + "\" use " + System.getProperty("java.io.tmpdir"));
        } else if (file.isDirectory()) {
            System.setProperty("org.apache.lucene.lockdir", lockDir);
        }
    }

    Vector fieldType;

    Vector fieldName;

    Vector fieldValue;

    /**
     * Creates a new MCRLUCENESearchStore.
     */
    public MCRLUCENESearchStore() throws MCRPersistenceException {
    }

    /**
     * This method creates and stores the searchable data from MCRObject in the
     * LUCENE datastore.
     * 
     * @param obj
     *            the MCRObject to put in the search store
     * @exception MCRPersistenceException
     *                if an error was occured
     */
    public final void create(MCRBase obj) throws MCRPersistenceException {
        logger.debug("MCRLUCENESearchStore create: MCRObjectID    : "
                + obj.getId().getId());
        logger.debug("MCRLUCENEPersistence create: MCRLabel       : "
                + obj.getLabel());
        org.jdom.Element root = obj.createXML().getRootElement();

        /*
         * try { // XMLOutputter outputter = new
         * XMLOutputter(org.jdom.output.Format.getPrettyFormat()); XMLOutputter
         * outputter = new XMLOutputter(); outputter.output(new
         * org.jdom.Document(root), System.out); } catch (Exception ex) {
         * ex.printStackTrace(); }
         */
        root.detach();

        try {
            List list = buildFields(root);
            if (null != list) {
                Document doc = buildLuceneDocument(obj.getId().getId(), list,
                        obj.getId().getTypeId());
                if (null != doc)
                    addDocumentToLucene(doc);
            }
        } catch (Exception ex) {
            throw new MCRPersistenceException(
                    "Error Creating search entry in lucene " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * The methode create a new datastore based of given configuration. It
     * create a new data table for storing MCRObjects with the same MCRObjectID
     * type.
     * 
     * @param mcr_type
     *            the MCRObjectID type as string
     * @param mcr_conf
     *            the configuration LUCENE stream as JDOM tree
     * @exception MCRConfigurationException
     *                if the configuration is not correct
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public void createDataBase(String mcr_type, org.jdom.Document mcr_conf)
            throws MCRConfigurationException, MCRPersistenceException {
        logger.info("This feature exist not for this store.");
    }

    /**
     * Updates the searchable content in the database. Currently this is the
     * same like delete and then a new create. Should be made with XUpdate in
     * the future.
     * 
     * @param obj
     *            the MCRObject to put in the search store
     * @exception MCRPersistenceException
     *                if an error was occured
     */
    public void update(MCRBase obj) throws MCRPersistenceException {
        logger.debug("MCRLUCENESearchStore create: MCRObjectID    : "
                + obj.getId().getId());
        logger.debug("MCRLUCENEPersistence create: MCRLabel       : "
                + obj.getLabel());
        delete(obj.getId());
        create(obj);
    }

    /**
     * Deletes the object with the given object id in the datastore.
     * 
     * @param mcr_id
     *            id of the object to delete
     * 
     * @throws MCRPersistenceException
     *             something goes wrong during delete
     */
    public void delete(MCRObjectID mcr_id) throws MCRPersistenceException {
        logger.debug("MCRLUCENEPersistence delete: MCRObjectID    : "
                + mcr_id.getId());
        logger.info("MCRLUCENEPersistence delete: MCRObjectID    : "
                + mcr_id.getId());
        try {
            deleteLuceneDocument(mcr_id.getId(), mcr_id.getTypeId());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

    /**
     * Transforms MyCoRe Objekt with stylesheet mcr_document2fields.xsl
     * 
     * @param root
     *            object to be to be transformed
     * 
     * @return list of childen, fields which correspond to lucene fields
     *  
     */
    private List buildFields(org.jdom.Element root) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MCRXSLTransformation transformer = MCRXSLTransformation
                    .getInstance();

            String stylesheet = "/mcr_object2lucene_fields.xsl";

            java.net.URL url = MCRLUCENESearchStore.class.getResource(stylesheet);
            if (null == url) {
                String msg = "File not found in CLASSPATH: " + stylesheet;
                logger.error(msg);
                throw new MCRConfigurationException(msg);
            }
            Templates xsl = transformer.getStylesheet(url.getFile());

            TransformerHandler handler = transformer.getTransformerHandler(xsl);
            transformer.transform(new org.jdom.Document(root), handler, out);
            out.close();
            byte[] output = out.toByteArray();
            org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
            org.jdom.Document jdom = builder.build(new ByteArrayInputStream(
                    output));
            return jdom.getRootElement().getChildren("field");
        } catch (Exception e) {
            //        throw new MCRException( "Error transforming MCRObject." );
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Build lucene document from transformed xml list
     * 
     * @param key
     *            MCRObject ID
     * @param fields
     *            corresponding to lucene fields
     * 
     * @return The lucene document
     *  
     */
    private Document buildLuceneDocument(String key, List fields, String typeId)
            throws Exception {
        Document doc = new Document();

        doc.add(Field.Keyword("mcr_ID", key));
        doc.add(Field.Keyword("mcr_type", typeId));

        for (int i = 0; i < fields.size(); i++) {
            org.jdom.Element xType = (org.jdom.Element) (fields.get(i));
            String name = xType.getAttributeValue("name");
            String type = xType.getAttributeValue("type");
            String content = xType.getText();
            if (null != name && null != type) {
                if ("date".equals(type)) {
                    DateFormat f1 = new SimpleDateFormat(dateformat);
                    DateFormat f2 = new SimpleDateFormat("yyyyMMdd");
                    Date d;
                    d = f1.parse(content);
                    content = f2.format(d);
                    type = "Text";
                }

                logger.debug("Name: " + name + " Type: " + type + " Content: "
                        + content);
                if (type.equals("Keyword"))
                    doc.add(Field.Keyword(name, content));
                if (type.equals("Text"))
                    doc.add(Field.Text(name, content));
                if (type.equals("UnStored"))
                    doc.add(Field.UnStored(name, content));
            }
        }

        return doc;
    }

    /**
     * Adds document to Lucene
     * 
     * @param doc
     *            lucene document to add to index
     *  
     */
    private void addDocumentToLucene(Document doc) throws Exception {
        IndexWriter writer = null;
        Analyzer analyzer = new GermanAnalyzer();

        // does directory for text index exist, if not build it
        if (first) {
            first = false;
            File file = new File(indexDir);

            if (!file.exists()) {
                logger.info("The Directory doesn't exist: " + indexDir
                        + " try to build it");
                IndexWriter writer2 = new IndexWriter(indexDir, analyzer, true);
                writer2.close();
            } else if (file.isDirectory()) {
                if (0 == file.list().length) {
                    logger.info("No Entries in Directory, initialize: "
                            + indexDir);
                    IndexWriter writer2 = new IndexWriter(indexDir, analyzer,
                            true);
                    writer2.close();
                }
            }

        } // if ( first

        if (null == writer) {
            writer = new IndexWriter(indexDir, analyzer, false);
            writer.mergeFactor = 200;
            writer.maxMergeDocs = 2000;
        }

        writer.addDocument(doc);
        writer.close();
        writer = null;
    }

    /**
     * Delete document in Lucene
     * 
     * @param id
     *            string document id
     * @param type
     *            string name of textindex
     *  
     */
    private void deleteLuceneDocument(String id, String type) throws Exception {

        IndexSearcher searcher = new IndexSearcher(indexDir);

        if (null == searcher)
            return;
        Term te1 = new Term("mcr_ID", id);
        Term te2 = new Term("mcr_type", type);

        BooleanQuery bq = new BooleanQuery();
        TermQuery qu = new TermQuery(te1);
        bq.add(qu, true, false);
        qu = new TermQuery(te2);
        bq.add(qu, true, false);

        logger.info("Searching for: " + bq.toString(""));

        Hits hits = searcher.search(bq);

        logger.info("Number of documents found : " + hits.length());
        if (1 == hits.length()) {
            logger.info(" id: " + hits.id(0) + " score: " + hits.score(0)
                    + " key: " + hits.doc(0).get("mcr_ID"));
            if (id.equals(hits.doc(0).get("mcr_ID"))) {
                IndexReader reader = IndexReader.open(indexDir);
                reader.delete(hits.id(0));
                reader.close();
                logger.info("DELETE: " + id);
            }
        }

    }

}