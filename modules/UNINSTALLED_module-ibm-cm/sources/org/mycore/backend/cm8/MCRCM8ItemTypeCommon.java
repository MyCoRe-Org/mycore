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

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;
import com.ibm.mm.sdk.common.DKTextIndexDefICM;
import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * This class implements common methods for handling the ItemType on IBM Content
 * Manager 8.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
final class MCRCM8ItemTypeCommon implements DKConstantICM {
    /**
     * The constructor of this class.
     */
    private static Logger LOGGER = Logger.getLogger(MCRCM8ItemTypeCommon.class);

    MCRCM8ItemTypeCommon() {
    }

    /**
     * This method read the configuration and return the DKTextIndexDefICM NSE
     * definition.
     * 
     * @return the DKTextIndexDefICM NSE definition.
     */
    static final DKTextIndexDefICM getTextDefinition() {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        MCRConfiguration conf = MCRConfiguration.instance();

        // create the TIE definition
        DKTextIndexDefICM mcr_item_text_index = new DKTextIndexDefICM();
        int text_commitcount = conf.getInt("MCR.persistence_cm8_textsearch_commitcount", 1);
        mcr_item_text_index.setCommitCount(text_commitcount);
        mcr_item_text_index.setFormat(DKTextIndexDefICM.TEXT_INDEX_DOC_FORMAT_TEXT);

        int text_ccsid = conf.getInt("MCR.persistence_cm8_textsearch_ccsid", 850);
        mcr_item_text_index.setIndexCCSID(text_ccsid);

        String text_indexdir = conf.getString("MCR.persistence_cm8_textsearch_indexdir", "/home/icmadmin/index");
        mcr_item_text_index.setIndexDir(text_indexdir);

        String text_lang = conf.getString("MCR.persistence_cm8_textsearch_lang", "DE");
        mcr_item_text_index.setIndexLangCode(text_lang);

        int text_minchanges = conf.getInt("MCR.persistence_cm8_textsearch_minchanges", 1);
        mcr_item_text_index.setMinChanges(text_minchanges);

        String text_updatefreq = conf.getString("MCR.persistence_cm8_textsearch_updatefreq", "");
        mcr_item_text_index.setUpdateFrequency(text_updatefreq);

        String text_workingdir = conf.getString("MCR.persistence_cm8_textsearch_workingdir", "/home/icmadmin/work");
        mcr_item_text_index.setWorkingDir(text_workingdir);
        logger.debug("CM8 TextSearch - CommitCount = " + mcr_item_text_index.getCommitCount());
        logger.debug("CM8 TextSearch - Format = " + mcr_item_text_index.getFormat());
        logger.debug("CM8 TextSearch - CCSID = " + mcr_item_text_index.getIndexCCSID());
        logger.debug("CM8 TextSearch - IndexDir = " + mcr_item_text_index.getIndexDir());
        logger.debug("CM8 TextSearch - Lang = " + mcr_item_text_index.getIndexLangCode());
        logger.debug("CM8 TextSearch - MinChanges = " + mcr_item_text_index.getMinChanges());
        logger.debug("CM8 TextSearch - UpdateFreq = " + mcr_item_text_index.getUpdateFrequency());
        logger.debug("CM8 TextSearch - WorkingDir = " + mcr_item_text_index.getWorkingDir());

        return mcr_item_text_index;
    }

    /**
     * This methode is internal and create a DK_CM_VARCHAR attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeVarChar(DKDatastoreICM connection, String name, int len, boolean search) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_VARCHAR);
        attr.setStringType(DK_CM_ATTR_VAR_ALPHANUM_EXT);
        attr.setSize(len);
        attr.setTextSearchable(search);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_BOOLEAN attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeBoolean(DKDatastoreICM connection, String name) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_VARCHAR);
        attr.setStringType(DK_CM_ATTR_VAR_ALPHANUM_EXT);
        attr.setSize(10);
        attr.setTextSearchable(false);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_DATE attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeDate(DKDatastoreICM connection, String name) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_DATE);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_DOUBLE attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeDouble(DKDatastoreICM connection, String name) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_DOUBLE);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_INTEGER attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeInteger(DKDatastoreICM connection, String name) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_INTEGER);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_BLOB attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeBlob(DKDatastoreICM connection, String name, int len, boolean search) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_BLOB);
        attr.setSize(len);
        attr.setTextSearchable(search);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * This methode is internal and create a DK_CM_CLOB attribute.
     * 
     * @param connection
     *            the connection to the database
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return If the attribute exists, false was returned, else true.
     */
    static final boolean createAttributeClob(DKDatastoreICM connection, String name, int len, boolean search) throws Exception {
        if (attributeExists((DKDatastoreDefICM) connection.datastoreDef(), name)) {
            LOGGER.warn("CM8 Datastore attribute already exists: " + name);

            return false;
        }

        DKAttrDefICM attr = new DKAttrDefICM(connection);
        attr.setName(name);
        attr.setType(DK_CM_CLOB);
        attr.setSize(len);
        attr.setTextSearchable(search);
        attr.setNullable(true);
        attr.setUnique(false);
        attr.add();

        return true;
    }

    /**
     * returns true if the attribute exists
     * 
     * @author Thomas Scheffler (yagee)
     * 
     */
    static final boolean attributeExists(DKDatastoreDefICM deficm, String name) throws DKException, Exception {
        return (deficm.retrieveAttr(name) != null);
    }
}
