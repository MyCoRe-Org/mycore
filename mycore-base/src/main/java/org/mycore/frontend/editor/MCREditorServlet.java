/*
 * 
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
import java.io.FileNotFoundException;
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
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.frontend.editor.postprocessor.MCREditorPostProcessor;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This servlet handles form submissions from MyCoRe XML Editor pages and
 * converts the submitted data into a JDOM XML document for further processing.
 * It can also handle file uploads.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date: 2010-11-04 12:22:36 +0100 (Do, 04 Nov
 *          2010) $
 */
public class MCREditorServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private final static Logger LOGGER = Logger.getLogger(MCREditorServlet.class);

    public void doGetPost(MCRServletJob job) throws ServletException, java.io.IOException {
        MCRRequestParameters parms = new MCRRequestParameters(job.getRequest());
        String action = parms.getParameter("_action");

        if ("show.popup".equals(action)) {
            processShowPopup(job);
        } else if ("submit".equals(action)) {
            processSubmit(job, parms);
        } else if ("end.subselect".equals(action)) {
            processEndSubSelect(job, parms);
        } else if ("include".equals(action)) {
            includeEditorFromXSL(job);
        } else {
            sendBadRequest(job);
        }
    }

    private void sendBadRequest(MCRServletJob job) throws IOException {
        job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    private void includeEditorFromXSL(MCRServletJob job) throws IOException {

        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

        String sessionID = job.getRequest().getParameter("XSL.editor.session.id");
        Element editorResolved = null;

        if (sessionID != null) {
            editorResolved = MCREditorSessionCache.instance().getEditorSession(sessionID).getXML();
        }

        if (editorResolved == null || sessionID == null) {
            Map parameters = job.getRequest().getParameterMap();
            String ref = req.getParameter("_ref");
            String uri = req.getParameter("_uri");
            boolean validate = "true".equals(req.getParameter("_validate"));
            editorResolved = startSession(parameters, ref, uri, validate);
        }

        getLayoutService().sendXML(req, res, new MCRJDOMContent((Element) editorResolved.clone()));
    }

    /**
     * Shows a help popup window
     */
    private void processShowPopup(MCRServletJob job) throws java.io.IOException {
        String sessionID = job.getRequest().getParameter("_session");
        String ref = job.getRequest().getParameter("_ref");

        LOGGER.debug("Editor session " + sessionID + " show popup " + ref);

        Element editor = MCREditorSessionCache.instance().getEditorSession(sessionID).getXML();
        Element popup = MCREditorDefReader.findElementByID(ref, editor);
        Element clone = (Element) popup.clone();

        getLayoutService().doLayout(job.getRequest(), job.getResponse(), new MCRJDOMContent(clone));
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
        while (it.hasNext()) {
            editors.add((Element) it.next());
        }

        for (Element editor : editors) {
            Element editorResolved = null;
            if (sessionID != null) {
                MCREditorSession editorSession = MCREditorSessionCache.instance().getEditorSession(sessionID);
                if (editorSession == null) {
                    throw new MCRException("Editor session is invalid:" + sessionID);
                }
                editorResolved = editorSession.getXML();
            }

            if (sessionID == null || editorResolved == null) {
                Map parameters = request.getParameterMap();
                boolean validate = "true".equals(editor.getAttributeValue("validate", "false"));
                String ref = editor.getAttributeValue("id");
                editorResolved = startSession(parameters, ref, uri, validate);
            }

            String clazz1 = editor.getAttributeValue("class");
            String clazz2 = editorResolved.getAttributeValue("class");
            editor.removeContent();
            editor.addContent(editorResolved.cloneContent());
            editor.setAttribute("session", editorResolved.getAttributeValue("session"));
            editor.setAttribute("class", (clazz1 == null ? clazz2 : clazz1));
        }
    }

    /**
     * Starts a new editor session in webpage
     */
    private static Element startSession(Map parameters, String ref, String uri, boolean validate) {
        LOGGER.debug("Editor start editor session from " + ref + "@" + uri);

        MCRParameters mp = new MCRParameters(parameters);
        Element param = getTargetParameters(parameters);
        Element editor = new MCREditorDefReader(uri, ref, validate, mp).getEditor();

        MCREditorSession mcrEditor = new MCREditorSession(editor, mp);
        MCREditorSessionCache.instance().storeEditorSession(mcrEditor);

        if (param != null) {
            editor.addContent(param);
        }

        readEditorInput(editor, mcrEditor);

        return editor;
    }

    private static void readEditorInput(Element editor, MCREditorSession mcrEditor) {
        String sourceURI = mcrEditor.getSourceURI();
        LOGGER.info("Editor reading XML input from " + sourceURI);
        Element input = MCRURIResolver.instance().resolve(sourceURI);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(input));
        }
        MCREditorSubmission sub = new MCREditorSubmission(input, editor);
        MCREditorDefReader.fixConditionedVariables(editor);
        editor.addContent(sub.buildInputElements());
        editor.addContent(sub.buildRepeatElements());
    }

    private static Element getTargetParameters(Map parameters) {
        Element tps = new Element("target-parameters");

        for (Object o : parameters.keySet()) {
            String parameterName = (String) o;
            String[] parameterValues = (String[]) parameters.get(parameterName);

            if (parameterValues == null)
                continue;
            if (parameterValues.length == 0)
                continue;

            if (parameterName.startsWith("XSL.target.param.")) {
                for (String parameterValue : parameterValues) {
                    if (parameterValue.isEmpty())
                        continue;
                    String[] parts = parameterValue.split("=", 2);
                    String name = parts[0].trim();
                    String value = parts[1].trim();
                    tps.addContent(buildTargetParameter(name, value));
                }
            } else if (!parameterName.startsWith("XSL.")) {
                for (String parameterValue : parameterValues)
                    tps.addContent(buildTargetParameter(parameterName, parameterValue));
            }
        }

        return tps;
    }

    private static Element buildTargetParameter(String name, String value) {
        LOGGER.debug("Editor target parameter " + name + "=" + value);

        Element tp = new Element("target-parameter");
        tp.setAttribute("name", name);
        tp.addContent(value);
        return tp;
    }

    private void processEndSubSelect(MCRServletJob job, MCRRequestParameters parms) throws java.io.IOException {
        String root = "root";
        List variables = new ArrayList();
        Enumeration e = job.getRequest().getParameterNames();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = job.getRequest().getParameter(name);

            if (!name.startsWith("_var_")) {
                continue;
            }

            if (name.length() > 5) {
                name = root + "/" + name.substring(5);
                if (name.contains("[@")) {
                    name = name.replace("[@", MCREditorSubmission.ATTR_SEP);
                    name = name.replace("='", MCREditorSubmission.ATTR_SEP);
                    name = name.replace(MCREditorSubmission.BLANK, MCREditorSubmission.BLANK_ESCAPED);
                    name = name.replace("']", "");
                }
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

        sendToSubSelect(job.getResponse(), parms, variables, root);
    }

    private void processSubmit(MCRServletJob job, MCRRequestParameters parms) throws ServletException,
        java.io.IOException {
        LOGGER.debug("Editor: process submit");

        String sessionID = parms.getParameter("_session");
        MCREditorSession editorSession = MCREditorSessionCache.instance().getEditorSession(sessionID);
        if (editorSession == null) {
            LOGGER.error("No editor for session <" + sessionID + ">");
            throw new ServletException("invalid session");
        }

        Element editor = editorSession.getXML();
        String button = null;

        for (Enumeration e = parms.getParameterNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();

            if (name.startsWith("_p-") || name.startsWith("_m-") || name.startsWith("_u-") || name.startsWith("_d-")
                || name.startsWith("_s-")) {
                button = name;

                break;
            }
        }

        if (button == null) {
            LOGGER.info("Editor session " + sessionID + " submitting form data");

            // We do not remove the editor session from cache, because
            // the user may use the back button and re-submit if an error
            // occured. In nearly all editor states this is safe, but sometimes
            // may result in ugly behaviour in conjunction with repeaters.
            //
            // sessions.remove(sessionID); // no good idea
            processTargetSubmission(job, parms, editor);
        } else if (button.startsWith("_s-")) {
            StringTokenizer sst = new StringTokenizer(button.substring(3), "-");
            String id = sst.nextToken();
            String var = sst.nextToken();
            LOGGER.debug("Editor start subselect " + id + " at position " + var);

            Element subselect = MCREditorDefReader.findElementByID(id, editor);
            StringBuilder sb = new StringBuilder(getBaseURL());

            String webpage = URLEncoder.encode(parms.getParameter("_webpage"), "UTF-8");
            if ("editor".equals(subselect.getAttributeValue("type"))) {
                sb.append(subselect.getAttributeValue("href"));
                sb.append("?subselect.session=").append(sessionID);
                sb.append("&subselect.varpath=").append(var);
                sb.append("&subselect.webpage=").append(webpage);

                String wp = getBaseURL() + parms.getParameter("_webpage");
                if (!wp.contains("XSL.editor.session.id")) {
                    wp += "XSL.editor.session.id=" + sessionID;
                }
                String cancelURL = URLEncoder.encode(wp, "UTF-8");
                sb.append("&cancelURL=").append(cancelURL);
            } else if ("webpage".equals(subselect.getAttributeValue("type"))) {
                sb.append(subselect.getAttributeValue("href"));
                if (subselect.getAttributeValue("href").indexOf("?") > 0) {
                    sb.append("&XSL.subselect.session.SESSION=").append(sessionID);
                } else {
                    sb.append("?XSL.subselect.session.SESSION=").append(sessionID);
                }
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

            LOGGER.debug("Editor goto subselect at " + url);
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(url));
        } else {
            int pos = button.lastIndexOf("-");

            String action = button.substring(1, 2);
            String path = button.substring(3, pos);
            int nr = Integer.parseInt(button.substring(pos + 1, button.length() - 2));

            LOGGER.debug("Editor action " + action + " " + nr + " " + path);

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
            StringBuilder sb = new StringBuilder(getBaseURL());
            String wp = parms.getParameter("_webpage");
            sb.append(wp);
            if (!wp.contains("XSL.editor.session.id=")) {
                sb.append("XSL.editor.session.id=");
                sb.append(sessionID);
            }

            path = path.replace('/', '_').replace('@', '_').replace('[', '_').replace(']', '_');
            sb.append("#rep").append(path);

            LOGGER.debug("Editor redirect to " + sb.toString());
            job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(sb.toString()));
        }
    }

    private void processTargetSubmission(MCRServletJob job, MCRRequestParameters parms, Element editor)
        throws ServletException, java.io.IOException {
        LOGGER.debug("Editor: processTargetSubmission ");

        HttpServletRequest req = job.getRequest();
        HttpServletResponse res = job.getResponse();

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
            StringBuilder sb = new StringBuilder(getBaseURL());
            String wp = parms.getParameter("_webpage");
            sb.append(wp);
            if (!wp.contains("XSL.editor.session.id=")) {
                sb.append("XSL.editor.session.id=");
                sb.append(sessionID);
            }
            LOGGER.debug("Editor redirect to " + sb.toString());
            res.sendRedirect(res.encodeRedirectURL(sb.toString()));

            return;
        }

        Document xml = sub.getXML();

        postProcess(editor, sub);

        String targetType = parms.getParameter("_target-type");
        LOGGER.debug("Editor: targettype=" + targetType);

        if (targetType.equals("servlet")) {
            sendToServlet(req, res, sub);
        } else if (targetType.equals("url")) {
            sendToURL(req, res);
        } else if (targetType.equals("webapp")) {
            sendToWebAppFile(req, res, sub, editor);
        } else if (targetType.equals("debug")) {
            sendToDebug(res, xml, sub);
        } else if (targetType.equals("display")) {
            getLayoutService().doLayout(req, res, new MCRJDOMContent(sub.getXML()));
        } else if (targetType.equals("subselect")) {
            List variables = sub.getVariables();
            String root = sub.getXML().getRootElement().getName();
            sendToSubSelect(res, parms, variables, root);
        } else {
            LOGGER.debug("Unknown targettype");
        }

        LOGGER.debug("Editor: processTargetSubmission DONE");
    }

    private void postProcess(Element editor, MCREditorSubmission sub) {
        Element postProcessorConfiguration = editor.getChild("postprocessor");
        if (postProcessorConfiguration != null) {
            String clazz = postProcessorConfiguration.getAttributeValue("class");
            LOGGER.debug("Transforming editor submission with " + clazz);
            try {
                Object instance = Class.forName(clazz).newInstance();
                MCREditorPostProcessor postprocessor = (MCREditorPostProcessor) instance;
                postprocessor.init(postProcessorConfiguration);
                Document input = sub.getXML();
                input = postprocessor.process(input);
                sub.setXML(input);
            } catch (Exception ex) {
                String msg = "Exception when postprocessing input with " + clazz;
                throw new MCRException(msg, ex);
            }
        }
    }

    private void sendToServlet(HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub)
        throws IOException, ServletException {
        String name = sub.getParameters().getParameter("_target-name");
        String url = sub.getParameters().getParameter("_target-url");

        LOGGER.debug("name=" + name + " url=" + url);

        RequestDispatcher rd = null;

        if (name != null && name.trim().length() > 0) {
            rd = getServletContext().getNamedDispatcher(name);
        } else if (url != null && url.trim().length() > 0) {
            rd = getServletContext().getRequestDispatcher(url);
        }

        LOGGER.debug("rd=" + rd);

        if (rd != null) {
            req.setAttribute("MCREditorSubmission", sub);
            rd.forward(req, res);
        }
    }

    private void sendToURL(HttpServletRequest req, HttpServletResponse res) throws IOException {
        StringBuilder url = new StringBuilder(req.getParameter("_target-url"));
        url.append('?').append(req.getQueryString());
        res.sendRedirect(res.encodeRedirectURL(url.toString()));
    }

    /**
     * Writes the output of editor to a local file in the web application
     * directory. Usage: &lt;target type="webapp" name="{relative_path_to_file}"
     * url="{redirect url after file is written}" /&gt; If cancel url is given,
     * user is redirected to that page, otherwise target url is used.
     * 
     * @throws IOException
     */
    private void sendToWebAppFile(HttpServletRequest req, HttpServletResponse res, MCREditorSubmission sub,
        Element editor) throws IOException {
        String path = sub.getParameters().getParameter("_target-name");

        LOGGER.debug("Writing editor output to webapp file " + path);

        File f = getWebAppFile(path);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        FileOutputStream fout = new FileOutputStream(f);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"));
        outputter.output(sub.getXML(), fout);
        fout.close();

        String url = sub.getParameters().getParameter("_target-url");
        Element cancel = editor.getChild("cancel");
        if ((url == null || url.length() == 0) && cancel != null && cancel.getAttributeValue("url") != null) {
            url = cancel.getAttributeValue("url");
        }

        LOGGER.debug("EditorServlet redirecting to " + url);
        res.sendRedirect(res.encodeRedirectURL(url));
    }

    private File getWebAppFile(String path) throws FileNotFoundException {
        String realPath = getServletContext().getRealPath(path);
        if (realPath != null) {
            return new File(realPath);
        }
        //resource is resolvable but not mappable (inside JAR?) 
        String contextPath = getServletContext().getRealPath("/");
        if (contextPath == null) {
            //WAR is not unpacked
            throw new FileNotFoundException("Cannot map " + path + " to local file");
        }
        File baseDir = new File(contextPath);
        return new File(baseDir, path);
    }

    private void sendToSubSelect(HttpServletResponse res, MCRRequestParameters parms, List variables, String root)
        throws IOException {
        String webpage = parms.getParameter("subselect.webpage");
        String sessionID = parms.getParameter("subselect.session");

        Element editor = MCREditorSessionCache.instance().getEditorSession(sessionID).getXML();
        MCREditorSubmission subnew = new MCREditorSubmission(editor, variables, root, parms);

        editor.removeChild("input");
        editor.removeChild("repeats");
        editor.removeChild("failed");
        editor.addContent(subnew.buildInputElements());
        editor.addContent(subnew.buildRepeatElements());

        // Redirect to webpage to reload editor form
        StringBuilder sb = new StringBuilder(getBaseURL());
        sb.append(webpage);
        if (!webpage.contains("XSL.editor.session.id=")) {
            sb.append("XSL.editor.session.id=");
            sb.append(sessionID);
        }

        LOGGER.debug("Editor redirect to " + sb.toString());
        res.sendRedirect(res.encodeRedirectURL(sb.toString()));
    }

    private void sendToDebug(HttpServletResponse res, Document unprocessed, MCREditorSubmission sub)
        throws IOException, UnsupportedEncodingException {
        res.setContentType("text/html; charset=UTF-8");

        PrintWriter pw = res.getWriter();

        pw.println("<html><body><p><pre>");

        for (int i = 0; i < sub.getVariables().size(); i++) {
            MCREditorVariable var = (MCREditorVariable) sub.getVariables().get(i);
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
        pre.addContent(outputter.outputString(unprocessed));
        outputter.output(pre, pw);

        pre = new Element("pre");
        pre.addContent(outputter.outputString(sub.getXML()));
        outputter.output(pre, pw);

        pw.println("</p></body></html>");
        pw.close();
    }
}
