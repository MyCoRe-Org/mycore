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

package org.mycore.backend.cm8.datatypes;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;
import com.ibm.mm.sdk.common.DKTextIndexDefICM;

/**
 * Provides methods to easily create attributes in ContentManager Library
 * Server.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public final class MCRCM8AttributeUtils implements DKConstantICM {

    private static final Logger LOGGER = Logger.getLogger(MCRCM8AttributeUtils.class);

    private static final DKTextIndexDefICM mcrTextIndexDef;
    static {
        mcrTextIndexDef = new DKTextIndexDefICM();
        final MCRConfiguration conf = MCRConfiguration.instance();

        final int text_commitcount = conf.getInt("MCR.persistence_cm8_textsearch_commitcount", 1);
        mcrTextIndexDef.setCommitCount(text_commitcount);
        mcrTextIndexDef.setFormat(DKTextIndexDefICM.TEXT_INDEX_DOC_FORMAT_TEXT);

        final int text_ccsid = conf.getInt("MCR.persistence_cm8_textsearch_ccsid", 850);
        mcrTextIndexDef.setIndexCCSID(text_ccsid);

        final String text_indexdir = conf.getString("MCR.persistence_cm8_textsearch_indexdir", "/home/icmadmin/index");
        mcrTextIndexDef.setIndexDir(text_indexdir);

        final String text_lang = conf.getString("MCR.persistence_cm8_textsearch_lang", "DE");
        mcrTextIndexDef.setIndexLangCode(text_lang);

        final int text_minchanges = conf.getInt("MCR.persistence_cm8_textsearch_minchanges", 1);
        mcrTextIndexDef.setMinChanges(text_minchanges);

        final String text_updatefreq = conf.getString("MCR.persistence_cm8_textsearch_updatefreq", "");
        mcrTextIndexDef.setUpdateFrequency(text_updatefreq);

        final String text_workingdir = conf.getString("MCR.persistence_cm8_textsearch_workingdir",
                "/home/icmadmin/work");
        mcrTextIndexDef.setWorkingDir(text_workingdir);
        LOGGER.debug("CM8 TextSearch - CommitCount = " + mcrTextIndexDef.getCommitCount());
        LOGGER.debug("CM8 TextSearch - Format = " + mcrTextIndexDef.getFormat());
        LOGGER.debug("CM8 TextSearch - CCSID = " + mcrTextIndexDef.getIndexCCSID());
        LOGGER.debug("CM8 TextSearch - IndexDir = " + mcrTextIndexDef.getIndexDir());
        LOGGER.debug("CM8 TextSearch - Lang = " + mcrTextIndexDef.getIndexLangCode());
        LOGGER.debug("CM8 TextSearch - MinChanges = " + mcrTextIndexDef.getMinChanges());
        LOGGER.debug("CM8 TextSearch - UpdateFreq = " + mcrTextIndexDef.getUpdateFrequency());
        LOGGER.debug("CM8 TextSearch - WorkingDir = " + mcrTextIndexDef.getWorkingDir());
    }

    private MCRCM8AttributeUtils() {
    }

    /**
     * This method read the configuration and return the DKTextIndexDefICM NSE
     * definition.
     * 
     * @return the DKTextIndexDefICM NSE definition.
     */
    public static final DKTextIndexDefICM getTextDefinition() {
        return mcrTextIndexDef;
    }

    /**
     * creates a DK_CM_VARCHAR attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return returns false if the attribute exists, else true.
     */
    public static final boolean createAttributeVarChar(final DKDatastoreDefICM defICM, final String name,
            final int len, final boolean search) throws Exception {
        return createAttribute(defICM, name, DK_CM_VARCHAR, len, search);
    }

    /**
     * creates a DK_CM_BOOLEAN attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeBoolean(final DKDatastoreDefICM defICM, final String name) throws Exception {
        return createAttribute(defICM, name, DK_CM_VARCHAR, 10, false);
    }

    /**
     * creates a DK_CM_DATE attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeDate(final DKDatastoreDefICM defICM, final String name) throws Exception {
        return createAttribute(defICM, name, DK_CM_DATE, 0, false);
    }

    /**
     * creates a DK_CM_DATE attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeTimestamp(final DKDatastoreDefICM defICM, final String name) throws Exception {
        return createAttribute(defICM, name, DK_CM_TIMESTAMP, 0, false);
    }

    /**
     * creates a DK_CM_DOUBLE attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeDouble(final DKDatastoreDefICM defICM, final String name) throws Exception {
        return createAttribute(defICM, name, DK_CM_DOUBLE, 0, false);
    }

    /**
     * creates a DK_CM_INTEGER attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeInteger(final DKDatastoreDefICM defICM, final String name) throws Exception {
        return createAttribute(defICM, name, DK_CM_INTEGER, 0, false);
    }

    /**
     * creates a DK_CM_BLOB attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeBlob(final DKDatastoreDefICM defICM, final String name, final int len,
            final boolean search) throws Exception {
        return createAttribute(defICM, name, DK_CM_BLOB, len, search);
    }

    /**
     * creates a DK_CM_CLOB attribute.
     * 
     * @param defICM
     *            datastore definition of ContentManager
     * @param name
     *            the name of the attribute
     * @param len
     *            the len of the character field
     * @param search
     *            ist true, if the attribute should text searchable
     * 
     * @return returns false if the attribute exists, else true.
     */
    static final boolean createAttributeClob(final DKDatastoreDefICM defICM, final String name, final int len,
            final boolean search) throws Exception {
        return createAttribute(defICM, name, DK_CM_CLOB, len, search);
    }

    private static final boolean createAttribute(final DKDatastoreDefICM defICM, final String name, final short type,
            final int size, final boolean textSearchable) throws DKException, Exception {
        if (attributeExists(defICM, name)) {
            LOGGER.info("CM8 Datastore attribute already exists: " + name);
            return false;
        }
        LOGGER.info("Creating attribute with name: " + name);
        final DKAttrDefICM attr = new DKAttrDefICM(defICM.getDatastore());
        attr.setName(name);
        attr.setType(type);
        if (type == DK_CM_VARCHAR) {
            attr.setStringType(DK_CM_ATTR_VAR_ALPHANUM_EXT);
        }
        attr.setTextSearchable(textSearchable);
        if (size > 0) {
            attr.setSize(size);
        }
        attr.add();
        return true;
    }

    /**
     * returns true if the attribute exists
     */
    static final boolean attributeExists(final DKDatastoreDefICM defICM, final String name) throws DKException,
            Exception {
        return defICM.retrieveAttr(name) != null;
    }
}
