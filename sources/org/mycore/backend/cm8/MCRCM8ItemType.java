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

package org.mycore.backend.cm8;

import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRMetaDefault;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

import com.ibm.mm.sdk.common.DKAttrDefICM;
import com.ibm.mm.sdk.common.DKComponentTypeDefICM;
import com.ibm.mm.sdk.common.DKConstantICM;
import com.ibm.mm.sdk.common.DKDatastoreDefICM;
import com.ibm.mm.sdk.common.DKException;
import com.ibm.mm.sdk.common.DKItemTypeDefICM;
import com.ibm.mm.sdk.common.DKTextIndexDefICM;
import com.ibm.mm.sdk.server.DKDatastoreICM;

/**
 * This class implements all methode for handling the ItemType for a MCRObjectID
 * type on IBM Content Manager 8.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
final class MCRCM8ItemType implements DKConstantICM {

    // internal data
    private static final String META_PACKAGE_NAME = "org.mycore.backend.cm8.";

    /**
     * The constructor of this class.
     */
    MCRCM8ItemType() {
    }

    /**
     * The methode create a new datastore based of given configuration.
     * 
     * @param mcr_type
     *            the MCRObjectID type as string
     * @param mcr_conf
     *            the configuration XML stream as JDOM tree
     * @exception MCRConfigurationException
     *                if the configuration is not correct
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    static void create(String mcr_type, org.jdom.Document mcr_conf)
            throws MCRConfigurationException, MCRPersistenceException {
        Logger logger = MCRCM8ConnectionPool.getLogger();
        try {
            // read the configuration
            MCRConfiguration conf = MCRConfiguration.instance();
            String sb = new String("MCR.persistence_cm8_" + mcr_type);
            String mcr_item_type_name = conf.getString(sb);
            String mcr_item_type_prefix = conf.getString(sb + "_prefix");
            // connect to server
            DKDatastoreICM connection = null;
            DKDatastoreDefICM dsDefICM = null;
            try {
                connection = MCRCM8ConnectionPool.instance().getConnection();
                dsDefICM = (DKDatastoreDefICM) connection.datastoreDef();
                logger.info("CM8 Datastore Creation connected.");
                // create the Attribute for MCRObjectID
                MCRCM8ItemTypeCommon.createAttributeVarChar(connection,
                        mcr_item_type_prefix + "ID", MCRObjectID.MAX_LENGTH,
                        false);
                // create the Attribute for MCR_Label
                MCRCM8ItemTypeCommon.createAttributeVarChar(connection,
                        mcr_item_type_prefix + "label",
                        MCRObject.MAX_LABEL_LENGTH, false);
                // create the Attribut for the TS byte array
                MCRCM8ItemTypeCommon.createAttributeClob(connection,
                        mcr_item_type_prefix + "ts", 100 * 1024, true);
                // create the default attribute type
                MCRCM8ItemTypeCommon.createAttributeVarChar(connection,
                        mcr_item_type_prefix + "type",
                        MCRMetaDefault.DEFAULT_TYPE_LENGTH, false);
                // create the default attribute lang
                MCRCM8ItemTypeCommon.createAttributeVarChar(connection,
                        mcr_item_type_prefix + "lang",
                        MCRMetaDefault.DEFAULT_LANG_LENGTH, false);
                // check for the root itemtype
                try {
                    DKItemTypeDefICM isitemTypeDef = (DKItemTypeDefICM) dsDefICM
                            .retrieveEntity(mcr_item_type_name);
                    if (isitemTypeDef != null) {
                        logger.error("CM8 Datastore Creation itemtype "
                                + mcr_item_type_name + " exist.");
                        return;
                    }
                } catch (DKException e) {
                    logger.error("CM8 Datastore Creation itemtype "
                            + mcr_item_type_name + " exist.");
                    return;
                }

                // create the TIE definition
                DKTextIndexDefICM mcr_item_text_index = MCRCM8ItemTypeCommon
                        .getTextDefinition();

                // create the root itemtype
                DKItemTypeDefICM item_type = new DKItemTypeDefICM(connection);
                logger.info("CM8 Datastore Creation " + mcr_item_type_name);
                item_type.setName(mcr_item_type_name);
                item_type.setDescription(mcr_item_type_name);
                item_type.setClassification(DK_ICM_ITEMTYPE_CLASS_DOC_MODEL);
                item_type.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                DKAttrDefICM attr = (DKAttrDefICM) dsDefICM
                        .retrieveAttr(mcr_item_type_prefix + "ID");
                attr.setNullable(false);
                attr.setUnique(true);
                item_type.addAttr(attr);
                attr = (DKAttrDefICM) dsDefICM
                        .retrieveAttr(mcr_item_type_prefix + "label");
                attr.setNullable(false);
                attr.setUnique(false);
                item_type.addAttr(attr);
                attr = (DKAttrDefICM) dsDefICM
                        .retrieveAttr(mcr_item_type_prefix + "ts");
                attr.setNullable(false);
                attr.setUnique(false);
                attr.setTextSearchable(true);
                attr.setTextIndexDef(mcr_item_text_index);
                item_type.addAttr(attr);

                // get the configuration JDOM root element
                org.jdom.Element mcr_root = mcr_conf.getRootElement();

                // set config element offset to structure
                org.jdom.Element mcr_structure = mcr_root.getChild("structure");
                if (mcr_structure != null) {
                    // Set the structure child component
                    DKComponentTypeDefICM item_structure = new DKComponentTypeDefICM(
                            connection);
                    item_structure.setName(mcr_item_type_prefix + "structure");
                    item_structure.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                    // over all elements
                    List mcr_taglist = mcr_structure.getChildren();
                    for (int i = 0; i < mcr_taglist.size(); i++) {
                        // the tag
                        org.jdom.Element mcr_tag = (org.jdom.Element) mcr_taglist
                                .get(i);
                        String tagname = (String) mcr_tag.getAttribute("name")
                                .getValue();
                        // should it create for search?
                        String parasearch = (String) mcr_tag.getAttribute(
                                "parasearch").getValue();
                        if (parasearch == null) {
                            parasearch = "true";
                        }
                        if (!parasearch.toLowerCase().equals("true")) {
                            continue;
                        }
                        // create the tag child component
                        DKComponentTypeDefICM item_tag = new DKComponentTypeDefICM(
                                connection);
                        item_tag.setName(mcr_item_type_prefix + tagname);
                        item_tag.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                        // over all mcrmeta...
                        List mcr_subtaglist = mcr_tag.getChildren();
                        for (int j = 0; j < mcr_subtaglist.size(); j++) {
                            org.jdom.Element mcr_subtag = (org.jdom.Element) mcr_subtaglist
                                    .get(j);
                            String subtagname = mcr_subtag.getName();
                            if (subtagname.length() <= 7) {
                                continue;
                            }
                            if (!subtagname.substring(0, 7).equals("mcrmeta")) {
                                continue;
                            }
                            String classname = (String) mcr_subtag
                                    .getAttribute("class").getValue();
                            StringBuffer stb = new StringBuffer(128);
                            stb.append(META_PACKAGE_NAME).append("MCRCM8")
                                    .append(
                                            classname.substring(3, classname
                                                    .length()));
                            logger.debug("CM8 Datastore Creation: " + tagname
                                    + " with class " + stb.toString());
                            Object obj = new Object();
                            try {
                                obj = Class.forName(stb.toString())
                                        .newInstance();
                                DKComponentTypeDefICM item_subtag = ((MCRCM8MetaInterface) obj)
                                        .createItemType(mcr_subtag, connection,
                                                dsDefICM, mcr_item_type_prefix,
                                                mcr_item_text_index, "false");
                                item_tag.addSubEntity(item_subtag);
                            } catch (ClassNotFoundException e) {
                                throw new MCRException(classname
                                        + " ClassNotFoundException");
                            } catch (IllegalAccessException e) {
                                throw new MCRException(classname
                                        + " IllegalAccessException");
                            } catch (InstantiationException e) {
                                throw new MCRException(classname
                                        + " InstantiationException");
                            }
                        }
                        item_structure.addSubEntity(item_tag);
                    }
                    item_type.addSubEntity(item_structure);
                }

                // set config element offset to metadata
                org.jdom.Element mcr_metadata = mcr_root.getChild("metadata");
                if (mcr_metadata != null) {
                    DKComponentTypeDefICM item_metadata = new DKComponentTypeDefICM(
                            connection);
                    item_metadata.setName(mcr_item_type_prefix + "metadata");
                    item_metadata.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                    attr = (DKAttrDefICM) dsDefICM
                            .retrieveAttr(mcr_item_type_prefix + "lang");
                    attr.setNullable(true);
                    attr.setUnique(false);
                    item_metadata.addAttr(attr);
                    // over all elements
                    List mcr_taglist = mcr_metadata.getChildren();
                    for (int i = 0; i < mcr_taglist.size(); i++) {
                        // the tag
                        org.jdom.Element mcr_tag = (org.jdom.Element) mcr_taglist
                                .get(i);
                        String tagname = (String) mcr_tag.getAttribute("name")
                                .getValue();
                        String parasearch = (String) mcr_tag.getAttribute(
                                "parasearch").getValue();
                        if (parasearch == null) {
                            parasearch = "true";
                        }
                        if (!parasearch.toLowerCase().equals("true")) {
                            continue;
                        }
                        String textsearch = (String) mcr_tag.getAttribute(
                                "textsearch").getValue();
                        if (textsearch == null) {
                            textsearch = "false";
                        }
                        DKComponentTypeDefICM item_tag = new DKComponentTypeDefICM(
                                connection);
                        item_tag.setName(mcr_item_type_prefix + tagname);
                        item_tag.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                        // add lang attribute to tag
                        attr = (DKAttrDefICM) dsDefICM
                                .retrieveAttr(mcr_item_type_prefix + "lang");
                        attr.setNullable(true);
                        attr.setUnique(false);
                        item_tag.addAttr(attr);
                        // over all mcrmeta...
                        List mcr_subtaglist = mcr_tag.getChildren();
                        for (int j = 0; j < mcr_subtaglist.size(); j++) {
                            org.jdom.Element mcr_subtag = (org.jdom.Element) mcr_subtaglist
                                    .get(j);
                            String subtagname = mcr_subtag.getName();
                            if (subtagname.length() <= 7) {
                                continue;
                            }
                            if (!subtagname.substring(0, 7).equals("mcrmeta")) {
                                continue;
                            }
                            String classname = (String) mcr_subtag
                                    .getAttribute("class").getValue();
                            StringBuffer stb = new StringBuffer(128);
                            stb.append(META_PACKAGE_NAME).append("MCRCM8")
                                    .append(
                                            classname.substring(3, classname
                                                    .length()));
                            logger.debug("CM8 Datastore Creation: " + tagname
                                    + " with class " + stb.toString());
                            Object obj = new Object();
                            try {
                                obj = Class.forName(stb.toString())
                                        .newInstance();
                                DKComponentTypeDefICM item_subtag = ((MCRCM8MetaInterface) obj)
                                        .createItemType(mcr_subtag, connection,
                                                dsDefICM, mcr_item_type_prefix,
                                                mcr_item_text_index, textsearch);
                                item_tag.addSubEntity(item_subtag);
                            } catch (ClassNotFoundException e) {
                                throw new MCRException(classname
                                        + " ClassNotFoundException");
                            } catch (IllegalAccessException e) {
                                throw new MCRException(classname
                                        + " IllegalAccessException");
                            } catch (InstantiationException e) {
                                throw new MCRException(classname
                                        + " InstantiationException");
                            }
                        }
                        item_metadata.addSubEntity(item_tag);
                    }
                    item_type.addSubEntity(item_metadata);
                }

                // set config element offset to derivate
                org.jdom.Element mcr_derivate = mcr_root.getChild("derivate");
                if (mcr_derivate != null) {
                    DKComponentTypeDefICM item_derivate = new DKComponentTypeDefICM(
                            connection);
                    item_derivate.setName(mcr_item_type_prefix + "derivate");
                    item_derivate.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                    // over all elements
                    List mcr_taglist = mcr_derivate.getChildren();
                    for (int i = 0; i < mcr_taglist.size(); i++) {
                        // the tag
                        org.jdom.Element mcr_tag = (org.jdom.Element) mcr_taglist
                                .get(i);
                        String tagname = (String) mcr_tag.getAttribute("name")
                                .getValue();
                        String parasearch = (String) mcr_tag.getAttribute(
                                "parasearch").getValue();
                        if (parasearch == null) {
                            parasearch = "true";
                        }
                        if (!parasearch.toLowerCase().equals("true")) {
                            continue;
                        }
                        String textsearch = (String) mcr_tag.getAttribute(
                                "textsearch").getValue();
                        if (textsearch == null) {
                            textsearch = "false";
                        }
                        DKComponentTypeDefICM item_tag = new DKComponentTypeDefICM(
                                connection);
                        item_tag.setName(mcr_item_type_prefix + tagname);
                        item_tag.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                        // add lang attribute to the tag
                        attr = (DKAttrDefICM) dsDefICM
                                .retrieveAttr(mcr_item_type_prefix + "lang");
                        attr.setNullable(true);
                        attr.setUnique(false);
                        item_tag.addAttr(attr);
                        // over all mcrmeta...
                        List mcr_subtaglist = mcr_tag.getChildren();
                        for (int j = 0; j < mcr_subtaglist.size(); j++) {
                            org.jdom.Element mcr_subtag = (org.jdom.Element) mcr_subtaglist
                                    .get(j);
                            String subtagname = mcr_subtag.getName();
                            if (subtagname.length() <= 7) {
                                continue;
                            }
                            if (!subtagname.substring(0, 7).equals("mcrmeta")) {
                                continue;
                            }
                            String classname = (String) mcr_subtag
                                    .getAttribute("class").getValue();
                            StringBuffer stb = new StringBuffer(128);
                            stb.append(META_PACKAGE_NAME).append("MCRCM8")
                                    .append(
                                            classname.substring(3, classname
                                                    .length()));
                            logger.debug("CM8 Datastore Creation: " + tagname
                                    + " with class " + stb.toString());
                            Object obj = new Object();
                            try {
                                obj = Class.forName(stb.toString())
                                        .newInstance();
                                DKComponentTypeDefICM item_subtag = ((MCRCM8MetaInterface) obj)
                                        .createItemType(mcr_subtag, connection,
                                                dsDefICM, mcr_item_type_prefix,
                                                mcr_item_text_index, textsearch);
                                item_tag.addSubEntity(item_subtag);
                            } catch (ClassNotFoundException e) {
                                throw new MCRException(classname
                                        + " ClassNotFoundException");
                            } catch (IllegalAccessException e) {
                                throw new MCRException(classname
                                        + " IllegalAccessException");
                            } catch (InstantiationException e) {
                                throw new MCRException(classname
                                        + " InstantiationException");
                            }
                        }
                        item_derivate.addSubEntity(item_tag);
                    }
                    item_type.addSubEntity(item_derivate);
                }

                // the service part
                org.jdom.Element mcr_service = mcr_root.getChild("service");
                DKComponentTypeDefICM item_service = new DKComponentTypeDefICM(
                        connection);
                item_service.setName(mcr_item_type_prefix + "service");
                item_service.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                // over all elements
                List mcr_taglist = mcr_service.getChildren();
                for (int i = 0; i < mcr_taglist.size(); i++) {
                    // the tag
                    org.jdom.Element mcr_tag = (org.jdom.Element) mcr_taglist
                            .get(i);
                    String tagname = (String) mcr_tag.getAttribute("name")
                            .getValue();
                    String parasearch = (String) mcr_tag.getAttribute(
                            "parasearch").getValue();
                    if (parasearch == null) {
                        parasearch = "true";
                    }
                    if (!parasearch.toLowerCase().equals("true")) {
                        continue;
                    }
                    String textsearch = (String) mcr_tag.getAttribute(
                            "textsearch").getValue();
                    if (textsearch == null) {
                        textsearch = "false";
                    }
                    DKComponentTypeDefICM item_tag = new DKComponentTypeDefICM(
                            connection);
                    item_tag.setName(mcr_item_type_prefix + tagname);
                    item_tag.setDeleteRule(DK_ICM_DELETE_RULE_CASCADE);
                    // add lang attribute to the tag
                    attr = (DKAttrDefICM) dsDefICM
                            .retrieveAttr(mcr_item_type_prefix + "lang");
                    attr.setNullable(true);
                    attr.setUnique(false);
                    item_tag.addAttr(attr);
                    // over all mcrmeta...
                    List mcr_subtaglist = mcr_tag.getChildren();
                    for (int j = 0; j < mcr_subtaglist.size(); j++) {
                        org.jdom.Element mcr_subtag = (org.jdom.Element) mcr_subtaglist
                                .get(j);
                        String subtagname = mcr_subtag.getName();
                        if (subtagname.length() <= 7) {
                            continue;
                        }
                        if (!subtagname.substring(0, 7).equals("mcrmeta")) {
                            continue;
                        }
                        String classname = (String) mcr_subtag.getAttribute(
                                "class").getValue();
                        StringBuffer stb = new StringBuffer(128);
                        stb.append(META_PACKAGE_NAME).append("MCRCM8").append(
                                classname.substring(3, classname.length()));
                        logger.debug("CM8 Datastore Creation: " + tagname
                                + " with class " + stb.toString());
                        Object obj = new Object();
                        try {
                            obj = Class.forName(stb.toString()).newInstance();
                            DKComponentTypeDefICM item_subtag = ((MCRCM8MetaInterface) obj)
                                    .createItemType(mcr_subtag, connection,
                                            dsDefICM, mcr_item_type_prefix,
                                            mcr_item_text_index, textsearch);
                            item_tag.addSubEntity(item_subtag);
                        } catch (ClassNotFoundException e) {
                            throw new MCRException(classname
                                    + " ClassNotFoundException");
                        } catch (IllegalAccessException e) {
                            throw new MCRException(classname
                                    + " IllegalAccessException");
                        } catch (InstantiationException e) {
                            throw new MCRException(classname
                                    + " InstantiationException");
                        }
                    }
                    item_service.addSubEntity(item_tag);
                }
                item_type.addSubEntity(item_service);

                item_type.add();
                logger.info("CM8 Datastore Creation: " + mcr_item_type_name
                        + " is created.");
            } finally {
                MCRCM8ConnectionPool.instance().releaseConnection(connection);
            }
        } catch (Exception e) {
            throw new MCRPersistenceException(e.getMessage(), e);
        }
    }

}