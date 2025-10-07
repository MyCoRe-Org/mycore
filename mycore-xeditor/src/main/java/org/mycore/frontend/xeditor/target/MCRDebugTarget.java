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

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
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

    private List<Object> outputSteps = new ArrayList<>();

    private PrintWriter writer;

    private MCREditorSession session;

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

        job.getResponse().setContentType("text/html; charset=UTF-8");
        this.writer = job.getResponse().getWriter();
        this.session = session;

        handleSubmittedParameters(job);
        Document result = session.getEditedXML().clone();
        reportChangeTracking();
        result = doXMLCleanup(result);
        doPostProcessing(result);

        sendDebugOutput();
    }

    private void handleSubmittedParameters(MCRServletJob job)
        throws JaxenException, JDOMException {
        Map<String, String[]> parameters = job.getRequest().getParameterMap();

        addStepToOutput(new MCRBreakpoint("Submitted parameters"));
        addStepToOutput(parameters);

        session.getSubmission().setSubmittedValues(parameters);
    }

    private void doPostProcessing(Document result)
        throws IOException, JDOMException {
        result = session.getPostProcessor().process(result);
        addStepToOutput(new MCRBreakpoint("After postprocessing"));
        addStepToOutput(result.clone());
    }

    private Document doXMLCleanup(Document result) {
        result = session.getXMLCleaner().clean(result);
        addStepToOutput(new MCRBreakpoint("After cleaning"));
        addStepToOutput(result.clone());
        return result;
    }

    private void reportChangeTracking() {
        MCRChangeTracker tracker = session.getChangeTracker();
        while (tracker.getChangeCount() > 0) {
            MCRTrackedAction change = tracker.undoLastChange();
            if (change instanceof MCRBreakpoint) {
                addAsFirstStepToOutput(session.getEditedXML().clone());
            }
            addAsFirstStepToOutput(change);
        }
    }

    private void addStepToOutput(Object step) {
        outputSteps.add(step);
    }

    private void addAsFirstStepToOutput(Object step) {
        outputSteps.add(0, step);
    }

    private void sendDebugOutput() throws IOException {
        writer.println("<html><body>");

        for (Object step : outputSteps) {
            sendDebugOutput(step);
        }

        writer.println("</body></html>");
        writer.close();
    }

    private void sendDebugOutput(Object step) throws IOException {
        if (step instanceof MCRBreakpoint bp) {
            outputBreakpoint(bp);
        } else if (step instanceof MCRChange c) {
            outputChange(c);
        } else if (step instanceof Document doc) {
            outputXML(doc);
        } else if (step instanceof Map map) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> parameters = (Map<String, String[]>) map;
            outputParameters(parameters);
        }
    }

    private void outputBreakpoint(MCRBreakpoint bp) {
        writer.println("<h3>" + bp.getMessage() + ":</h3>");
    }

    private void outputChange(MCRChange c) {
        writer.println( c.getMessage() + "<br/>");
    }

    private void outputXML(Document doc) {
        Element pre = new Element("pre").setAttribute("lang", "xml");
        pre.setText(XML_OUTPUTTER.outputString(doc));
        writer.println("<p>" + XML_OUTPUTTER.outputString(pre) + "</p>");
    }

    private void outputParameters(Map<String, String[]> parameters) {
        List<String> names = new ArrayList<>(parameters.keySet());
        Collections.sort(names);

        for (String name : names) {
            for (String value : parameters.get(name)) {
                writer.println(name + " = " + value + "<br/>");
            }
        }
    }
}
