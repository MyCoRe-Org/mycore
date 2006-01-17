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

package org.mycore.datamodel.metadata;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;

/**
 * This class is a abstract basic class for objects in the MyCoRe Project. It is
 * the frame to produce a full functionality object.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public abstract class MCRBase {
    /**
     * constant value for the object id length
     */
    public final static int MAX_LABEL_LENGTH = 256;

    // from configuration
    protected static MCRConfiguration mcr_conf = null;

    protected static String mcr_encoding = null;

    protected static String persist_name;

    protected static String persist_type;

    // the DOM document
    protected org.jdom.Document jdom_document = null;

    // the object content
    protected MCRObjectID mcr_id = null;

    protected String mcr_label = null;

    protected String mcr_schema = null;

    protected MCRObjectService mcr_service = null;

    // other
    protected static String NL;

    protected static String SLASH;

    // logger
    static Logger logger = Logger.getLogger(MCRBase.class.getPackage().getName());

    /**
     * Load static data for all MCRObjects
     */
    static {
        NL = System.getProperty("line.separator");
        SLASH = System.getProperty("file.separator");

        try {
            // Load the configuration
            mcr_conf = MCRConfiguration.instance();

            // Default Encoding
            mcr_encoding = mcr_conf.getString("MCR.metadata_default_encoding", MCRDefaults.ENCODING);
            logger.debug("Encoding = " + mcr_encoding);
        } catch (Exception e) {
            logger.error("error occured: ", e);
            throw new MCRException(e.getMessage(), e);
        }
    }

    /**
     * This is the constructor of the MCRBase class. It make an instance of the
     * parser class and the metadata class. <br>
     * 
     * @exception MCRException
     *                general Exception of MyCoRe
     * @exception MCRConfigurationException
     *                a special exception for configuartion data
     */
    public MCRBase() throws MCRException, MCRConfigurationException {
        mcr_id = new MCRObjectID();
        mcr_label = new String("");
        mcr_schema = new String("");

        // Service class
        mcr_service = new MCRObjectService();
    }

    /**
     * This methode return the object id. If this is not set, null was returned.
     * 
     * @return the id as MCRObjectID
     */
    public final MCRObjectID getId() {
        return mcr_id;
    }

    /**
     * This methode return the object label. If this is not set, null was
     * returned.
     * 
     * @return the lable as a string
     */
    public final String getLabel() {
        return mcr_label;
    }

    /**
     * This methode return the object schema. If this is not set, null was
     * returned.
     * 
     * @return the schema as a string
     */
    public final String getSchema() {
        return mcr_schema;
    }

    /**
     * This methode return the instance of the MCRObjectService class. If this
     * was not found, null was returned.
     * 
     * @return the instance of the MCRObjectService class
     */
    public final MCRObjectService getService() {
        return mcr_service;
    }

    /**
     * This methode read the XML input stream from an URI into a temporary DOM
     * and check it with XSchema file.
     * 
     * @param uri
     *            an URI
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public abstract void setFromURI(String uri) throws MCRException;

    /**
     * This methode read the XML input stream from a byte array into JDOM and
     * check it with XSchema file.
     * 
     * @param xml
     *            a XML string
     * @exception MCRException
     *                general Exception of MyCoRe
     */
    public abstract void setFromXML(byte[] xml, boolean valid) throws MCRException;

    /**
     * This methode set the object ID.
     * 
     * @param id
     *            the object ID
     */
    public final void setId(MCRObjectID id) {
        if (id.isValid()) {
            mcr_id = id;
        }
    }

    /**
     * This methode set the object label.
     * 
     * @param label
     *            the object label
     */
    public final void setLabel(String label) {
        mcr_label = label.trim();

        if (mcr_label.length() > MAX_LABEL_LENGTH) {
            mcr_label = mcr_label.substring(0, MAX_LABEL_LENGTH);
        }
    }

    /**
     * This methode set the object schema.
     * 
     * @param schema
     *            the object schema
     */
    public final void setSchema(String schema) {
        if (schema == null) {
            mcr_schema = "";

            return;
        }

        mcr_schema = schema.trim();
    }

    /**
     * This methode set the object MCRObjectService.
     * 
     * @param service
     *            the object MCRObjectService part
     */
    public final void setService(MCRObjectService service) {
        if (service != null) {
            mcr_service = service;
        }
    }

    /**
     * This methode create a XML stream for all object data.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Document with the XML data of the object as byte array
     */
    public abstract org.jdom.Document createXML() throws MCRException;

    /**
     * The methode create the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public abstract void createInDatastore() throws MCRPersistenceException, MCRActiveLinkException;

    /**
     * The methode delete the object in the data store.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public abstract void deleteFromDatastore(String id) throws MCRPersistenceException, MCRActiveLinkException;

    /**
     * The methode receive the object for the given MCRObjectID and stored it in
     * this MCRObject.
     * 
     * @param id
     *            the object ID
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public abstract void receiveFromDatastore(String id) throws MCRPersistenceException;

    /**
     * The methode receive the object for the given MCRObjectID and returned it
     * as XML stream.
     * 
     * @param id
     *            the object ID
     * @return the XML stream of the object as string
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     */
    public abstract byte[] receiveXMLFromDatastore(String id) throws MCRPersistenceException;

    /**
     * The methode update the object in the data store.
     * 
     * @exception MCRPersistenceException
     *                if a persistence problem is occured
     * @throws MCRActiveLinkException
     */
    public abstract void updateInDatastore() throws MCRPersistenceException, MCRActiveLinkException;

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the mcr_id value is valid
     * <li>the label value is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    public boolean isValid() {
        if (!mcr_id.isValid()) {
            return false;
        }

        if ((mcr_label == null) || ((mcr_label = mcr_label.trim()).length() == 0)) {
            return false;
        }

        if ((mcr_schema == null) || ((mcr_schema = mcr_schema.trim()).length() == 0)) {
            return false;
        }

        return true;
    }
}
