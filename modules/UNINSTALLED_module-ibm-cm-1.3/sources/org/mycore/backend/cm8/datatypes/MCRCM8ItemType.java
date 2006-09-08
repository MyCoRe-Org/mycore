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

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jaxen.XPath;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.cm8.MCRCM8DatastorePool;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetaDefault;
import org.mycore.datamodel.metadata.MCRObjectID;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;
import com.ibm.mm.sdk.common.DKItemTypeDefICM;
import com.ibm.mm.sdk.common.dkDatastore;

/**
 * This class implements all methode for handling the ItemType for a MCRObjectID
 * type on IBM Content Manager 8.
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public final class MCRCM8ItemType implements DKConstantICM {
    private static final Logger LOGGER = Logger.getLogger(MCRCM8ItemType.class);

    private static final String META_PACKAGE_NAME = "org.mycore.backend.cm8.datatypes";

    /**
     * The constructor of this class.
     */
    MCRCM8ItemType() {
    }

    /**
     * The methode create a new datastore based of given configuration.
     * 
     * @param mcrType
     *            the MCRObjectID type as string
     * @param datamodel
     *            the configuration XML stream as JDOM tree
     * @exception MCRConfigurationException
     *                if the configuration is not correct
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public static void create(final String mcrType, final org.jdom.Document datamodel)
            throws MCRConfigurationException, MCRPersistenceException {

        try {
            // read the configuration
            final MCRConfiguration conf = MCRConfiguration.instance();
            final String sb = new String("MCR.persistence_cm8_" + mcrType);
            final String itemTypeName = conf.getString(sb);
            final String itemPrefix = cutString(conf.getString(sb + "_prefix"), 2);
            datamodel.setProperty("itemPrefix", itemPrefix);

            final dkDatastore dataStore = MCRCM8DatastorePool.instance().getDatastore();
            final DKDatastoreDefICM dsDefICM = (DKDatastoreDefICM) dataStore.datastoreDef();
            LOGGER.info("Acquired dkDatastore from Pool.");

            try {
                final DKItemTypeDefICM isitemTypeDef = (DKItemTypeDefICM) dsDefICM.retrieveEntity(itemTypeName);
                if (isitemTypeDef != null) {
                    LOGGER.error("CM8 Datastore Creation itemtype " + itemTypeName + " exist.");
                    return;
                }
            } catch (final DKException e) {
                LOGGER.error("CM8 Datastore Creation itemtype " + itemTypeName + " exist.");
                return;
            }

            try {

                // create the Attribut for the TS byte array
                // MCRCM8ItemTypeUtils.createAttributeClob(connection, "mcrts",
                // 100 * 1024, true);
                // check for the root itemtype
                // // create the TIE definition
                // final DKTextIndexDefICM mcr_item_text_index =
                // MCRCM8ItemTypeUtils.getTextDefinition();
                // create the root itemtype
                final DKItemTypeDefICM itemType = new DKItemTypeDefICM(dataStore);
                LOGGER.info("CM8 Datastore Creation " + itemTypeName);
                itemType.setName(itemTypeName);
                itemType.setDescription(itemTypeName);
                itemType.setClassification(DK_ICM_ITEMTYPE_CLASS_DOC_MODEL);
                itemType.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);

                // create the default attribute type
                MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, "mcrType", MCRMetaDefault.DEFAULT_TYPE_LENGTH,
                        false);
                // create the default attribute lang
                MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, "mcrLang", MCRMetaDefault.DEFAULT_LANG_LENGTH,
                        false);
                // create the Attribute for MCRObjectID
                itemType.addAttr(getVarCharAttr(dsDefICM, "mcrID", MCRObjectID.MAX_LENGTH, false, true, false));
                itemType.addAttr(getVarCharAttr(dsDefICM, "mcrLabel", MCRBase.MAX_LABEL_LENGTH, true, false, false));
                // attr = (DKAttrDefICM) dsDefICM.retrieveAttr("mcrts");
                // attr.setNullable(false);
                // attr.setUnique(false);
                // attr.setTextSearchable(true);
                // attr.setTextIndexDef(mcr_item_text_index);
                // item_type.addAttr(attr);

                buildChildComponents(datamodel, dsDefICM, itemType);

                itemType.add();
                LOGGER.info("CM8 Datastore Creation: " + itemTypeName + " is created.");
            } finally {
                MCRCM8DatastorePool.instance().releaseDatastore(dataStore);
            }
        } catch (final Exception e) {
            throw new MCRPersistenceException(e.getMessage(), e);
        }
    }

    /**
     * @param str
     * @return
     */
    private static String cutString(final String str, final int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, 13);
    }

    private static DKAttrDefICM getVarCharAttr(final DKDatastoreDefICM dsDefICM, final String name, final int size,
            final boolean textSearchable, final boolean unique, final boolean nullable) throws Exception, DKException {
        MCRCM8AttributeUtils.createAttributeVarChar(dsDefICM, name, size, textSearchable);
        final DKAttrDefICM attr = (DKAttrDefICM) dsDefICM.retrieveAttr("mcrID");
        attr.setNullable(nullable);
        attr.setUnique(unique);
        return attr;
    }

    private static void buildChildComponents(final Document metamodel, final DKDatastoreDefICM dsDefICM,
            final DKItemTypeDefICM itemType) throws DKException, Exception {
        // get all elements with class attribute but ignore MCRMetaAccessRule
        final XPath path = new JDOMXPath("/configuration/*/element/*[@class != 'MCRMetaAccessRule']");
        LOGGER.debug("Selecting metadata nodes");
        final List classNodes = path.selectNodes(metamodel);
        for (final Iterator it = classNodes.iterator(); it.hasNext();) {
            final Element classNode = (Element) it.next();
            final String classname = classNode.getAttributeValue("class");
            final StringBuffer stb = new StringBuffer(META_PACKAGE_NAME).append(".MCRCM8").append(
                    classname.substring(3, classname.length()));
            LOGGER.debug("CM8 Datastore create sub entity for: " + classNode.getAttributeValue("name") + " with class "
                    + stb.toString());
            final MCRCM8ComponentType meta = (MCRCM8ComponentType) Class.forName(stb.toString()).newInstance();
            meta.setDsDefICM(dsDefICM);
            meta.setComponentNamePrefix(metamodel.getProperty("itemPrefix").toString());
            final DKComponentTypeDefICM subItem = meta.createComponentType(classNode);
            itemType.addSubEntity(subItem);
        }
    }
}
