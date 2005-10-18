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

package org.mycore.backend.cm8;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mycore.backend.sql.MCRSQLStatement;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRContentInputStream;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.services.query.MCRTextSearchInterface;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKConstant;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDDO;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKItemTypeDefICM;
import com.ibm.mm.sdk.common.DKNVPair;
import com.ibm.mm.sdk.common.DKResults;
import com.ibm.mm.sdk.common.DKTextICM;
import com.ibm.mm.sdk.common.DKTextIndexDefICM;
import com.ibm.mm.sdk.common.dkIterator;
import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * This class implements the MCRContentStore interface to store the content of
 * MCRFile objects in a IBM Content Manager 7 index class. The index class, the
 * keyfield labels and maximum DKDDO size can be configured in mycore.properties
 * 
 * <code>
 *   MCR.IFS.ContentStore.<StoreID>.ItemType        Index Class to use
 *   MCR.IFS.ContentStore.<StoreID>.Attribute.File  Name of file ID attribute
 *   MCR.IFS.ContentStore.<StoreID>.Attribute.Time  Name of timestamp attribute
 * </code>
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRCStoreContentManager8 extends MCRContentStore implements DKConstantICM, MCRTextSearchInterface {
    /** The ItemType name to store the content */
    protected String itemTypeName;

    /** The name of the attribute that stores the MCRFile.getID() */
    protected String attributeFile;

    protected final static int MAX_ATTRIBUTE_FILE_LENGTH = 128;

    /** The name of the attribute that stores the MCRFile.getOwnerID() */
    protected String attributeOwner;

    protected final static int MAX_ATTRIBUTE_OWNER_LENGTH = 128;

    /** The name of the attribute that stores the creation timestamp */
    protected String attributeTime;

    protected final static int MAX_ATTRIBUTE_TIME_LENGTH = 128;

    protected int storeTempSize = 4;

    protected String storeTempDir = "";

    private static final Logger LOGGER = MCRCM8ConnectionPool.getLogger();

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    /** Table which holds infos about where files are stored * */
    protected static final String METAIFSTABLE = CONFIG.getString("MCR.IFS.FileMetadataStore.SQL.TableName");

    /** Table which holds infos about how many documents are indexed * */
    protected static final String TIEINDEX_TABLE = "DB2EXT.TEXTINDEXES";

    protected static final String ICMDB = CONFIG.getString("MCR.persistence_cm8_library_server");

    protected static String TIEREF_TABLE;

    // TODO: Get this CollumnName from the Database somehow
    protected static final String DERIVATE_ATTRIBUTE = "ATTR0000001088";

    /**
     * The method initialized the CM8 content store with data of the property
     * files. If StoreTemp.Type is not set, "none" was set. The following types
     * are nessesary: <br />
     * <ul>
     * <li>none - it use the InputStream.available() method to get the length
     * of the stream.</li>
     * <li>memory - it write the InputStream in the memory to get the length of
     * the stream. Attention, this is only fo short files with maximal size in
     * MB of the StoreTemp.MemSize value! Files they are bigger was temporary
     * stored in a directory that is defined in the prperty variable
     * StoreTemp.Dir.</li>
     * </ul>
     * 
     * @param storeID
     *            the IFS store ID
     */
    public void init(String storeID) {
        super.init(storeID);
        itemTypeName = CONFIG.getString(prefix + "ItemType");
        attributeTime = CONFIG.getString(prefix + "Attribute.File");
        attributeOwner = CONFIG.getString(prefix + "Attribute.Owner");
        attributeFile = CONFIG.getString(prefix + "Attribute.Time");
        storeTempSize = CONFIG.getInt(prefix + "StoreTemp.MemSize", 4);
        storeTempDir = CONFIG.getString(prefix + "StoreTemp.Dir", "/tmp");

        if (TIEREF_TABLE == null) {
            // TIEREF_TABLE=MCRSQLConnection.justGetSingleValue(new
            // MCRSQLStatement(TIEINDEX_TABLE).setCondition("COLNAME","TIEREF").toSelectStatement("TABSCHEMA"));
            storeTieRefTable();
        }
    }

    private boolean storeTieRefTable() {
        DKDatastoreICM ds = MCRCM8ConnectionPool.instance().getConnection();
        Connection connection;

        try {
            connection = (Connection) ds.connection().handle();
        } catch (Exception e) {
            LOGGER.warn("Cannot get DKHandle from MCRCM8ConnectionPool!", e);
            MCRCM8ConnectionPool.instance().releaseConnection(ds);

            return false;
        }

        LOGGER.debug("Getting TIEREF Table name...");

        ResultSet rs;

        try {
            rs = connection.createStatement().executeQuery(new MCRSQLStatement(TIEINDEX_TABLE).setCondition("COLNAME", "TIEREF").toSelectStatement("TABSCHEMA,TABNAME"));

            if (rs.next()) {
                StringBuffer tieref = new StringBuffer(rs.getString(1));

                if (rs.wasNull()) {
                    MCRCM8ConnectionPool.instance().releaseConnection(ds);

                    return false;
                }

                tieref.append('.').append(rs.getString(2));

                if (rs.wasNull()) {
                    MCRCM8ConnectionPool.instance().releaseConnection(ds);

                    return false;
                }

                TIEREF_TABLE = tieref.toString();
                LOGGER.debug("TIEREF Table: " + TIEREF_TABLE);
            } else {
                LOGGER.warn("Failure getting TIEREF Index from " + TIEINDEX_TABLE + "!");
                MCRCM8ConnectionPool.instance().releaseConnection(ds);

                return false;
            }
        } catch (SQLException e1) {
            LOGGER.warn("Cannot get TIEREF Table out of Library Server", e1);
            MCRCM8ConnectionPool.instance().releaseConnection(ds);

            return false;
        }

        MCRCM8ConnectionPool.instance().releaseConnection(ds);

        return true;
    }

    protected String doStoreContent(MCRFileReader file, MCRContentInputStream source) throws Exception {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        DKDatastoreICM connection = null;

        try {
            logger.debug("Get a connection to CM8 connection pool.");
            connection = MCRCM8ConnectionPool.instance().getConnection();

            DKTextICM ddo = null;

            try {
                ddo = (DKTextICM) connection.createDDO(itemTypeName, DK_CM_ITEM);
            } catch (Exception ex) {
                createStore(connection);
                ddo = (DKTextICM) connection.createDDO(itemTypeName, DK_CM_ITEM);
            }

            logger.debug("A new DKTextICM was created.");

            logger.debug("MCRFile ID = " + file.getID());

            short dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeFile);
            ((DKDDO) ddo).setData(dataId, file.getID());

            logger.debug("MCRFile OwnerID = " + ((MCRFile) file).getOwnerID());
            dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeOwner);
            ((DKDDO) ddo).setData(dataId, ((MCRFile) file).getOwnerID());

            String timestamp = buildNextTimestamp();
            dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeTime);
            ((DKDDO) ddo).setData(dataId, timestamp);

            logger.debug("MimeType = " + file.getContentType().getMimeType());
            ddo.setMimeType(file.getContentType().getMimeType());
            ddo.setTextSearchableFlag(true);

            int filesize = 0;

            byte[] buffer = new byte[(storeTempSize * 1024 * 1024) + 16];

            try {
                filesize = source.read(buffer, 0, (storeTempSize * 1024 * 1024) + 16);
            } catch (IOException e) {
                throw new MCRException("Cant read File with ID " + file.getID(), e);
            }

            if (filesize <= (storeTempSize * 1024 * 1024)) {
                logger.debug("Set the MCRContentInputStream with memory length " + filesize + ".");
                ddo.add(new ByteArrayInputStream(buffer), filesize);
            } else {
                int si = filesize;
                File tmp = new File(storeTempDir, file.getID());
                FileOutputStream ftmp = new FileOutputStream(tmp);

                try {
                    ftmp.write(buffer, 0, filesize);
                } catch (IOException e) {
                    throw new MCRException("Cant write File with ID " + file.getID() + " to " + storeTempDir, e);
                }

                while (true) {
                    try {
                        si = source.read(buffer, 0, (storeTempSize * 1024 * 1024) + 16);
                    } catch (IOException e) {
                        throw new MCRException("Cant read File with ID " + file.getID(), e);
                    }

                    if (si == -1) {
                        break;
                    }

                    filesize += si;

                    try {
                        ftmp.write(buffer, 0, si);
                    } catch (IOException e) {
                        throw new MCRException("Cant write File with ID " + file.getID() + " to " + storeTempDir, e);
                    }
                }

                ftmp.close();
                logger.debug("Set the MCRContentInputStream with stream length " + filesize + ".");
                ddo.add(new FileInputStream(tmp), filesize);

                try {
                    tmp.delete();
                } catch (SecurityException e) {
                }
            }

            logger.debug("Add the DKTextICM.");

            String storageID = ddo.getPidObject().pidString();
            logger.debug("StorageID = " + storageID);
            logger.debug("The file was stored under CM8 Ressource Manager.");

            return storageID;
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }
    }

    /**
     * the method removes the content for the given IFS storageID.
     * 
     * @param storageID
     *            the IFS storage ID
     * @exception if
     *                an error was occured.
     */
    protected void doDeleteContent(String storageID) throws Exception {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        logger.debug("StorageID = " + storageID);

        DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();

        try {
            DKTextICM ddo = (DKTextICM) connection.createDDO(storageID);
            ddo.del();
            logger.debug("The file was deleted from CM8 Ressource Manager.");
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }
    }

    protected void doRetrieveContent(MCRFileReader file, OutputStream target) throws Exception {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        logger.debug("StorageID = " + file.getStorageID());

        DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();

        try {
            DKTextICM ddo = (DKTextICM) connection.createDDO(file.getStorageID());
            ddo.retrieve(DK_CM_CONTENT_NO);

            String[] url = ddo.getContentURLs(DK_CM_RETRIEVE, DK_CM_CHECKOUT, -1, -1, DK_ICM_GETINITIALRMURL);
            logger.debug("URL = " + url[0]);

            InputStream is = new URL(url[0]).openStream();
            MCRUtils.copyStream(is, target);

            logger.debug("The file was retrieved from CM8 Ressource Manager.");
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }
    }

    /**
     * This method creates a new ItemType to store ressource data under CM8.
     * 
     * @param connection
     *            the DKDatastoreICM connection
     */
    private void createStore(DKDatastoreICM connection) throws Exception {
        Logger logger = MCRCM8ConnectionPool.getLogger();

        // create the Attribute for IFS File ID
        if (!MCRCM8ItemTypeCommon.createAttributeVarChar(connection, attributeFile, MAX_ATTRIBUTE_FILE_LENGTH, false)) {
            logger.warn("CM8 Datastore Creation attribute " + attributeFile + " already exists.");
        }

        // create the Attribute for IFS File OwnerID
        if (!MCRCM8ItemTypeCommon.createAttributeVarChar(connection, attributeOwner, MAX_ATTRIBUTE_OWNER_LENGTH, false)) {
            logger.warn("CM8 Datastore Creation attribute " + attributeOwner + " already exists.");
        }

        // create the Attribute for IFS Time
        if (!MCRCM8ItemTypeCommon.createAttributeVarChar(connection, attributeTime, MAX_ATTRIBUTE_TIME_LENGTH, false)) {
            logger.warn("CM8 Datastore Creation attribute " + attributeTime + " already exists.");
        }

        // create a text search definition
        DKTextIndexDefICM mcr_item_text_index = MCRCM8ItemTypeCommon.getTextDefinition();
        mcr_item_text_index.setUDFName("ICMfetchFilter");
        mcr_item_text_index.setUDFSchema("ICMADMIN");

        // create the root itemtype
        logger.info("Create the ItemType " + itemTypeName);

        DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
        item_type.setName(itemTypeName);
        item_type.setDescription(itemTypeName);
        item_type.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
        item_type.setClassification(DK_ICM_ITEMTYPE_CLASS_RESOURCE_ITEM);
        item_type.setXDOClassName(DK_ICM_XDO_TEXT_CLASS_NAME);
        item_type.setXDOClassID(DK_ICM_XDO_TEXT_CLASS_ID);
        item_type.setTextIndexDef(mcr_item_text_index);
        item_type.setTextSearchable(true);

        DKDatastoreDefICM dsDefICM = (DKDatastoreDefICM) connection.datastoreDef();
        DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr(attributeFile);
        attr.setNullable(false);
        attr.setUnique(false);
        item_type.addAttr(attr);
        attr = (DKAttrDefICM) dsDefICM.retrieveAttr(attributeOwner);
        attr.setNullable(false);
        attr.setUnique(false);
        item_type.addAttr(attr);
        attr = (DKAttrDefICM) dsDefICM.retrieveAttr(attributeTime);
        attr.setNullable(false);
        attr.setUnique(false);
        item_type.addAttr(attr);

        short rmcode = 1; // the default
        item_type.setDefaultRMCode(rmcode);

        short smscode = 1; // the default
        item_type.setDefaultCollCode(smscode);
        item_type.add();
        logger.info("The ItemType " + itemTypeName + " for IFS CM8 store is created.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.query.MCRTextSearchInterface#getDerivateIDs(java.lang.String)
     */
    public String[] getDerivateIDs(String doctext) {
        LOGGER.debug("TS incoming query " + doctext);

        // toggles contains-text-basic:contains-text
        boolean basicquery = true;

        if (doctext == null) {
            return (new String[0]);
        }

        // transform query
        HashSet results = new HashSet();
        HashSet queries = CM8QueryParser.parse(doctext);
        HashSet tempSet;
        String[] temp;
        boolean first = true;

        for (Iterator it = queries.iterator(); it.hasNext();) {
            // convert StringArray to HashSet for merging
            temp = queryIndex((CM8Query) it.next(), basicquery);
            tempSet = new HashSet();

            for (int i = 0; i < temp.length; i++) {
                tempSet.add(temp[i]);
            }

            if (first) {
                results = tempSet;
                first = false;
            } else {
                results = MCRUtils.mergeHashSets(results, tempSet, MCRUtils.COMMAND_AND);
            }
        }

        // we have all results linked with and
        // we need to check if an prohibited clause is in the query
        HashSet negative = containNegativeClause(queries);

        if (negative.size() > 0) {
            // have to check that all documents of a derivate
            // get hit by those queries
            for (Iterator it = results.iterator(); it.hasNext();) {
                String derivateID = (String) it.next();
                int derCount = countDerivateContents(derivateID);

                // check every query that all documents in store match it
                for (Iterator neg = negative.iterator(); neg.hasNext();) {
                    CM8Query query = (CM8Query) neg.next();
                    int cqr = 0;
                    String[] ders = queryIndex(query, basicquery);

                    // the array is full of derivateIDs check how many are
                    // equal to our derivateID
                    for (int i = 0; i < ders.length; i++) {
                        if (ders[i].equals(derivateID)) {
                            cqr++;
                        }
                    }

                    if (derCount != cqr) {
                        StringBuffer msg = new StringBuffer("Query (").append(basicquery ? query.containstextbasic() : query.containstext()).append(") gets ").append(cqr).append(" of exact ").append(derCount).append("  needed results!");
                        LOGGER.debug(msg.toString());
                        LOGGER.debug("Removing from results:" + derivateID);
                        results.remove(derivateID);

                        break;
                    }
                }
            }
        }

        return MCRUtils.getStringArray(results.toArray());
    }

    private static HashSet containNegativeClause(HashSet queries) {
        HashSet returns = new HashSet();

        for (Iterator it = queries.iterator(); it.hasNext();) {
            CM8Query query = (CM8Query) it.next();

            if (query.prohibited) {
                returns.add(query);
            }
        }

        // return all negative Queries...
        return returns;
    }

    /**
     * returns the number of indexed documents
     * 
     * @param derivateID
     * @return number of index documents
     */
    protected int countDerivateContents(String derivateID) {
        LOGGER.debug("DerivateID = " + derivateID);

        int count;

        if ((TIEREF_TABLE == null) && !storeTieRefTable()) {
            throw new MCRException("Result of SQL query: \"SELECT TABSCHEMA,TABNAME FROM DB2EXT.TEXTINDEXES WHERE COLNAME='TIEREF'\" was empty!");
        }

        DKDatastoreICM ds = MCRCM8ConnectionPool.instance().getConnection();
        Connection connection;

        try {
            connection = (Connection) ds.connection().handle();
        } catch (Exception e) {
            MCRCM8ConnectionPool.instance().releaseConnection(ds);
            throw new MCRPersistenceException("Cannot get DKHandle from MCRCM8ConnectionPool!", e);
        }

        ResultSet rs;

        try {
            rs = connection.createStatement().executeQuery(new MCRSQLStatement(TIEREF_TABLE).setCondition(DERIVATE_ATTRIBUTE, derivateID).toSelectStatement("COUNT(*)"));

            if (rs.next()) {
                count = rs.getInt(1);
            } else {
                MCRCM8ConnectionPool.instance().releaseConnection(ds);
                throw new MCRPersistenceException("Failure counting Derivates in Index from " + TIEREF_TABLE + "!");
            }
        } catch (SQLException e1) {
            MCRCM8ConnectionPool.instance().releaseConnection(ds);
            throw new MCRPersistenceException("Cannot query TIEREF Table out of Library Server", e1);
        }

        MCRCM8ConnectionPool.instance().releaseConnection(ds);
        LOGGER.debug("Derivate: " + derivateID + " has " + count + " indexed files!");

        return count;
    }

    private String[] queryIndex(CM8Query query, boolean basic) {
        StringBuffer sb = new StringBuffer();
        sb.append('/').append(itemTypeName).append("[").append(basic ? "contains-text-basic" : "contains-text").append(" (@TIEREF,\"").append(basic ? query.containstextbasic() : query.containstext()).append("\")=1]");
        LOGGER.debug("TS outgoing query " + sb.toString());

        // start the query
        String[] outgo = new String[0];
        DKDatastoreICM connection = MCRCM8ConnectionPool.instance().getConnection();

        try {
            DKNVPair[] options = new DKNVPair[2];
            options[0] = new DKNVPair(DKConstant.DK_CM_PARM_MAX_RESULTS, "0");

            // No Maximum (Default)
            options[1] = new DKNVPair(DKConstant.DK_CM_PARM_END, null);

            DKResults results = (DKResults) connection.evaluate(sb.toString(), DKConstantICM.DK_CM_XQPE_QL_TYPE, options);
            dkIterator iter = results.createIterator();
            LOGGER.debug("Number of Results:  " + results.cardinality());
            outgo = new String[results.cardinality()];

            for (int i = 0; iter.more(); i++) {
                DKDDO resitem = (DKDDO) iter.next();
                short dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR, attributeOwner);
                outgo[i] = (String) resitem.getData(dataId);
                LOGGER.debug("MCRDerivateID :" + outgo[i]);
            }
        } catch (Exception e) {
        } finally {
            MCRCM8ConnectionPool.instance().releaseConnection(connection);
        }

        return outgo;
    }

    protected static String parseQueryBasic(String query) {
        int i = query.indexOf('\"');
        i++;

        if (i == 0) {
            return "";
        }

        int j = query.lastIndexOf('\"');

        if (j == -1) {
            return "";
        }

        StringBuffer tmp = new StringBuffer(query.substring(i, j));

        for (int x = 0; x < tmp.length(); x++) {
            switch (tmp.charAt(x)) {
            case '\'':

                // replace quotes by two quotes
                tmp.insert(x, '\'');
                x++;

                break;

            case '\"':

                // replace double quotes by quotes
                tmp.setCharAt(x, '\'');

                break;

            default:
                break;
            }
        }

        return tmp.toString();
    }

    protected static String parseQuery(String query) {
        String tmp = parseQueryBasic(query);

        if (tmp.length() == 0) {
            return tmp;
        }

        StringBuffer result = new StringBuffer();
        int begin = 0;

        for (int i = 0; i < tmp.length(); i++) {
            while ((i < tmp.length()) && (tmp.charAt(i) == ' ')) {
                i++;
            }

            if (tmp.charAt(i) == '-') {
                result.append("NOT ");
                i++;
            }

            begin = i;

            if (tmp.charAt(i) == '\'') {
                // at the beginning of a new word this marks a phrase
                i++;

                while (((i + 1) < tmp.length()) && ((tmp.charAt(i) != '\'') && (tmp.charAt(i + 1) != ' '))) {
                    // a space character after a quote marks the end of the
                    // phrase
                    i++;
                }

                i++; // stop at space character
                result.append(tmp.substring(begin, i));
            } else {
                // a single word
                result.append('\'');

                while ((i < tmp.length()) && (tmp.charAt(i) != ' ')) {
                    // a space character after a quote marks the end of the
                    // phrase
                    i++;
                }

                result.append(tmp.substring(begin, i)).append('\'');
            }

            result.append(" & ");
        }

        result.delete(result.length() - 3, result.length());

        // remove last space
        return result.toString();
    }

    private static final class CM8QueryParser {
        public static HashSet parse(String query) {
            return getQueries(query);
        }

        protected static String preParse(String query) {
            int i = query.indexOf('\"');
            i++;

            if (i == 0) {
                return "";
            }

            int j = query.lastIndexOf('\"');

            if (j == -1) {
                return "";
            }

            StringBuffer tmp = new StringBuffer(query.substring(i, j));

            for (int x = 0; x < tmp.length(); x++) {
                switch (tmp.charAt(x)) {
                case '\'':

                    // replace quotes by two quotes
                    tmp.insert(x, '\'');
                    x++;

                    break;

                case '\"':

                    // replace double quotes by quotes
                    tmp.setCharAt(x, '\'');

                    break;

                default:
                    break;
                }
            }

            return tmp.toString();
        }

        protected static HashSet getQueries(String query) {
            String tmp = preParse(query);
            HashSet queries = new HashSet();

            if (tmp.length() == 0) {
                return queries;
            }

            int begin = 0;

            for (int i = 0; i < tmp.length(); i++) {
                boolean prohibited = false;
                boolean required = true;

                while ((i < tmp.length()) && (tmp.charAt(i) == ' ')) {
                    i++;
                }

                if (tmp.charAt(i) == '-') {
                    prohibited = true;
                    required = false;
                    i++;

                    while (tmp.charAt(i) == ' ')

                        // removes spaces in front of negative clauses
                        i++;
                }

                begin = i;

                if (tmp.charAt(i) == '\'') {
                    // at the beginning of a new word this marks a phrase
                    i++;

                    while (((i + 1) < tmp.length()) && ((tmp.charAt(i) != '\'') && (tmp.charAt(i + 1) != ' '))) {
                        // a space character after a quote marks the end of the
                        // phrase
                        i++;
                    }

                    i++; // stop at space character
                    queries.add(new CM8Query(tmp.substring(begin, i), required, prohibited));
                } else {
                    // a single word
                    while ((i < tmp.length()) && (tmp.charAt(i) != ' ')) {
                        // a space character after a quote marks the end of the
                        // phrase
                        i++;
                    }

                    queries.add(new CM8Query(tmp.substring(begin, i), required, prohibited));
                }
            }

            return queries;
        }
    }

    private static final class CM8Query {
        public boolean required;

        public boolean prohibited;

        public String Query;

        public CM8Query(String query, boolean required, boolean prohibited) {
            this.prohibited = prohibited;
            this.required = required;

            if ((query.charAt(0) == '\'') && (query.charAt(query.length() - 1) == '\'')) {
                this.Query = query;
            } else if ((query.charAt(0) != '\'') && (query.charAt(query.length() - 1) != '\'')) {
                this.Query = '\'' + query + '\'';
            } else if (query.charAt(0) != '\'') {
                this.Query = '\'' + query;
            } else if (query.charAt(query.length() - 1) != '\'') {
                this.Query = query + '\'';
            }
        }

        public CM8Query(String query) {
            this(query, true, false);
        }

        public String containstext() {
            if (prohibited && required) {
                return "";
            } else if (prohibited) {
                return "NOT " + Query;
            } else {
                return Query;
            }
        }

        public String containstextbasic() {
            if (prohibited && required) {
                return "";
            } else if (prohibited) {
                return "-" + Query;
            } else if (required) {
                return "+" + Query;
            } else {
                return Query;
            }
        }

        public boolean equals(Object o) {
            if (o instanceof CM8Query) {
                CM8Query c = (CM8Query) o;

                return ((prohibited == c.prohibited) && (required == c.required) && (Query.equals(c.Query)));
            } else {
                return false;
            }
        }

        public int hashCode() {
            int ext = prohibited ? pow(31, Query.length()) : 0;

            return Query.hashCode() + ext;
        }

        private int pow(int a, int b) {
            int c = a;

            if (b == 0) {
                return 1;
            } else {
                for (; b > 1; b--)
                    c *= a;
            }

            return c;
        }
    }
}
