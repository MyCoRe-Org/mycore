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

package org.mycore.frontend.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet handles form submissions from MyCoRe XML Editor pages and
 * converts the submitted data into a JDOM XML document for further processing.
 * It can also handle file uploads.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCREditorServlet extends MCRServlet implements MCRSessionListener {
    private static final long serialVersionUID = 1L;

    protected final static Logger logger = Logger.getLogger(MCREditorServlet.class);

    private final static String EDITOR_SESSIONS_KEY = "editorSessions";

    @Override
    public void init() throws ServletException {
        super.init();
        MCRSessionMgr.addSessionListener(this);
    }

    @Override
    public void destroy() {
        super.destroy();
        MCRSessionMgr.removeSessionListener(this);
    }

    /**
     * For each user session, the state of all editor forms most recently used
     * is kept in a cache. The number of editor form data that is kept is controlled
     * by the property MCR.EditorFramework.MaxEditorsInSession.
     */
    static MCRCache getEditorSessionCache() {
        
        MCRSession current = MCRSessionMgr.getCurrentSession();
        MCRCache editorsInSession = (MCRCache) (current.get(EDITOR_SESSIONS_KEY));
        if (editorsInSession == null) {
            int maxEditors = MCRConfiguration.instance().getInt("MCR.EditorFramework.MaxEditorsInSession",10);
            editorsInSession = new MCRCache(maxEditors, "Editor data in MCRSessions, MCRSession=" + current.getID());
            current.put(EDITOR_SESSIONS_KEY, editorsInSession);
        }
        return editorsInSession;
    }
    
    public void sessionEvent(MCRSessionEvent event) {
        if (event.getType() == MCRSessionEvent.Type.destroyed) {
            // close MCRCache instance cleanly
            MCRSession current = event.getSession();
            MCRCache editorsInSession = (MCRCache) current.get(EDITOR_SESSIONS_KEY);
            if (editorsInSession != null) {
                logger.info(EDITOR_SESSIONS_KEY + " is NOT null");
                editorsInSession.close();
            }
        }
    }

    public void doGetPost(MCRServletJob job) throws ServletException, java.io.IOException {
        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        MCRRequestParameters parms = new MCRRequestParameters(req);

        String action = parms.getParameter("_action");

        if ("show.popup".equals(action)) {
            processShowPopup(req, res);
        } else if ("submit".equals(action)) {
            processSubmit(req, res, parms);
        } else if ("end.subselect".equals(action)) {
            processEndSubSelect(req, res, parms);
        } else if ("include".equals(action)) {
            includeEditorFromXSL(req, res);
        } else {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void includeEditorFromXSL(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String sessionID = req.getParameter("XSL.editor.session.id");
        Element editorResolved = null;

        if (sessionID != null)
            editorResolved = (Element)(getEditorSessionCache().get(sessionID));

        if ((editorResolved == null) || (sessionID == null)) {
            Map parameters = req.getParameterMap();
            String ref = req.getParameter("_ref");
            String uri = req.getParameter("_uri");
            boolean validate = "true".equals(req.getParameter("_validate"));
            editorResolved = startSession(parameters, ref, uri, validate);
        }

        getLayoutService().sendXML(req, res, new Document((Element) (editorResolved.clone())));
    }

    /**
     * Shows a help popup window
     */
    private void processShowPopup(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
        String sessionID = req.getParameter("_session");
        String ref = req.getParameter("_ref");

        logger.debug("Editor session " + sessionID + " show popup " + ref);

        Element editor = (Element)(getEditorSessionCache().get(sessionID));
        Element popup = MCREditorDefReader.findElementByID(ref, editor);
        Element clone = (Element) (popup.clone());

        getLayoutService().doLayout(req, res, new Document(clone));
    }

    /**
     * Replaces editor elements in static webpage with complete editor
     * definition.
     * 
     * @param request
     *            the current MCRServletJob
     * @param uri
     *            the uri of the static XML file containing the editor
     * @param xml
     *            the complete XML document of the static webpage
     */
    public static void replaceEditorElements(HttpServletRequest request, String uri, Document xml) {
        String sessionID = request.getParameter("XSL.editor.session.id");

        List<Element> editors = new ArrayList<Element>();
        Iterator it = xml.getDescendants(new ElementFilter("editor"));
        while (it.hasNext())
            editors.add((Element)(it.next()));

        for (Element editor: editors ) {
            Element editorResolved = null;
            if( sessionID != null ) 
                editorResolved = (Element)(getEditorSessionCache().get(sessionID));
            
            if ((sessionID == null) || (editorResolved == null))
            {
                Map parameters = request.getParameterMap();
                boolean validate = "true".equals(editor.getAttributeValue("validate", "false"));
                String ref = editor.getAttributeValue("id");
                editorResolved = startSession(parameters, ref, uri, validate);
            }

            editor.removeContent();
            editor.addContent(editorResolved.cloneContent());
            editor.setAttribute("session", editorResolved.getAttributeValue("session"));
        }
    }

    /**
     * Starts a new editor session in webpage
     */
    private static Element startSession(Map parameters, String ref, String uri, boolean validate) {
        logger.debug("Editor start editor session from " + ref + "@" + uri);

        Element param = getTargetParameters(parameters);
        Element editor = MCREditorDefReader.readDef(uri, ref, validate);

        setDefault(editor, "cell", "row", "1");
        setDefault(editor, "cell", "col", "1");

        if (param != null) {
            editor.addContent(param);
        }

        String sourceURI = replaceParameters(editor, "source", "uri", parameters);

        if (sourceURI != null) {
            logger.info("Editor reading XML input from " + sourceURI);
            Element input = MCRURIResolver.instance().resolve(sourceURI);
            MCREditorSubmission sub = new MCREditorSubmission(input, editor);
            editor.addContent(sub.buildInputElements());
            editor.addContent(sub.buildRepeatElements());
        }
        else
            logger.debug("Editor is started empty without XML input");
        
        String cancelURL = replaceParameters(editor, "cancel", "url", parameters);
        logger.debug("Editor cancel url is " + cancelURL );
        editor.removeChildren("cancel");
        if( cancelURL != null)
            editor.addContent(new Element("cancel").setAttribute("url", cancelURL));

        String sessionID = buildSessionID();
        editor.setAttribute("session", sessionID);
        getEditorSessionCache().put(sessionID, editor);
        logger.debug("Storing editor under session id " + sessionID);

        return editor;
    }

    /**
     * Replaces parameter names with their values for a list of element
     * attributes. For example, in &lt;source
     * uri="request:servlets/SomeServlet?id={id}" /&gt;, replaces the {id} with
     * the actual value, if present.
     * 
     * @param parent
     *            parent Element, e.g. the editor element
     * @param elementName
     *            name of the child element(s), e.g. the "source" element
     * @param attributeName
     *            name of the attribute, e.g. the "uri" attribute
     * @param parameters
     *            the request parameter map
     * @return the value of the first attribute in the element list where all
     *         parameters could be replaced successfully
     */
    private static String replaceParameters(Element parent, String elementName, String attributeName, Map<String,String[]> parameters) {
        // Get all elements of a given name, e.g. "source"
        List<Element> l = parent.getChildren(elementName);
        for (Element e : l) {
            // Get attribute of that element, e.g. "uri"
            String value = e.getAttributeValue(attributeName);

            StringTokenizer st = new java.util.StringTokenizer(value, "{}", true);
            // The attribute value with all {...} replaced with actual request
            // param values
            StringBuffer replaced = new StringBuffer();
            boolean withinParameter = false;

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equals("{"))
                    withinParameter = true; // Begin of request parameter name
                else if (token.equals("}"))
                    withinParameter = false; // End of request parameter name
                else if (withinParameter) {
                    // If request parameter not given, skip complete value
                    if (!parameters.containsKey(token))
                        break;
                    else
                        // Replace parameter with value from request
                        replaced.append(parameters.get(token)[0]);
                } else
                    replaced.append(token);
            }
            // If all parameters have been replaced, return this value
            if (!withinParameter)
                return replaced.toString();
        }
        return null; // Fall back, no matches at all 
    }
    
    private static void setDefault(Element editor, String filter, String attrib, String value) {
        Iterator it = editor.getDescendants(new ElementFilter(filter));
        while (it.hasNext()) {
            Element e = (Element) (it.next());
            if (e.getAttribute(attrib) == null)
                e.setAttribute(attrib, value);
        }
    }

    private static Element getTargetParameters(Map parameters) {
        Element tps = new Element("target-parameters");
        Iterator keys = parameters.keySet().iterator();

        while (keys.hasNext()) {
            String key = (String) (keys.next());

            if (key.startsWith("XSL.target.param.")) {
                String expr = ((String[]) (parameters.get(key)))[0];

                if ((expr != null) && (expr.trim().length() > 0)) {
                    int pos = expr.indexOf("=");
                    String name = expr.substring(0, pos).trim();
                    String value = expr.substring(pos + 1).trim();
                    Element tp = new Element("target-parameter");
                    tp.setAttribute("name", name);
                    tp.addContent(value);
                    tps.addContent(tp);
                }

                continue;
            }

            if (key.startsWith("XSL.")) {
                continue;
            }

            String[] values = (String[]) (parameters.get(key));

            for (int i = 0; (values != null) && (i < values.length); i++) {
                logger.debug("Editor target parameter " + key + "=" + values[i]);

                Element tp = new Element("target-parameter");
                tp.setAttribute("name", key);
                tp.addContent(values[i]);
                tps.addContent(tp);
            }
        }

        return tps;
    }

    private static Random random = new Random();

    private static synchronized String buildSessionID() {
        StringBuffer sb = new StringBuffer();
        sb.append(Long.toString(System.currentTimeMillis(), 36));
        sb.append(Long.toString(random.nextLong(), 36));
        sb.reverse();

        return sb.toString();
    }

    private void processEndSubSelect(HttpServletRequest req, HttpServletResponse res, MCRRequestParameters parms) throws java.io.IOException {
        String root = "root";
        List variables = new ArrayList();
        Enumeration e = req.getParameterNames();

        while (e.hasMoreElements()) {
            String name = (String) (e.nextElement());
            String value = req.getParameter(name);

            if (!name.startsWith("_var_")) {
                continue;
            }

            if (name.length() > 5) {
                name = root + "/" + name.substring(5);
            } else {
                name = root;
            }

            variables.add(new MCREditorVariable(name, value));
        }
        
        // Remove subselect parameters from current session
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.deleteObject("XSL.subselect.session");
        mcrSession.deleteObject("XSL.subselect.varpath");
        mcrSession.deleteObject("XSL.subselect.webpage");

        sendToSubSelect(res, parms, variables, root);
    }

    private void processSubmit(HttpServletRequest req, HttpServletResponse res, MCRRequestParameters parms) throws ServletException, java.io.IOException {
        logger.debug("Editor: process submit");

        String sessionID = parms.getParameter("_session");
        Element editor = (Element)(getEditorSessionCache().get(sessionID));

        if (editor == null) {
            logger.error("No editor for session <" + sessionID + ">");
            throw new ServletException("invalid session");
        }

        String button = null;

        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) (e.nextElement());

            if (name.startsWith("_p-") || name.startsWith("_m-") || name.startsWith("_u-") || name.startsWith("_d-") || name.startsWith("_s-")) {
                button = name;

                break;
            }
        }

        if (button == null) {
            logger.info("Editor session " + sessionID + " submitting form data");

            // We do not remove the editor session from cache, because
            // the user may use the back button and re-submit if an error
            // occured. In nearly all editor states this is safe, but sometimes
            // may result in ugly behaviour in conjunction with repeaters.
            //
            // sessions.remove(sessionID); // no good idea
            processTargetSubmission(req, res, parms, editor);
        } else if (button.startsWith("_s-")) {
            StringTokenizer sst = new StringTokenizer(button.substring(3), "-");
            String id = sst.nextToken();
            String var = sst.nextToken();
            logger.debug("Editor start subselect " + id + " at position " + var);

            Element subselect = MCREditorDefReader.findElementByID(id, editor);
            StringBuffer sb = new StringBuffer(getBaseURL());

            String webpage = URLEncoder.encode(parms.getParameter("_webpage"),"UTF-8");
            if ("editor".equals(subselect.getAttributeValue("type"))) {
                sb.append(subselect.getAttributeValue("href"));
                sb.append("?subselect.session=").append(sessionID);
                sb.append("&subselect.varpath=").append(var);
                sb.append("&subselect.webpage=").append(webpage);
                sb.append("&XSL.editor.cancel.url=").append(getBaseURL());
                sb.append(parms.getParameter("_webpage"));
                sb.append("XSL.editor.session.id=").append(sessionID);
            } else if ("webpage".equals(subselect.getAttributeValue("type"))) {
                sb.append(subselect.getAttributeValue("href"));
                if (subselect.getAttributeValue("href").indexOf("?") > 0)
                    sb.append("&XSL.subselect.session.SESSION=").append(sessionID);
                else
                    sb.append("?XSL.subselect.session.SESSION=").append(sessionID);
                sb.append("&XSL.subselect.varpath.SESSION=").append(var);
                sb.append("&XSL.subselect.webpage.SESSION=").append(webpage);
            } else if ("servlet".equals(subselect.getAttributeValue("type"))) {
                sb.append(subselect.getAttributeValue("href"));
                sb.append("?subselect.session=").append(sessionID);
                sb.append("&subselect.varpath=").append(var);
                sb.append("&subselect.webpage=").append(webpage);
            }

            String url = sb.toString();

            editor.removeChild("input");
            editor.removeChild("repeats");

            MCREditorSubmission sub = new MCREditorSubmission(parms, editor, false);
            editor.addContent(sub.buildInputElements());
            editor.addContent(sub.buildRepeatElements());

            logger.debug("Editor goto subselect at " + url);
            res.sendRedirect(res.encodeRedirectURL(url));
        } else {
            int pos = button.lastIndexOf("-");

            String action = button.substring(1, 2);
            String path = button.substring(3, pos);
            int nr = Integer.parseInt(button.substring(pos + 1, button.length() - 2));

            logger.debug("Editor action " + action + " " + nr + " " + path);

            editor.removeChild("input");
            editor.removeChild("repeats");
            editor.removeChild("failed");

            MCREditorSubmission sub = new MCREditorSubmission(parms, editor, false);

            if ("p".equals(action)) {
                sub.doPlus(path, nr);
            } else if ("m".equals(action)) {
                sub.doMinus(path, nr);
            } else if ("u".equals(action)) {
                sub.doUp(path, nr);
            } else if ("d".equals(action)) {
                sub.doUp(path, nr + 1);
            }

            editor.addContent(sub.buildInputElements());
            editor.addContent(sub.buildRepeatElements());

            // Redirect to webpage to reload editor form
            StringBuffer sb = new StringBuffer(getBaseURL());
            sb.append(parms.getParameter("_webpage"));
            sb.append("XSL.editor.session.id=");
            sb.append(sessionID);

            logger.debug("Editor redirect to " + sb.toString());
            res.sendRedirect(res.encodeRedirectURL(sb.toString()));
        }
    }

    private void processTargetSubmission(HttpServletRequest req, HttpServletResponse res, MCRRequestParameters parms, Element editor) throws ServletException, java.io.IOException {
        logger.debug("Editor: processTargetSubmission ");

        editor.removeChild("failed");

        MCREditorSubmission sub = new MCREditorSubmission(parms, editor, true);

        if (sub.errors()) // validation failed, go back to editor form in
        // webpage
        {
            editor.removeChild("input");
            editor.removeChild("repeats");
            editor.addContent(sub.buildInputElements());
            editor.addContent(sub.buildRepeatElements());
            editor.addContent(sub.buildFailedConditions());

            String sessionID = parms.getParameter("_session");

            // Redirect to webpage to reload editor form
            StringBuffer sb = new StringBuffer(getBaseURL());
            sb.append(parms.getParameter("_webpage"));
            sb.append("XSL.editor.session.id=");
            sb.append(sessionID);
            logger.debug("Editor redirect to " + sb.toString());
            res.sendRedirect(res.encodeRedirectURL(sb.toString()));

            return;
        }

        String targetType = parms.getParameter("_target-type");
        logger.debug("Editor: targettype=" + targetType);

        if (targetType.equals("servlet")) {
            sendToServlet(req, res, sub);
        } else if (targetType.equals("url")) {
            sendToURL(req, res);
        } else if (targetType.equals("webapp")) {
          sendToWebAppFile(req, res, sub, editor);
        } else if (targetType.equals("debug")) {
            sendToDebug(res, sub);
        } else if (targetType.equals("display")) {
            getLayoutService().doLayout(req, res, sub.getXML());
        } else if (targetType.equals("subselect")) {
            List variables = sub.getVariables();
            String root = sub.getXML().getRootElement().getName();
            sendToSubSelect(res, parms, variables, root);
        } else {
            logger.debug("Unknown targettype");
        }

        logger.debug("Editor: processTargetSubmission DONE");
    }

    private void sendToServlet(HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub) throws IOException, ServletException {
        String name = sub.getParameters().getParameter("_target-name");
        String url = sub.getParameters().getParameter("_target-url");

        logger.debug("name=" + name + " url=" + url);

        RequestDispatcher rd = null;

        if ((name != null) && (name.trim().length() > 0)) {
            rd = getServletContext().getNamedDispatcher(name);
        } else if ((url != null) && (url.trim().length() > 0)) {
            rd = getServletContext().getRequestDispatcher(url);
        }

        logger.debug("rd=" + rd);

        if (rd != null) {
            req.setAttribute("MCREditorSubmission", sub);
            rd.forward(req, res);
        }
    }

    private void sendToURL(HttpServletRequest req, HttpServletResponse res) throws IOException {
        StringBuffer url = new StringBuffer(req.getParameter("_target-url"));
        url.append('?').append(req.getQueryString());
        res.sendRedirect(res.encodeRedirectURL(url.toString()));
    }

    /**
     * Writes the output of editor to a local file in the web application directory.
     * Usage: &lt;target type="webapp" name="{relative_path_to_file}" url="{redirect url after file is written}" /&gt;
     * If cancel url is given, user is redirected to that page, otherwise target url is used.
     * 
     * @throws IOException
     */
    private void sendToWebAppFile(HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub, Element editor) throws IOException {
      String path = sub.getParameters().getParameter("_target-name");

      logger.debug("Writing editor output to webapp file " + path);
      
      File f = new File(getServletContext().getRealPath(path));
      FileOutputStream fout = new FileOutputStream(f);
      XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding( "UTF-8"  ));
      outputter.output(sub.getXML(), fout);
      fout.close();
      
      String url = sub.getParameters().getParameter("_target-url");
      Element cancel = editor.getChild("cancel");
      if( ( cancel != null ) && ( cancel.getAttributeValue( "url" ) != null ) )
        url = cancel.getAttributeValue( "url" );
        
      logger.debug("EditorServlet redirecting to " + url);
      res.sendRedirect(res.encodeRedirectURL(url.toString()));
    }
 
    private void sendToSubSelect(HttpServletResponse res, MCRRequestParameters parms, List variables, String root) throws IOException {
        String webpage = parms.getParameter("subselect.webpage");
        String sessionID = parms.getParameter("subselect.session");

        Element editor = (Element)(getEditorSessionCache().get(sessionID));
        MCREditorSubmission subnew = new MCREditorSubmission(editor, variables, root, parms);

        editor.removeChild("input");
        editor.removeChild("repeats");
        editor.addContent(subnew.buildInputElements());
        editor.addContent(subnew.buildRepeatElements());

        // Redirect to webpage to reload editor form
        StringBuffer sb = new StringBuffer(getBaseURL());
        sb.append(webpage);
        sb.append("XSL.editor.session.id=");
        sb.append(sessionID);

        logger.debug("Editor redirect to " + sb.toString());
        res.sendRedirect(res.encodeRedirectURL(sb.toString()));
    }

    private void sendToDebug(HttpServletResponse res, MCREditorSubmission sub) throws IOException, UnsupportedEncodingException {
        res.setContentType("text/html; charset=UTF-8");

        PrintWriter pw = res.getWriter();

        pw.println("<html><body><p><pre>");

        for (int i = 0; i < sub.getVariables().size(); i++) {
            MCREditorVariable var = (MCREditorVariable) (sub.getVariables().get(i));
            pw.println(var.getPath() + " = " + var.getValue());

            FileItem file = var.getFile();

            if (file != null) {
                pw.println("      is uploaded file " + file.getContentType() + ", " + file.getSize() + " bytes");
            }
        }

        pw.println("</pre></p><p>");

        XMLOutputter outputter = new XMLOutputter();
        Format fmt = Format.getPrettyFormat();
        fmt.setLineSeparator("\n");
        fmt.setOmitDeclaration(true);
        outputter.setFormat(fmt);

        Element pre = new Element("pre");
        pre.addContent(outputter.outputString(sub.getXML()));
        outputter.output(pre, pw);

        pw.println("</p></body></html>");
        pw.close();
    }
}
