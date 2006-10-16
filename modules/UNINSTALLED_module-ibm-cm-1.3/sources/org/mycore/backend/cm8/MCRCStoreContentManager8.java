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
import java.io.FilterInputStream;
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
import org.mycore.backend.cm8.datatypes.MCRCM8AttributeUtils;
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
import com.ibm.mm.sdk.common.dkDatastore;
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

    private static final Logger LOGGER = Logger.getLogger(MCRCStoreContentManager8.class);

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
    public void init(final String storeID) {
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
        final dkDatastore ds = MCRCM8DatastorePool.instance().getDatastore();
        Connection connection;

        try {
            connection = (Connection) ds.connection().handle();
        } catch (final Exception e) {
            LOGGER.warn("Cannot get DKHandle from MCRCM8ConnectionPool!", e);
            MCRCM8DatastorePool.instance().releaseDatastore(ds);

            return false;
        }

        LOGGER.debug("Getting TIEREF Table name...");

        ResultSet rs;

        try {
            rs = connection.createStatement().executeQuery(
                    new MCRSQLStatement(TIEINDEX_TABLE).setCondition("COLNAME", "TIEREF").toSelectStatement(
                            "TABSCHEMA,TABNAME"));

            if (rs.next()) {
                final StringBuffer tieref = new StringBuffer(rs.getString(1));

                if (rs.wasNull()) {
                    MCRCM8DatastorePool.instance().releaseDatastore(ds);

                    return false;
                }

                tieref.append('.').append(rs.getString(2));

                if (rs.wasNull()) {
                    MCRCM8DatastorePool.instance().releaseDatastore(ds);

                    return false;
                }

                TIEREF_TABLE = tieref.toString();
                LOGGER.debug("TIEREF Table: " + TIEREF_TABLE);
            } else {
                LOGGER.warn("Failure getting TIEREF Index from " + TIEINDEX_TABLE + "!");
                MCRCM8DatastorePool.instance().releaseDatastore(ds);

                return false;
            }
        } catch (final SQLException e1) {
            LOGGER.warn("Cannot get TIEREF Table out of Library Server", e1);
            MCRCM8DatastorePool.instance().releaseDatastore(ds);

            return false;
        }

        MCRCM8DatastorePool.instance().releaseDatastore(ds);

        return true;
    }

    protected String doStoreContent(final MCRFileReader file, final MCRContentInputStream source) throws Exception {
        dkDatastore connection = null;

        try {
            LOGGER.debug("Get a connection to CM8 connection pool.");
            connection = MCRCM8DatastorePool.instance().getDatastore();

            DKTextICM ddo = null;

            try {
                ddo = (DKTextICM) connection.createDDO(itemTypeName, DK_CM_ITEM);
            } catch (final Exception ex) {
                createStore(connection);
                ddo = (DKTextICM) connection.createDDO(itemTypeName, DK_CM_ITEM);
            }

            LOGGER.debug("A new DKTextICM was created.");

            LOGGER.debug("MCRFile ID = " + file.getID());

            short dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeFile);
            ((DKDDO) ddo).setData(dataId, file.getID());

            LOGGER.debug("MCRFile OwnerID = " + ((MCRFile) file).getOwnerID());
            dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeOwner);
            ((DKDDO) ddo).setData(dataId, ((MCRFile) file).getOwnerID());

            final String timestamp = buildNextTimestamp();
            dataId = ((DKDDO) ddo).dataId(DK_CM_NAMESPACE_ATTR, attributeTime);
            ((DKDDO) ddo).setData(dataId, timestamp);

            LOGGER.debug("MimeType = " + file.getContentType().getMimeType());
            ddo.setMimeType(file.getContentType().getMimeType());
            ddo.setTextSearchableFlag(true);

            final int bufferLength = storeTempSize * 1024 * 1024;
            final byte[] buffer = new byte[bufferLength];
            int offset = 0;
            int numRead = 0;

            do {
                numRead = source.read(buffer, offset, bufferLength - offset);
                if (numRead > 0) {
                    offset += numRead;
                }
            } while (numRead != -1 && offset < bufferLength);

            if (numRead == -1) // all content was read into memory
            {
                LOGGER.debug("Adding content of ddo from memory buffer, size is " + offset);
                ddo.add(new ByteArrayInputStream(buffer), offset);
            } else // bufferLength bytes of content is in memory
            {
                final File tmp = new File(storeTempDir, file.getID());
                final FileOutputStream ftmp = new FileOutputStream(tmp);
                ftmp.write(buffer); // write buffer back to temp file

                while ((numRead = source.read(buffer, 0, bufferLength)) != -1) {
                    ftmp.write(buffer, 0, numRead);
                }

                ftmp.close();

                LOGGER.debug("Adding content of ddo from temp file, size is " + source.getLength());
                ddo.add(new FileInputStream(tmp), source.getLength());

                try {
                    tmp.delete();
                } catch (final Exception ignored) {
                }
            }

            LOGGER.debug("Added the DKTextICM.");

            final String storageID = ddo.getPidObject().pidString();
            LOGGER.debug("StorageID = " + storageID);
            LOGGER.debug("The file was stored under CM8 Ressource Manager.");

            return storageID;
        } finally {
            MCRCM8DatastorePool.instance().releaseDatastore(connection);
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
    protected void doDeleteContent(final String storageID) throws Exception {
        LOGGER.debug("StorageID = " + storageID);

        final DKDatastoreICM connection = (DKDatastoreICM) MCRCM8DatastorePool.instance().getDatastore();

        try {
            final DKTextICM ddo = (DKTextICM) connection.createDDO(storageID);
            ddo.del();
            LOGGER.debug("The file was deleted from CM8 Ressource Manager.");
        } finally {
            MCRCM8DatastorePool.instance().releaseDatastore(connection);
        }
    }

    protected void doRetrieveContent(final MCRFileReader file, final OutputStream target) throws Exception {
        MCRUtils.copyStream(doRetrieveContent(file), target);
    }

    protected InputStream doRetrieveContent(final MCRFileReader file) throws Exception {
        LOGGER.debug("StorageID = " + file.getStorageID());

        final DKDatastoreICM connection = (DKDatastoreICM) MCRCM8DatastorePool.instance().getDatastore();
        final DKTextICM ddo = (DKTextICM) connection.createDDO(file.getStorageID());
        ddo.retrieve(DK_CM_CONTENT_NO);

        final String[] url = ddo.getContentURLs(DK_CM_RETRIEVE, DK_CM_CHECKOUT, -1, -1, DK_ICM_GETINITIALRMURL);
        LOGGER.debug("URL = " + url[0]);

        final InputStream is = new URL(url[0]).openStream();
        LOGGER.debug("The file was retrieved from CM8 Ressource Manager.");
        return new CMInputStream(is, connection);
    }

    /**
     * This method creates a new ItemType to store ressource data under CM8.
     * 
     * @param connection
     *            the DKDatastoreICM connection
     */
    private void createStore(final dkDatastore connection) throws Exception {

        final DKDatastoreDefICM dsDefICM = (DKDatastoreDefICM) connection.datastoreDef();
        // create the Attribute for IFS File ID
        if (!MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, attributeFile, MAX_ATTRIBUTE_FILE_LENGTH, false)) {
            LOGGER.warn("CM8 Datastore Creation attribute " + attributeFile + " already exists.");
        }

        // create the Attribute for IFS File OwnerID
        if (!MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, attributeOwner, MAX_ATTRIBUTE_OWNER_LENGTH, false)) {
            LOGGER.warn("CM8 Datastore Creation attribute " + attributeOwner + " already exists.");
        }

        // create the Attribute for IFS Time
        if (!MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, attributeTime, MAX_ATTRIBUTE_TIME_LENGTH, false)) {
            LOGGER.warn("CM8 Datastore Creation attribute " + attributeTime + " already exists.");
        }

        // create a text search definition
        final DKTextIndexDefICM mcr_item_text_index = MCRCM8AttributeUtils.getTextDefinition();
        mcr_item_text_index.setUDFName("ICMfetchFilter");
        mcr_item_text_index.setUDFSchema("ICMADMIN");

        // create the root itemtype
        LOGGER.info("Create the ItemType " + itemTypeName);

        final DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
        item_type.setName(itemTypeName);
        item_type.setDescription(itemTypeName);
        item_type.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
        item_type.setClassification(DK_ICM_ITEMTYPE_CLASS_RESOURCE_ITEM);
        item_type.setXDOClassName(DK_ICM_XDO_TEXT_CLASS_NAME);
        item_type.setXDOClassID(DK_ICM_XDO_TEXT_CLASS_ID);
        item_type.setTextIndexDef(mcr_item_text_index);
        item_type.setTextSearchable(true);

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

        final short rmcode = 1; // the default
        item_type.setDefaultRMCode(rmcode);

        final short smscode = 1; // the default
        item_type.setDefaultCollCode(smscode);
        item_type.add();
        LOGGER.info("The ItemType " + itemTypeName + " for IFS CM8 store is created.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mycore.services.query.MCRTextSearchInterface#getDerivateIDs(java.lang.String)
     */
    public String[] getDerivateIDs(final String doctext) {
        LOGGER.debug("TS incoming query " + doctext);

        // toggles contains-text-basic:contains-text
        final boolean basicquery = true;

        if (doctext == null) {
            return new String[0];
        }

        // transform query
        HashSet results = new HashSet();
        final HashSet queries = CM8QueryParser.parse(doctext);
        HashSet tempSet;
        String[] temp;
        boolean first = true;

        for (final Iterator it = queries.iterator(); it.hasNext();) {
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
        final HashSet negative = containNegativeClause(queries);

        if (negative.size() > 0) {
            // have to check that all documents of a derivate
            // get hit by those queries
            for (final Iterator it = results.iterator(); it.hasNext();) {
                final String derivateID = (String) it.next();
                final int derCount = countDerivateContents(derivateID);

                // check every query that all documents in store match it
                for (final Iterator neg = negative.iterator(); neg.hasNext();) {
                    final CM8Query query = (CM8Query) neg.next();
                    int cqr = 0;
                    final String[] ders = queryIndex(query, basicquery);

                    // the array is full of derivateIDs check how many are
                    // equal to our derivateID
                    for (int i = 0; i < ders.length; i++) {
                        if (ders[i].equals(derivateID)) {
                            cqr++;
                        }
                    }

                    if (derCount != cqr) {
                        final StringBuffer msg = new StringBuffer("Query (").append(
                                basicquery ? query.containstextbasic() : query.containstext()).append(") gets ")
                                .append(cqr).append(" of exact ").append(derCount).append("  needed results!");
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

    private static HashSet containNegativeClause(final HashSet queries) {
        final HashSet returns = new HashSet();

        for (final Iterator it = queries.iterator(); it.hasNext();) {
            final CM8Query query = (CM8Query) it.next();

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
    protected int countDerivateContents(final String derivateID) {
        LOGGER.debug("DerivateID = " + derivateID);

        int count;

        if (TIEREF_TABLE == null && !storeTieRefTable()) {
            throw new MCRException(
                    "Result of SQL query: \"SELECT TABSCHEMA,TABNAME FROM DB2EXT.TEXTINDEXES WHERE COLNAME='TIEREF'\" was empty!");
        }

        final dkDatastore ds = MCRCM8DatastorePool.instance().getDatastore();
        Connection connection;

        try {
            connection = (Connection) ds.connection().handle();
        } catch (final Exception e) {
            MCRCM8DatastorePool.instance().releaseDatastore(ds);
            throw new MCRPersistenceException("Cannot get DKHandle from MCRCM8ConnectionPool!", e);
        }

        ResultSet rs;

        try {
            rs = connection.createStatement().executeQuery(
                    new MCRSQLStatement(TIEREF_TABLE).setCondition(DERIVATE_ATTRIBUTE, derivateID).toSelectStatement(
                            "COUNT(*)"));

            if (rs.next()) {
                count = rs.getInt(1);
            } else {
                MCRCM8DatastorePool.instance().releaseDatastore(ds);
                throw new MCRPersistenceException("Failure counting Derivates in Index from " + TIEREF_TABLE + "!");
            }
        } catch (final SQLException e1) {
            MCRCM8DatastorePool.instance().releaseDatastore(ds);
            throw new MCRPersistenceException("Cannot query TIEREF Table out of Library Server", e1);
        }

        MCRCM8DatastorePool.instance().releaseDatastore(ds);
        LOGGER.debug("Derivate: " + derivateID + " has " + count + " indexed files!");

        return count;
    }

    private String[] queryIndex(final CM8Query query, final boolean basic) {
        final StringBuffer sb = new StringBuffer();
        sb.append('/').append(itemTypeName).append("[").append(basic ? "contains-text-basic" : "contains-text").append(
                " (@TIEREF,\"").append(basic ? query.containstextbasic() : query.containstext()).append("\")=1]");
        LOGGER.debug("TS outgoing query " + sb.toString());

        // start the query
        String[] outgo = new String[0];
        final dkDatastore connection = MCRCM8DatastorePool.instance().getDatastore();

        try {
            final DKNVPair[] options = new DKNVPair[2];
            options[0] = new DKNVPair(DKConstant.DK_CM_PARM_MAX_RESULTS, "0");

            // No Maximum (Default)
            options[1] = new DKNVPair(DKConstant.DK_CM_PARM_END, null);

            final DKResults results = (DKResults) connection.evaluate(sb.toString(), DKConstant.DK_CM_XQPE_QL_TYPE,
                    options);
            final dkIterator iter = results.createIterator();
            LOGGER.debug("Number of Results:  " + results.cardinality());
            outgo = new String[results.cardinality()];

            for (int i = 0; iter.more(); i++) {
                final DKDDO resitem = (DKDDO) iter.next();
                final short dataId = resitem.dataId(DK_CM_NAMESPACE_ATTR, attributeOwner);
                outgo[i] = (String) resitem.getData(dataId);
                LOGGER.debug("MCRDerivateID :" + outgo[i]);
            }
        } catch (final Exception e) {
        } finally {
            MCRCM8DatastorePool.instance().releaseDatastore(connection);
        }

        return outgo;
    }

    protected static String parseQueryBasic(final String query) {
        int i = query.indexOf('\"');
        i++;

        if (i == 0) {
            return "";
        }

        final int j = query.lastIndexOf('\"');

        if (j == -1) {
            return "";
        }

        final StringBuffer tmp = new StringBuffer(query.substring(i, j));

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

    protected static String parseQuery(final String query) {
        final String tmp = parseQueryBasic(query);

        if (tmp.length() == 0) {
            return tmp;
        }

        final StringBuffer result = new StringBuffer();
        int begin = 0;

        for (int i = 0; i < tmp.length(); i++) {
            while (i < tmp.length() && tmp.charAt(i) == ' ') {
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

                while (i + 1 < tmp.length() && tmp.charAt(i) != '\'' && tmp.charAt(i + 1) != ' ') {
                    // a space character after a quote marks the end of the
                    // phrase
                    i++;
                }

                i++; // stop at space character
                result.append(tmp.substring(begin, i));
            } else {
                // a single word
                result.append('\'');

                while (i < tmp.length() && tmp.charAt(i) != ' ') {
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
        public static HashSet parse(final String query) {
            return getQueries(query);
        }

        protected static String preParse(final String query) {
            int i = query.indexOf('\"');
            i++;

            if (i == 0) {
                return "";
            }

            final int j = query.lastIndexOf('\"');

            if (j == -1) {
                return "";
            }

            final StringBuffer tmp = new StringBuffer(query.substring(i, j));

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

        protected static HashSet getQueries(final String query) {
            final String tmp = preParse(query);
            final HashSet queries = new HashSet();

            if (tmp.length() == 0) {
                return queries;
            }

            int begin = 0;

            for (int i = 0; i < tmp.length(); i++) {
                boolean prohibited = false;
                boolean required = true;

                while (i < tmp.length() && tmp.charAt(i) == ' ') {
                    i++;
                }

                if (tmp.charAt(i) == '-') {
                    prohibited = true;
                    required = false;
                    i++;

                    while (tmp.charAt(i) == ' ') {
                        // removes spaces in front of negative clauses
                        i++;
                    }
                }

                begin = i;

                if (tmp.charAt(i) == '\'') {
                    // at the beginning of a new word this marks a phrase
                    i++;

                    while (i + 1 < tmp.length() && tmp.charAt(i) != '\'' && tmp.charAt(i + 1) != ' ') {
                        // a space character after a quote marks the end of the
                        // phrase
                        i++;
                    }

                    i++; // stop at space character
                    queries.add(new CM8Query(tmp.substring(begin, i), required, prohibited));
                } else {
                    // a single word
                    while (i < tmp.length() && tmp.charAt(i) != ' ') {
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

        public CM8Query(final String query, final boolean required, final boolean prohibited) {
            this.prohibited = prohibited;
            this.required = required;

            if (query.charAt(0) == '\'' && query.charAt(query.length() - 1) == '\'') {
                Query = query;
            } else if (query.charAt(0) != '\'' && query.charAt(query.length() - 1) != '\'') {
                Query = '\'' + query + '\'';
            } else if (query.charAt(0) != '\'') {
                Query = '\'' + query;
            } else if (query.charAt(query.length() - 1) != '\'') {
                Query = query + '\'';
            }
        }

        public CM8Query(final String query) {
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

        public boolean equals(final Object o) {
            if (o instanceof CM8Query) {
                final CM8Query c = (CM8Query) o;

                return prohibited == c.prohibited && required == c.required && Query.equals(c.Query);
            } else {
                return false;
            }
        }

        public int hashCode() {
            final int ext = prohibited ? pow(31, Query.length()) : 0;

            return Query.hashCode() + ext;
        }

        private int pow(final int a, int b) {
            int c = a;

            if (b == 0) {
                return 1;
            } else {
                for (; b > 1; b--) {
                    c *= a;
                }
            }

            return c;
        }
    }

    private static class CMInputStream extends FilterInputStream {

        private dkDatastore connection;

        public CMInputStream(final InputStream source, final dkDatastore connection) {
            super(source);
            this.connection = connection;
        }

        public void close() throws IOException {
            super.close();
            MCRCM8DatastorePool.instance().releaseDatastore(connection);
        }
    }
}
