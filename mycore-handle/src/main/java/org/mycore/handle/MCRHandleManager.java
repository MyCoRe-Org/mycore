package org.mycore.handle;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


/**
 * Class contains convenience functions for handling handles.
 *
 * @author shermann
 */
public class MCRHandleManager {

    static final Logger LOGGER = Logger.getLogger(MCRHandleManager.class);

    /**
     * Convenience function to use in xslt stylesheets.
     *
     * @see MCRHandleManager#isHandleRequested(MCRBase)
     */
    public static boolean isHandleRequested(String base) throws Throwable {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria q = session.createCriteria(MCRHandle.class);
        q.add(Restrictions.eq("mcrid", base));
        return q.list().size() > 0 ? true : false;
    }

    /**
     * Checks whether the are handles or handle requests for a given {@link MCRBase}
     *
     * @return true if a handle request has been issued already
     */
    public static boolean isHandleRequested(MCRBase base) throws Throwable {
        Session session = MCRHIBConnection.instance().getSession();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.eq("mcrid", base.getId().toString()));

            return q.list().size() > 0 ? true : false;
        } catch (Exception ex) {
            throw new Exception("Could not execute query", ex);
        }
    }

    /**
     * When a object signature has been created this method will always return true.
     *
     * @return true if a handle request has been issued already
     */
    public static boolean isHandleRequested(MCRBase base, String path) throws Throwable {
        Session session = MCRHIBConnection.instance().getSession();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.eq("mcrid", base.getId().toString()));
            q.add(Restrictions.eq("path", path));
            q.add(Restrictions.isNotNull("objectSignature"));

            return q.list().size() > 0 ? true : false;
        } catch (Exception ex) {
            throw new Exception("Could not execute query", ex);
        }
    }

    /**
     * Deletes a certain handle from the database.
     *
     * @param handle the handle to delete
     */
    synchronized public static void delete(MCRHandle handle) throws Throwable {
        Session session = MCRHIBConnection.instance().getSession();
        try {
            session.delete(handle);
        } catch (Exception ex) {
            throw new Exception("Could not delete handle", ex);
        }
    }

    /**
     * Deletes all handles registered for the given {@link MCRBase}.
     *
     */
    @SuppressWarnings("unchecked")
    synchronized public static void delete(MCRBase obj) {
        Session session = MCRHIBConnection.instance().getSession();

        Criteria q = session.createCriteria(MCRHandle.class);
        q.add(Restrictions.eq("mcrid", obj.getId().toString()));
        for (MCRHandle handle : (List<MCRHandle>) q.list()) {
            session.delete(handle);
        }
    }

    /**
     * Deletes the handle for the given file both from the local database and from the remote system.
     *
     */
    synchronized public static void delete(MCRPath file) throws Throwable {
        MCRHandle handle = MCRHandleManager.getHandle(file);
        if (handle == null) {
            return;
        }
        LOGGER.info(MessageFormat.format("Deleting handle \"{0}\" from remote system and local database", handle));

        // delete handle from remote system
        int status = requestHandleDelete(handle.getObjectSignature(), handle.getMessageSignature(), file);
        if (!String.valueOf(status).startsWith("2")) {
            LOGGER.warn("Could not post handle delete request");
            return;
        }

        // delete handle from local database
        MCRHandleManager.delete(handle);
    }

    /**
    */
    static private int requestHandleDelete(String objectSignature, String messageSignature, MCRPath file) {
        PostMethod post = new PostMethod(MCRHandleCommons.EDA_REPOS_URL + messageSignature);
        int status = -1;
        try {
            JsonObject jsonDelete = MCRHandleManager.createJson(objectSignature, messageSignature, file);
            post.setRequestEntity(new StringRequestEntity(jsonDelete.toString(), "application/json", "UTF-8"));
            LOGGER.info(MessageFormat.format("Sending request to {0} (request handle delete for object signature {1})", post.getURI(), objectSignature));
            status = MCRHandleCommons.HTTP_CLIENT.executeMethod(post);
        } catch (IOException e) {
            LOGGER.error("Could not request handle:del", e);
        } finally {
            post.releaseConnection();
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    public static MCRHandle getHandle(MCRPath file) throws Throwable {
        String owner = file.getOwner();

        List<MCRHandle> list = new ArrayList<MCRHandle>();
        Session session = MCRHIBConnection.instance().getSession();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.and(Restrictions.eq("mcrid", owner), Restrictions.eq("path", file.getOwnerRelativePath())));

            list = q.list();
        } catch (Exception ex) {
            throw new Exception("Could not execute query", ex);
        }
        return list.size() < 1 ? null : list.get(0);
    }

    /**
     * Retrieves the handle for a given object id and its path
     *
     * @deprecated please use {@link MCRHandleManager#getHandle(MCRBase base)}
     */
    public static List<MCRHandle> getHandle(MCRBase base, String path) throws Throwable {
        return MCRHandleManager.getHandle(base);
    }

    /**
     * Retrieves all handle for a given object id.
     *
     */
    @SuppressWarnings("unchecked")
    public static List<MCRHandle> getHandle(MCRBase base) throws Throwable {
        List<MCRHandle> list = new ArrayList<MCRHandle>();
        Session session = MCRHIBConnection.instance().getSession();
        try {
            Criteria q = session.createCriteria(MCRHandle.class);
            q.add(Restrictions.eq("mcrid", base.getId().toString()));

            list = q.list();
        } catch (Exception ex) {
            throw new Exception("Could not execute query", ex);
        }
        return list;
    }

    /**
     * Method requests a handle for the given file at {@link MCRHandleCommons#EDA_REPOS_URL}.
     *
     * @param file the file this handle is assigned to
     */
    synchronized static public void requestHandle(MCRPath file) throws Throwable {
        Session session = MCRHIBConnection.instance().getSession();
        try {
            MCRIHandleProvider handleProvider = getHandleProvider();
            MCRHandle handle = handleProvider.requestHandle(file);
            if (handle != null) {
                session.saveOrUpdate(handle);
            }
        } catch (Exception ex) {
            throw new Exception("Could not save handle in database", ex);
        }
    }

    /**
     * @return
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     *
     * TODO handle provider to be set by property
     */
    @SuppressWarnings("unchecked")
    private static MCRIHandleProvider getHandleProvider() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<MCRIHandleProvider> c = (Class<MCRIHandleProvider>) Class.forName(MCRGbvHandleProvider.class.getName());
        MCRIHandleProvider provider = c.newInstance();
        return provider;
    }

    /**
     * Creates the json that will be send to {@link MCRGbvHandleProvider#EDA_REPOS_URL}.
     *
     * @param gbvObjectUUID
     * @param messageSignature
     * @return
     */
    private static JsonObject createJson(String gbvObjectUUID, String messageSignature, MCRPath file) {
        JsonObject handleProps = new JsonObject();
        handleProps.addProperty("handle-url", "");
        handleProps.addProperty("handle-email", "");
        handleProps.addProperty("signature", gbvObjectUUID);

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
        toReturn.addProperty("callback", "handle:del");
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
}
