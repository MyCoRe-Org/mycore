/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import javax.servlet.ServletContext;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.frontend.xeditor.MCREditorSession;
import org.mycore.frontend.xeditor.tracker.MCRChangeTracker;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRDebugTarget implements MCREditorTarget {

    @Override
    public void handleSubmission(ServletContext context, MCRServletJob job, MCREditorSession session, String parameter)
        throws Exception {
        job.getResponse().setContentType("text/html; charset=UTF-8");
        PrintWriter out = job.getResponse().getWriter();
        out.println("<html><body>");

        Map<String, String[]> parameters = job.getRequest().getParameterMap();
        session.getSubmission().setSubmittedValues(parameters);

        Document result = session.getEditedXML().clone();
        MCRChangeTracker tracker = session.getChangeTracker().clone();

        List<Step> steps = new ArrayList<>();
        for (String label; (label = tracker.undoLastBreakpoint(result)) != null;)
            steps.add(0, new Step(label, result.clone()));

        result = session.getEditedXML().clone();
        result = MCRChangeTracker.removeChangeTracking(result);

        result = session.getXMLCleaner().clean(result);
        steps.add(new Step("After cleaning", result));

        result = session.getPostProcessor().process(result);
        steps.add(new Step("After postprocessing", result));

        for (int i = 0; i < steps.size(); i++) {
            if (i == steps.size() - 3)
                outputParameters(parameters, out);

            steps.get(i).output(out);
        }

        out.println("</body></html>");
        out.close();
    }

    private void outputParameters(Map<String, String[]> values, PrintWriter out) {
        out.println("<h3>Submitted parameters:</h3>");
        out.println("<p><pre>");

        List<String> names = new ArrayList<>(values.keySet());
        Collections.sort(names);

        for (String name : names)
            for (String value : values.get(name))
                out.println(name + " = " + value);

        out.println("</pre></p>");
    }

    class Step {

        private String label;

        private Document xml;

        public Step(String label, Document xml) {
            this.label = label;
            this.xml = xml;
        }

        private Format format = Format.getPrettyFormat().setLineSeparator("\n").setOmitDeclaration(true);

        public void output(PrintWriter out) throws IOException {
            out.println("<h3>" + label + ":</h3>");
            XMLOutputter outputter = new XMLOutputter(format);
            out.println("<p>");
            Element pre = new Element("pre");
            pre.addContent(outputter.outputString(xml));
            outputter.output(pre, out);
            out.println("</p>");
        }
    }
}
