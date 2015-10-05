/**
 * 
 */
package org.mycore.handle;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class responsible for retrieving and storing the remotely created handle. 
 * 
 * @see MCRHandleCommons#EDA_REPOS_URL
 * @author shermann
 */
public class MCRGetRemoteCreatedHandle extends TimerTask {
    /** The logger for this class */
    private static final Logger LOGGER = Logger.getLogger(MCRGetRemoteCreatedHandle.class);

    private static MCRGetRemoteCreatedHandle instance;

    /** Hidden constructor */
    private MCRGetRemoteCreatedHandle() {

    }

    /**
     * Returns the instance of this class.
     * 
     * @return the instance
     */
    public static MCRGetRemoteCreatedHandle getInstance() {
        if (instance == null) {
            instance = new MCRGetRemoteCreatedHandle();
        }
        return instance;
    }

    /**
     * Reads the handles from the database where the checksum is equal to -1 (meaning a handle:add 
     * requests has already been submitted). For each entry the handle created by the remote system 
     * is requested and finally stored in the database (if status is 'done').
     * */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        MCRGbvHandleChecksumProvider checksumProvider = new MCRGbvHandleChecksumProvider();

        LOGGER.debug("Storing pending handles...");
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.eq("checksum", -1));
            q.add(Restrictions.and(Restrictions.isNotNull("objectSignature"),
                Restrictions.isNotNull("messageSignature")));

            List<MCRHandle> list = q.list();
            for (MCRHandle handle : list) {
                JsonObject message = getMessage(handle.getMessageSignature());

                if (MCRGbvHandleProvider.STATUS_CANCEL.equals(getStatus(message))) {
                    LOGGER.warn("Status is " + MCRGbvHandleProvider.STATUS_CANCEL
                        + "...deleting handle request in database");
                    try {
                        MCRHandleManager.delete(handle);
                    } catch (Throwable e) {
                        LOGGER.error("Could not delete handle " + handle, e);
                    }
                    continue;
                }

                if (!isHandleCreated(message)) {
                    LOGGER.info("Handle for object " + handle.getObjectSignature() + " is not created yet");
                    continue;
                }

                String handleString = getHandle(handle.getObjectSignature());
                if (handleString == null) {
                    LOGGER.error("Handle string for object " + handle.getObjectSignature()
                        + " is null but is marked as created. Please investigate");
                    continue;
                }

                String[] parts = handleString.split("/");
                String localName = parts[1].substring(0, parts[1].length() - 1);

                int checksum = Integer.valueOf(parts[1].substring(parts[1].length() - 1));
                handle.setNamingAuthority(MCRGbvHandleProvider.NAMING_AUTHORITY);
                handle.setNamingAuthoritySegment(MCRGbvHandleProvider.NAMING_AUTHORITY_SEGMENT);
                handle.setLocalName(localName);
                handle.setChecksum(checksum);

                int calculated = checksumProvider.checksum(handle);
                if (checksum != calculated) {
                    LOGGER.warn(MessageFormat.format(
                        "Calculated checksum differs from the one submitted calc={0} != submitted={1}", calculated,
                        checksum));
                }

                session.update(handle);
                MCRDerivate derivateObject = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(handle
                    .getMcrid()));
                MCRPath file = MCRPath.getPath(MCRObjectID.getInstance(handle.getMcrid()).toString(), handle.getPath());
                derivateObject.getDerivate().getOrCreateFileMetadata(file, null, handle.toString());

                MCRMetadataManager.update(derivateObject);
            }
        } catch (Exception ex) {
            MCRHandleManager.LOGGER.error("Could not get handles from database", ex);
            tx.rollback();
        } finally {
            if (tx.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                tx.commit();
            }
            session.disconnect();
            session.close();
        }
    }

    /**
     * @param messageSignature
     * @return
     * @throws IOException
     */
    private JsonObject getMessage(String messageSignature) throws IOException {
        GetMethod get = new GetMethod(MCRHandleCommons.EDA_REPOS_URL + messageSignature);
        JsonObject object = null;

        try {
            LOGGER.info(MessageFormat.format("Sending request to {0} (get message)", get.getURI(), messageSignature));
            MCRHandleCommons.HTTP_CLIENT.executeMethod(get);
            String responseBody = get.getResponseBodyAsString();
            if (responseBody == null) {
                return null;
            }
            object = (JsonObject) new JsonParser().parse(responseBody);
        } finally {
            get.releaseConnection();
        }

        return object;
    }

    /** 
     * Checks if the handle is already created for the given message signature.
     * 
     * @param message  
     * @return true if the handle was created false otherwise
     */
    private boolean isHandleCreated(JsonObject message) {
        String status = getStatus(message);
        if (status == null) {
            return false;
        }

        if (MCRGbvHandleProvider.STATUS_DONE.equals(status)) {
            return true;
        }
        return false;
    }

    String getStatus(JsonObject message) {
        JsonElement statusElement = message.get("status");
        if (statusElement == null) {
            return null;
        }

        return statusElement.getAsString();
    }

    /**
     * Retrieves the handle from the remote system.
     * 
     * @param objectSignature
     * @return the handle if any or null
     */
    private String getHandle(String objectSignature) {
        GetMethod get = new GetMethod(MCRHandleCommons.GBV_OBJECT_REPOS_URL + objectSignature);
        try {
            LOGGER.info(MessageFormat.format("Sending request to {0} (getting handle for object signature {1})",
                get.getURI(), objectSignature));
            MCRHandleCommons.HTTP_CLIENT.executeMethod(get);
            String responseBody = get.getResponseBodyAsString();
            if (responseBody == null) {
                return null;
            }
            JsonObject o = (JsonObject) new JsonParser().parse(responseBody);
            JsonElement handle = o.get("handle");
            if (handle != null) {
                return handle.getAsString();
            }
        } catch (ClassCastException | IOException e) {
            LOGGER.error("Could not get handle value for object signature " + objectSignature, e);
        } finally {
            get.releaseConnection();
        }
        return null;
    }
}
