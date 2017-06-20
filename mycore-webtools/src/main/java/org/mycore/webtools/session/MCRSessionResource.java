package org.mycore.webtools.session;

import java.awt.Color;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
        JsonArray rootJSON = MCRSessionMgr
            .getAllSessions()
            .values()
            .stream()
            .collect(Collectors.toCollection(ArrayList::new)) //copy to work on
            .parallelStream()
            .map(s -> generateSessionJSON(s, resolveHostname))
            .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);
        return Response.status(Status.OK).entity(rootJSON.toString()).build();
    }

    /**
     * Kills the session with the specified session identifier.
     *
     * @return 200 OK if the session could be killed
     */
    @POST
    @Path("kill/{id}")
    public Response kill(@PathParam("id") String sessionID) {
        // check permissions
        MCRJerseyUtil.checkPermission("manage-sessions");

        // kill
        MCRSession session = MCRSessionMgr.getSession(sessionID);
        session.close();
        return Response.status(Status.OK).build();
    }

    /**
     * Builds the session JSON object.
     * 
     * @param session the session to represent in JSON format
     * @param resolveHostname if host names should be resolved, adds the property 'hostName'
     * @return a gson JsonObject containing all session information
     */
    private static JsonObject generateSessionJSON(MCRSession session, boolean resolveHostname) {
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
        session.getFirstURI()
            .ifPresent(u -> sessionJSON.addProperty("firstURI", u.toString()));
        sessionJSON.add("constructingStacktrace", buildStacktrace(session));
        return sessionJSON;
    }

    /**
     * Resolves the host name by the given ip.
     * 
     * @param ip the ip to resolve
     * @return the host name or null if the ip couldn't be resolved
     */
    private static String resolveHostName(String ip) {
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
    private static JsonElement buildStacktrace(MCRSession session) {
        JsonObject containerJSON = new JsonObject();
        JsonArray stacktraceJSON = new JsonArray();
        StackTraceElement[] constructingStackTrace = session.getConstructingStackTrace();
        for (StackTraceElement stackTraceElement : constructingStackTrace) {
            // build json
            JsonObject lineJSON = new JsonObject();
            lineJSON.addProperty("class", stackTraceElement.getClassName());
            lineJSON.addProperty("file", stackTraceElement.getFileName());
            lineJSON.addProperty("method", stackTraceElement.getMethodName());
            lineJSON.addProperty("line", stackTraceElement.getLineNumber());
            stacktraceJSON.add(lineJSON);
        }
        containerJSON.addProperty("color", hashToColor(Arrays.hashCode(constructingStackTrace)));
        containerJSON.add("stacktrace", stacktraceJSON);
        return containerJSON;
    }

    /**
     * Converts an hash code to a hex color code (e.g. #315a4f).
     * 
     * @param hashCode the hash code to convert
     * @return a hex color code as string
     */
    private static String hashToColor(int hashCode) {
        Color c = new Color(hashCode);
        return String.format(Locale.ROOT, "#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

}
