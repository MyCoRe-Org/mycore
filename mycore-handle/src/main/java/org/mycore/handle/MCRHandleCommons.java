/**
 *
 */
package org.mycore.handle;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

/**
 * Class contains constants useful within the handle context.
 *
 * @author shermann
 *
 */
public class MCRHandleCommons {

    /**
     * The http client to send messages to the remote handle service.
     */
    public static final HttpClient HTTP_CLIENT;
    static {
        MultiThreadedHttpConnectionManager connectionMgr = new MultiThreadedHttpConnectionManager();
        connectionMgr.getParams().setDefaultMaxConnectionsPerHost(10);
        connectionMgr.getParams().setMaxTotalConnections(10);
        HTTP_CLIENT = new HttpClient(connectionMgr);
    }

    /**
     * The default contact e-mail.
     *
     * TODO make a property
     */
    static final String HANDLE_MAIL = "thulb_handle@listserv.uni-jena.de";

    /** the format timestamps must be in */
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ROOT);

    /**
     * The url to the gbv object repository.
     */
    static final String GBV_OBJECT_REPOS_URL = "https://ws.gbv.de/riak/digicult/";

    /**
     * The context uuid for the eda bucket.
     */
    static final String EDA_REPOS_URL = "https://ws.gbv.de/riak/eda/";

    /**
     * The context uuid for the gbv bucket.
     */
    static final String GBV_CONTEXT_UUID = "fa1fb1d38a5241a797bb343c8ac1ad98";

    /**
     * The context uuid for the eda message bucket.
     */
    static final String EDA_MESSAGE_CONTEXT_UUID = "4ddbb3f591ad4494a6fb62c0bd588531";

    /**
     * The default owner.
     *
     * TODO make a property
     */
    static final String DEFAULT_OWNER = "thulb_test";
}
