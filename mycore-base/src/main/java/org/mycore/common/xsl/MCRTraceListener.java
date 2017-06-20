/*
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

package org.mycore.common.xsl;

import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xalan.templates.ElemTemplate;
import org.apache.xalan.templates.ElemTemplateElement;
import org.apache.xalan.trace.GenerateEvent;
import org.apache.xalan.trace.SelectionEvent;
import org.apache.xalan.trace.TraceListener;
import org.apache.xalan.trace.TracerEvent;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Traces the execution of xsl stylesheet elements in debug mode. The trace
 * is written to the log, and in parallel as comment elements to the output
 * html.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRTraceListener implements TraceListener {

    private final static Logger LOGGER = LogManager.getLogger(MCRTraceListener.class);

    /**
     * Traces the execution of xsl stylesheet elements in debug mode. The trace
     * is written to the log, and in parallel as comment elements to the output
     * html.
     */
    public void trace(TracerEvent ev) {
        if (LOGGER.isDebugEnabled()) {
            ElemTemplateElement ete = ev.m_styleNode; // Current position in
            // stylesheet

            StringBuilder log = new StringBuilder();

            // Find the name of the stylesheet file currently processed
            try {
                StringTokenizer st = new StringTokenizer(ete.getBaseIdentifier(), "/\\");
                String stylesheet = null;
                while (st.hasMoreTokens()) {
                    stylesheet = st.nextToken();
                }
                if (stylesheet != null) {
                    log.append(" ").append(stylesheet);
                }
            } catch (Exception ignored) {
            }

            // Output current line number and column number
            log.append(" line " + ete.getLineNumber() + " col " + ete.getColumnNumber());

            // Find the name of the xsl:template currently processed
            try {
                ElemTemplate et = ev.m_processor.getCurrentTemplate();
                log.append(" in <xsl:template");
                if (et.getMatch() != null) {
                    log.append(" match=\"" + et.getMatch().getPatternString() + "\"");
                }
                if (et.getName() != null) {
                    log.append(" name=\"" + et.getName().getLocalName() + "\"");
                }
                if (et.getMode() != null) {
                    log.append(" mode=\"" + et.getMode().getLocalName() + "\"");
                }
                log.append(">");
            } catch (Exception ignored) {
            }

            // Output name of the xsl or html element currently processed
            log.append(" " + ete.getTagName());
            LOGGER.debug("Trace" + log.toString());

            // Output xpath of current xml source node in context
            StringBuilder path = new StringBuilder();
            Node node = ev.m_sourceNode;
            if (node != null) {
                path.append(node.getLocalName());
                while ((node = node.getParentNode()) != null) {
                    path.insert(0, node.getLocalName() + "/");
                }
            }
            if (path.length() > 0) {
                LOGGER.debug("Source " + path.toString());
            }
            try {
                if ("true".equals(ev.m_processor.getParameter("DEBUG"))) {
                    ev.m_processor.getResultTreeHandler().comment(log.toString() + " ");
                    if (path.length() > 0) {
                        ev.m_processor.getResultTreeHandler().comment(" source " + path.toString() + " ");
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * When a stylesheet generates characters, they will be logged in debug
     * mode.
     */
    public void generated(GenerateEvent ev) {
        if (LOGGER.isDebugEnabled() && ev.m_eventtype == 12) {
            LOGGER.debug("Output " + new String(ev.m_characters, ev.m_start, ev.m_length).trim());
        }
    }

    /**
     * When a stylesheet does a selection, like in &lt;xsl:value-of /&gt; or
     * similar elements, the selection element and xpath is logged in debug
     * mode.
     */
    public void selected(SelectionEvent ev) {
        if (LOGGER.isDebugEnabled()) {
            String log = "Selection <xsl:" + ev.m_styleNode.getTagName() + " " + ev.m_attributeName + "=\""
                + ev.m_xpath.getPatternString() + "\">";
            LOGGER.debug(log);
            try {
                if ("true".equals(ev.m_processor.getParameter("DEBUG"))) {
                    ev.m_processor.getResultTreeHandler().comment(" " + log + " ");
                }
            } catch (SAXException ignored) {
            }
        }
    }
}
