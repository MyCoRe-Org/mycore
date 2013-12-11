package org.mycore.services.handle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.iview2.services.MCRIView2Tools;
import org.mycore.services.handle.hibernate.tables.MCRHandle;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Class responsible for storing object signatures in a remote system. 
 * 
 * @see MCRDigicultHandleProvider#DIGICULT_OBJECT_REPOS_URL
 * @author shermann
 */
public class MCRRequestHandleAdd extends TimerTask {
    /** Singleton. */
    private static MCRRequestHandleAdd instance;

    /** The logger for this class */
    private static Logger LOGGER = Logger.getLogger(MCRRequestHandleAdd.class);

    private MCRRequestHandleAdd() {
    }

    /**
     * @return the singleton instance of this class
     */
    public static MCRRequestHandleAdd getInstance() {
        if (instance == null) {
            instance = new MCRRequestHandleAdd();
        }
        return instance;
    }

    /**
     * Method reads from the mcrhandle table to get pending handle requests 
     * (handles with an object signature but with no message signature). A request for each object 
     * given by its signature is then send to {@link MCRDigicultHandleProvider#EDA_REPOS_URL}
     * */
    @SuppressWarnings("unchecked")
    public void run() {
        LOGGER.debug("Checking for new handle:add entries...");
        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = session.beginTransaction();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.and(Restrictions.isNotNull("objectSignature"), Restrictions.isNull("messageSignature")));
            List<MCRHandle> list = q.list();

            for (MCRHandle handle : list) {
                String messageSignature = UUID.randomUUID().toString().replace("-", "");

                int status = requestHandleAdd(handle.getObjectSignature(), messageSignature,
                        MCRFile.getMCRFile(MCRObjectID.getInstance(handle.getMcrid()), handle.getPath()));

                if (!String.valueOf(status).startsWith("2")) {
                    LOGGER.warn("Could not post handle:add");
                    continue;
                }
                handle.setMessageSignature(messageSignature);
                session.update(handle);
            }
        } catch (Exception ex) {
            MCRHandleManager.LOGGER.error("Could not get handles from database", ex);
            tx.rollback();
        } finally {
            tx.commit();
            session.disconnect();
        }
    }

    /**
     * Creates the json that will be send to {@link MCRHandleCommons#EDA_REPOS_URL}.
     * 
     * @param digicultObjectUUID
     * @param messageSignature 
     * @return
     */
    private JsonObject createJson(String digicultObjectUUID, String messageSignature, MCRFile file) throws URISyntaxException {
        JsonObject handleProps = new JsonObject();
        handleProps.addProperty("handle-url", getHandleURL(file));
        handleProps.addProperty("handle-email", MCRHandleCommons.HANDLE_MAIL);
        handleProps.addProperty("signature", digicultObjectUUID);

        JsonArray args = new JsonArray();
        args.add(handleProps);

        JsonObject toReturn = new JsonObject();
        // when requesting a handle this should be set to pending
        toReturn.addProperty("status", "pending");
        // the create date of the object the handle is for
        toReturn.addProperty("ctime", MCRHandleCommons.DATE_FORMAT.format(new Date()));
        toReturn.add("args", args);
        toReturn.addProperty("cmodel", "eda:action");
        toReturn.addProperty("agent", "ipython");
        // the action to take
        toReturn.addProperty("callback", "handle:add");
        // date when this action is due (usually right now would be fine)
        toReturn.addProperty("stime", String.valueOf(System.currentTimeMillis()));
        // context uuids for messages (this is fixed)
        toReturn.addProperty("context", MCRHandleCommons.EDA_MESSAGE_CONTEXT_UUID);
        //signature of message
        toReturn.addProperty("signature", messageSignature);
        // just the test owner
        toReturn.addProperty("owner", MCRHandleCommons.DEFAULT_OWNER);

        return toReturn;
    }

    /**
     * @param file
     * @return
     */
    private String getHandleURL(MCRFile file) throws URISyntaxException {
        String url = MCRIView2Tools.getViewerURL(file);
        if (url == null) {
            url = MCRServlet.getServletBaseURL() + MCRFileNodeServlet.class.getSimpleName() + file.getAbsolutePath();
            return url;
        }
        return url;
    }

    /**
     * Actually sends the request to {@link MCRDigicultHandleProvider#EDA_REPOS_URL}.
     * 
     * @param file the file the handle is requested for
     * @param objectSignature the signature of the object at {@link MCRHandleCommons#DIGICULT_OBJECT_REPOS_URL}

     * @return the http status, a status of -1 indicates the request was not send at all
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws HttpException
     */
    private int requestHandleAdd(String objectSignature, String messageSignature, MCRFile file) throws URISyntaxException {
        PostMethod post = new PostMethod(MCRHandleCommons.EDA_REPOS_URL + messageSignature);
        int status = -1;
        try {
            JsonObject jsonAdd = createJson(objectSignature, messageSignature, file);
            post.setRequestEntity(new StringRequestEntity(jsonAdd.toString(), "application/json", "UTF-8"));
            LOGGER.info(MessageFormat.format("Sending request to {0} (request handle for object signature {1})", post.getURI(), objectSignature));
            status = MCRHandleCommons.HTTP_CLIENT.executeMethod(post);
        } catch (IOException e) {
            LOGGER.error("Could not request handle:add", e);
        } finally {
            post.releaseConnection();
        }
        return status;
    }
}