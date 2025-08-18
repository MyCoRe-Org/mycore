/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.xeditor.target;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.access.MCRMissingPrivilegeException;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRBreakpoint;
import org.mycore.frontend.xeditor.tracker.MCRChange;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;
import org.mycore.frontend.xeditor.tracker.MCRTrackedAction;

import jakarta.servlet.ServletContext;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDebugTarget implements MCREditorTarget {

    private static final Format XML_OUTPUT_FORMAT =
        Format.getPrettyFormat().setLineSeparator("\n").setOmitDeclaration(true);

    private static final XMLOutputter XML_OUTPUTTER = new XMLOutputter(XML_OUTPUT_FORMAT);
    
    private static final String USE_DEBUG_PERMISSION = "use-xeditor-debug";

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        if (!MCRAccessManager.checkPermission(USE_DEBUG_PERMISSION)) {
            throw new MCRMissingPrivilegeException("use xeditor debug target", USE_DEBUG_PERMISSION);
        }

        Map<String, String[]> parameters = job.getRequest().getParameterMap();
        session.getSubmission().setSubmittedValues(parameters);

        job.getResponse().setContentType("text/html; charset=UTF-8");
        PrintWriter out = job.getResponse().getWriter();
        out.println("<html><body>");

        Document result = session.getEditedXML().clone();
        MCRChangeTracker tracker = session.getChangeTracker().copy();
        tracker.setEditedXML(result);

        List<Object> steps = new ArrayList<>();
        while (tracker.getChangeCount() > 0) {
            MCRTrackedAction change = tracker.undoLastChange();
            if (change instanceof MCRBreakpoint) {
                steps.add(0, result.clone());
            }
            steps.add(0, change);
        }

        result = session.getEditedXML().clone();

        result = session.getXMLCleaner().clean(result);
        steps.add(new MCRBreakpoint("After cleaning"));
        steps.add(result.clone());

        result = session.getPostProcessor().process(result);
        steps.add(new MCRBreakpoint("After postprocessing"));
        steps.add(result.clone());

        for (int i = 0; i < steps.size(); i++) {
            if (i == steps.size() - 6) {
                outputParameters(parameters, out);
            }

            output(steps.get(i), out);
        }

        out.println("</body></html>");
        out.close();
    }

    private void outputParameters(Map<String, String[]> values, PrintWriter out) throws IOException {
        output(new MCRBreakpoint("Submitted parameters"), out);
        out.println("<ul>");

        List<String> names = new ArrayList<>(values.keySet());
        Collections.sort(names);

        for (String name : names) {
            for (String value : values.get(name)) {
                out.print("<li>");
                out.println(name + " = " + value);
                out.println("</li>");
            }
        }

        out.println("</ul>");
    }

    private void output(Object o, PrintWriter out) throws IOException {
        if (o instanceof MCRBreakpoint bp) {
            out.println("<h3>" + bp.getMessage() + ":</h3>");
        } else if (o instanceof MCRChange c) {
            out.println("<p>" + c.getMessage() + "</p>");
        } else if (o instanceof Document doc) {
            Element pre = new Element("pre").setAttribute("lang", "xml");
            pre.setText(XML_OUTPUTTER.outputString(doc));
            out.println("<p>" + XML_OUTPUTTER.outputString(pre) + "</p>");
        }
    }
}
