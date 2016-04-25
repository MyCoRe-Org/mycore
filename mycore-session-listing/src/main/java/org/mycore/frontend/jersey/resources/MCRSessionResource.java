package org.mycore.frontend.jersey.resources;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.jersey.MCRJerseyUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Resource which provides information about mycore sessions. 
 * 
 * @author Matthias Eichner
 */
@Path("session")
public class MCRSessionResource {

    /**
     * Lists all {@link MCRSession}'s in json format.
     * 
     * @param resolveHostname (false) if the host names are resolved. Resolving host names takes some
     *          time, so this is deactivated by default
     * @return list of sessions
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list")
    public Response list(@DefaultValue("false") @QueryParam("resolveHostname") Boolean resolveHostname) {
        // check permissions
        MCRJerseyUtil.checkPermission("manage-sessions");

        // get all sessions
        Collection<MCRSession> sessions = new ArrayList<MCRSession>(MCRSessionMgr.getAllSessions().values());

        // generate json
        JsonArray rootJSON = new JsonArray();
        for (MCRSession session : sessions) {
            JsonObject sessionJSON = generateSessionJSON(session, resolveHostname);
            rootJSON.add(sessionJSON);
        }
        return Response.status(Status.OK).entity(rootJSON.toString()).build();
    }

    /**
     * Builds the session JSON object.
     * 
     * @param session the session to represent in JSON format
     * @param resolveHostname if host names should be resolved, adds the property 'hostName'
     * @return a gson JsonObject containing all session information
     */
    private JsonObject generateSessionJSON(MCRSession session, boolean resolveHostname) {
        JsonObject sessionJSON = new JsonObject();

        String userID = session.getUserInformation().getUserID();
        String ip = session.getCurrentIP();

        sessionJSON.addProperty("id", session.getID());
        sessionJSON.addProperty("login", userID);
        sessionJSON.addProperty("ip", ip);
        if (resolveHostname) {
            String hostname = resolveHostName(ip);
            if (hostname != null) {
                sessionJSON.addProperty("hostname", hostname);
            }
        }
        String userRealName = session.getUserInformation().getUserAttribute(MCRUserInformation.ATT_REAL_NAME);
        if (userRealName != null) {
            sessionJSON.addProperty("realName", userRealName);
        }
        sessionJSON.addProperty("createTime", session.getCreateTime());
        sessionJSON.addProperty("lastAccessTime", session.getLastAccessedTime());
        sessionJSON.addProperty("loginTime", session.getLoginTime());
        sessionJSON.add("constructingStacktrace", buildStacktrace(session));
        return sessionJSON;
    }

    /**
     * Resolves the host name by the given ip.
     * 
     * @param ip the ip to resolve
     * @return the host name or null if the ip couldn't be resolved
     */
    private String resolveHostName(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getHostName();
        } catch (UnknownHostException unknownHostException) {
            return null;
        }
    }

    /**
     * Builds the constructing stack trace for the given session.
     * Containing the class, file, method and line.
     * 
     * @param session session
     * @return json array containing all {@link StackTraceElement} as json
     */
    private JsonElement buildStacktrace(MCRSession session) {
        JsonObject containerJSON = new JsonObject();
        StringBuffer hashBuffer = new StringBuffer();
        JsonArray stacktraceJSON = new JsonArray();
        for (StackTraceElement stackTraceElement : session.getConstructingStackTrace()) {
            // build json
            JsonObject lineJSON = new JsonObject();
            String className = stackTraceElement.getClassName();
            int lineNumber = stackTraceElement.getLineNumber();
            lineJSON.addProperty("class", className);
            lineJSON.addProperty("file", stackTraceElement.getFileName());
            lineJSON.addProperty("method", stackTraceElement.getMethodName());
            lineJSON.addProperty("line", lineNumber);
            stacktraceJSON.add(lineJSON);
            // hash
            hashBuffer.append(className).append(lineNumber);
        }
        containerJSON.addProperty("color", hashToColor(hashBuffer.toString().hashCode()));
        containerJSON.add("stacktrace", stacktraceJSON);
        return containerJSON;
    }

    /**
     * Converts an hash code to a hex color code (e.g. #315a4f).
     * 
     * @param hashCode the hash code to convert
     * @return a hex color code as string
     */
    private String hashToColor(int hashCode) {
        int r = (hashCode & 0xFF0000) >> 16;
        int g = (hashCode & 0x00FF00) >> 8;
        int b = hashCode & 0x0000FF;
        String hexR = Integer.toHexString(r);
        String hexG = Integer.toHexString(g);
        String hexB = Integer.toHexString(b);
        hexR = ("00" + hexR).substring(hexR.length());
        hexG = ("00" + hexG).substring(hexG.length());
        hexB = ("00" + hexB).substring(hexB.length());
        return new StringBuilder("#").append(hexR).append(hexG)
            .append(hexB).toString();
    }

}
