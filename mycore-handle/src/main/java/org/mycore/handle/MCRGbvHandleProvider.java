/**
 *
 */
package org.mycore.handle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.gson.JsonObject;


/**
 * @author shermann
 *
 */
public class MCRGbvHandleProvider implements MCRIHandleProvider {

    public static final String NAMING_AUTHORITY = "428894";

    public static final String URN_PREFIX = "urn:nbn:de:gbv:601-" + NAMING_AUTHORITY + "-";

    public static final String NAMING_AUTHORITY_SEGMENT = "vzg";

    private static final Logger LOGGER = Logger.getLogger(MCRGbvHandleProvider.class);

    /** done status */
    static final String STATUS_DONE = "done";

    /** typo is actually needed */
    static final String STATUS_CANCEL = "cancelled";

    private static HttpClient HTTP_CLIENT;

    static {
        MultiThreadedHttpConnectionManager connectionMgr = new MultiThreadedHttpConnectionManager();
        connectionMgr.getParams().setDefaultMaxConnectionsPerHost(10);
        connectionMgr.getParams().setMaxTotalConnections(10);
        HTTP_CLIENT = new HttpClient(connectionMgr);
    }

    @Override
    public MCRHandle requestHandle(MCRPath file) {
        String objectSignature = UUID.randomUUID().toString().replace("-", "");

        int status = -1;
        status = registerObject(objectSignature);
        if (!String.valueOf(status).startsWith("2")) {
            LOGGER.error("Received the non ok http status " + status);
            return null;
        }

        MCRHandle handle = new MCRHandle();
        handle.setMcrid(file.getOwner());
        handle.setPath(file.getOwnerRelativePath());
        handle.setObjectSignature(objectSignature);

        return handle;
    }

    /**
     * @param objectSignature
     * @return
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws HttpException
     */
    private int registerObject(String objectSignature) {
        /* post to gbv namespace */
        PostMethod post = new PostMethod(MCRHandleCommons.GBV_OBJECT_REPOS_URL + objectSignature);
        int status = -1;
        try {
            JsonObject gbvJSON = createGbvSimple(objectSignature);
            post.setRequestEntity(new StringRequestEntity(gbvJSON.toString(), "application/json", "UTF-8"));
            LOGGER.info("Sending request to " + post.getURI() + " (register object)");
            status = HTTP_CLIENT.executeMethod(post);

        } catch (IOException e) {
            LOGGER.error("Could not register object", e);
        } finally {
            post.releaseConnection();
        }
        return status;
    }

    @Override
    public MCRHandle generateHandle() {
        MCRHandle handle = new MCRHandle(NAMING_AUTHORITY, NAMING_AUTHORITY_SEGMENT, UUID.randomUUID().toString());
        return handle;
    }

    @Override
    public MCRHandle[] generateHandle(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0 but was " + amount);
        }

        MCRHandle[] handles = new MCRHandle[amount];
        for (int i = 0; i < amount; i++) {
            handles[i] = new MCRHandle(NAMING_AUTHORITY, NAMING_AUTHORITY_SEGMENT, UUID.randomUUID().toString());
        }
        return handles;
    }

    /**
     * @param uuid
     * @return
     */
    private JsonObject createGbvSimple(String signature) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("cmodel", "digicult:simple"); // TODO make a property
        /* gbv context, fixed */
        jsonObject.addProperty("context", MCRHandleCommons.GBV_CONTEXT_UUID);
        jsonObject.addProperty("ctime", MCRHandleCommons.DATE_FORMAT.format(new Date()));
        jsonObject.addProperty("mtime", MCRHandleCommons.DATE_FORMAT.format(new Date()));
        jsonObject.addProperty("owner", MCRHandleCommons.DEFAULT_OWNER);
        jsonObject.addProperty("signature", signature);

        return jsonObject;
    }
}
