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
import org.mycore.common.MCRPersistenceException;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKTextIndexDefICM;
import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * This class implements the interface for the CM8 persistence layer for the
 * data model type MetaClassification.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCM8MetaClassification implements DKConstantICM, MCRCM8MetaInterface {
    /**
     * This method create a DKComponentTypeDefICM to create a complete ItemType
     * from the configuration.
     * 
     * @param element
     *            a MCR datamodel element as JDOM Element
     * @param connection
     *            the connection to the CM8 datastore
     * @param dsDefICM
     *            the datastore definition
     * @param prefix
     *            the prefix name for the item type
     * @param textindex
     *            the definition of the text search index
     * @param textserach
     *            the flag to use textsearch as string (the value has no effect
     *            for this class)
     * @return a DKComponentTypeDefICM for the MCR datamodel element
     * @exception MCRPersistenceException
     *                general Exception of MyCoRe CM8
     */
    public DKComponentTypeDefICM createItemType(org.jdom.Element element, DKDatastoreICM connection, DKDatastoreDefICM dsDefICM, String prefix, DKTextIndexDefICM textindex, String textsearch) throws MCRPersistenceException {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        String subtagname = prefix + (String) element.getAttribute("name").getValue();
        String classname = prefix + "classid";
        String categname = prefix + "categid";
        int clalen = org.mycore.datamodel.metadata.MCRMetaClassification.MAX_CLASSID_LENGTH;
        int catlen = org.mycore.datamodel.metadata.MCRMetaClassification.MAX_CATEGID_LENGTH;

        DKComponentTypeDefICM lt = new DKComponentTypeDefICM(connection);

        try {
            // create component child
            lt.setName(subtagname);
            lt.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);

            DKAttrDefICM attr;

            // create the classid attribute for the data content
            MCRCM8ItemTypeCommon.createAttributeVarChar(connection, classname, clalen, false);

            // add type attribute
            attr = (DKAttrDefICM) dsDefICM.retrieveAttr(prefix + "type");
            attr.setNullable(true);
            attr.setUnique(false);
            lt.addAttr(attr);

            // add the value attribute
            attr = (DKAttrDefICM) dsDefICM.retrieveAttr(classname);
            attr.setNullable(true);
            attr.setUnique(false);
            lt.addAttr(attr);

            // create the categid attribute for the data content
            MCRCM8ItemTypeCommon.createAttributeVarChar(connection, categname, catlen, false);

            // add the value attribute
            attr = (DKAttrDefICM) dsDefICM.retrieveAttr(categname);
            attr.setNullable(true);
            attr.setUnique(false);
            lt.addAttr(attr);
        } catch (Exception e) {
            throw new MCRPersistenceException(e.getMessage(), e);
        }

        return lt;
    }
}
