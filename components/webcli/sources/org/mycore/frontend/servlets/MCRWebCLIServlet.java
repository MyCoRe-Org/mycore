package org.mycore.frontend.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.cli.MCRCommand;
import org.mycore.frontend.cli.MCRExternalCommandInterface;

/**
 * Handles request from AJAX GUI.
 * @author Thomas Scheffler (yagee)
 * @since 2.0
 */
public class MCRWebCLIServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static List<MCRCommand> knownCommands;

    private static JSONObject commandsJSON;

    private static final Logger LOGGER = Logger.getLogger(MCRWebCLIServlet.class);

    private static final String SESSION_KEY = "MCRWebCLI";

    @Override
    public void init() throws ServletException {
        super.init();
        commandsJSON = new JSONObject();
        commandsJSON.put("commands", new ArrayList<Object>());
        knownCommands = getCommands();
    }

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
        String user = session.getCurrentUserID();
        if (!MCRAccessManager.checkPermission("use-webcli")) {
            StringBuilder sb=new StringBuilder("Access denied: ");
            sb.append(user).append("\nIP: ").append(session.getCurrentIP());
            generateErrorResponse(job.getRequest(), job.getResponse(), HttpServletResponse.SC_FORBIDDEN, sb.toString());
            return;
        }
        try {
            String request = getProperty(job.getRequest(), "request");
            HttpSession hsession = job.getRequest().getSession();
            if (request != null) {
                if (request.equals("getStatus")) {
                    printJSONObject(new JSONObject().put("running", isRunning()), job.getResponse());
                    return;
                } else if (request.equals("getLogs")) {
                    printJSONObject(getCurrentSessionContainer(true, hsession).getLogs(), job.getResponse());
                    return;
                } else if (request.equals("getKnownCommands")) {
                    printJSONObject(commandsJSON, job.getResponse());
                    return;
                } else if (request.equals("getCommandQueue")) {
                    printJSONObject(new JSONObject().put("commandQueue", new LinkedList<String>(getCurrentSessionContainer(true, hsession).getCommandQueue())), job.getResponse());
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

    private static void printJSONObject(JSONObject json, HttpServletResponse response) throws IOException {
        LOGGER.debug("JSON STRING" + json.toString());
        response.setContentType("application/x-json");
        response.setCharacterEncoding(CONFIG.getString("MCR.Request.CharEncoding", "UTF-8"));
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
        synchronized (session) {
            sessionValue = session.get(SESSION_KEY);
            if (sessionValue == null) {
                if (!create)
                    return null;
                // create object
                sessionValue = new MCRWebCLIContainer(knownCommands, hsession);
                session.put(SESSION_KEY, sessionValue);
            }
        }
        return (MCRWebCLIContainer) sessionValue;
    }

    protected static List<MCRCommand> getCommands() {
        ArrayList<MCRCommand> knownCommands = new ArrayList<MCRCommand>();
        knownCommands.add(new MCRCommand("process {0}", "org.mycore.frontend.cli.MCRCommandLineInterface.readCommandsFile String",
                "Execute the commands listed in the text file {0}."));
        knownCommands.add(new MCRCommand("show command statistics", "org.mycore.frontend.cli.MCRCommandLineInterface.showCommandStatistics",
                "Show statistics on number of commands processed and execution time needed per command"));
        addJSONCommand("Basic commands", knownCommands);
        String classes = MCRConfiguration.instance().getString("MCR.CLI.Classes.Internal", "");
        for (StringTokenizer st = new StringTokenizer(classes, ","); st.hasMoreTokens();) {
            String classname = st.nextToken();
            LOGGER.debug("Will load commands from class " + classname);
            Object obj;
            try {
                obj = Class.forName(classname).newInstance();
            } catch (Exception e) {
                String msg = "Could not instantiate class " + classname;
                throw new org.mycore.common.MCRConfigurationException(msg, e);
            }
            ArrayList<MCRCommand> commands = ((MCRExternalCommandInterface) obj).getPossibleCommands();
            knownCommands.addAll(commands);
            addJSONCommand(obj.getClass().getSimpleName(), commands);
        }
        return knownCommands;
    }

    private static void addJSONCommand(String parent, Collection<MCRCommand> cmds) {
        List<String> commands = new ArrayList<String>(cmds.size());
        for (final MCRCommand cmd : cmds) {
            commands.add(cmd.showSyntax());
        }
        JSONObject item = new JSONObject();
        item.put("name", parent);
        item.put("commands", commands);
        commandsJSON.getJSONArray("commands").put(item);
    }

    private static void generateErrorResponse(HttpServletRequest request, HttpServletResponse response, int errorCode, String message) throws IOException {
        response.setStatus(errorCode);
        response.setContentType("text/plain");
        response.getWriter().println(message);
        response.getWriter().flush();
        response.getWriter().close();
    }

}
