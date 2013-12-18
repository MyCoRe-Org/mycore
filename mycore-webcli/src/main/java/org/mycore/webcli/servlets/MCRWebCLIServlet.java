package org.mycore.webcli.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRJSONUtils;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.cli.MCRExternalCommandInterface;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

import com.google.gson.JsonObject;

/**
 * Handles request from AJAX GUI.
 * @author Thomas Scheffler (yagee)
 * @since 2.0
 */
public class MCRWebCLIServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(MCRWebCLIServlet.class);

    private static final String SESSION_KEY = "MCRWebCLI";

    /**
     * Provides the access for the ajax gui.
     * 
     * Two parameters are recognized: <code>request</code> and
     * <code>run</code>
     * <p>
     * While <code>run</code> will not return anything else but a HTTP return
     * code, it is used to submit a new command to the current
     * <code>MCRSession</code> command queue (see
     * {@link MCRWebCLIContainer#addCommand(String) addCommand} method).
     * </p>
     * <p>
     * On the other hand: <code>request</code> can be used to receive some
     * information from the command execution.
     * <ul>
     *  <li><code>getStatus</code> will return
     *      <p>
     *          {"running": return of {@link MCRWebCLIContainer#isRunning()}}
     *      </p>
     *  </li>
     *  <li><code>getStatus</code> will return {@link MCRWebCLIContainer#getLogs()}
     *  </li>
     *  <li><code>getKnownCommands</code> will return
     *      <p>
     *          {"commands":[<br/>
     *          &#160;&#160;&#160;&#160;simple class name of <code>MCR.CLI.Classes.Internal</code>: [commands return by {@link MCRExternalCommandInterface#getPossibleCommands()}]]<br/>
     *          ]}
     *      </p>
     *  </li>
     *  <li><code>getCommandQueue</code> will return
     *      <p>
     *          {"commandQueue":[return of {@link MCRWebCLIContainer#getCommandQueue()}]}
     *      </p>
     *  </li>
     * </ul>
     * The content type of the {@link HttpServletResponse#SC_OK OK} response is <code>application/x-json</code>.
     * </p>
     */
    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String user = session.getUserInformation().getUserID();
        if (!MCRAccessManager.checkPermission("use-webcli")) {
            StringBuilder sb = new StringBuilder("Access denied: ");
            sb.append(user).append("\nIP: ").append(session.getCurrentIP());
            generateErrorResponse(job.getRequest(), job.getResponse(), HttpServletResponse.SC_FORBIDDEN, sb.toString());
            return;
        }
        try {
            String request = getProperty(job.getRequest(), "request");
            HttpSession hsession = job.getRequest().getSession();
            if (request != null) {
                JsonObject jsonObject = new JsonObject();
                if (request.equals("getStatus")) {
                    jsonObject.addProperty("running", isRunning());
                    printJsonObject(jsonObject, job.getResponse());
                    return;
                } else if (request.equals("getLogs")) {
                    printJsonObject(getCurrentSessionContainer(true, hsession).getLogs(), job.getResponse());
                    return;
                } else if (request.equals("getKnownCommands")) {
                    printJsonObject(MCRWebCLIContainer.getKnownCommands(), job.getResponse());
                    return;
                } else if (request.equals("getCommandQueue")) {
                    jsonObject.add("commandQueue", MCRJSONUtils.getJsonArray(getCurrentSessionContainer(true, hsession).getCommandQueue()));
                    printJsonObject(jsonObject, job.getResponse());
                    return;
                }
            }
            String command = getProperty(job.getRequest(), "run");
            if (command != null) {
                MCRWebCLIContainer cliCont = getCurrentSessionContainer(true, hsession);
                cliCont.addCommand(command);
            }
        } catch (RuntimeException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.close();
            generateErrorResponse(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, sw.toString());
        }
    }

    private static void printJsonObject(JsonObject json, HttpServletResponse response) throws IOException {
        LOGGER.debug("JSON STRING" + json.toString());
        response.setContentType("application/x-json");
        response.setCharacterEncoding(MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8"));
        response.getWriter().print(json);
    }

    private static boolean isRunning() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object sessionValue = session.get(SESSION_KEY);
        return (sessionValue != null && ((MCRWebCLIContainer) sessionValue).isRunning());
    }

    private static MCRWebCLIContainer getCurrentSessionContainer(boolean create, HttpSession hsession) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object sessionValue;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (session) {
            sessionValue = session.get(SESSION_KEY);
            if (sessionValue == null) {
                if (!create)
                    return null;
                // create object
                sessionValue = new MCRWebCLIContainer(hsession);
                session.put(SESSION_KEY, sessionValue);
            }
        }
        return (MCRWebCLIContainer) sessionValue;
    }

    private static void generateErrorResponse(HttpServletRequest request, HttpServletResponse response, int errorCode, String message) throws IOException {
        response.setStatus(errorCode);
        response.setContentType("text/plain");
        response.getWriter().println(message);
        response.getWriter().flush();
        response.getWriter().close();
    }

}
