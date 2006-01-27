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

package org.mycore.services.fieldquery;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.mycore.frontend.editor.MCREditorSubmission;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.parsers.bool.MCRCondition;

/**
 * This servlet executes queries and presents result pages.
 * 
 * @author Harald Richter
 */
public class MCRSearchServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

    public void doGetPost(MCRServletJob job) throws IOException, ServletException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();

        MCREditorSubmission sub = (MCREditorSubmission) (request.getAttribute("MCREditorSubmission"));
        if (null != sub) {
            org.jdom.Document input = sub.getXML();
            if (LOGGER.isDebugEnabled()) {
                XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
                LOGGER.debug(out.outputString(input));
            }

            org.jdom.Element root = input.getRootElement();

            // Remove condition fields without values
            Iterator it = root.getDescendants(new ElementFilter("condition"));
            while (it.hasNext()) {
                Element condition = (Element) it.next();
                if (condition.getAttribute("value") == null) {
                    LOGGER.debug("Remove condition field without value : " + condition.getAttribute("field"));
                    condition.detach();
                }
            }

            String index = root.getAttributeValue("index");
            MCRSearcher ls = MCRSearcherFactory.getSearcher(index);

            MCRCondition cond = new MCRQueryParser().parse((Element) root.getChild("conditions").getChildren().get(0));
            int maxResults = Integer.parseInt(root.getAttributeValue("maxResults", "100"));
            MCRResults result = ls.search(cond, null, maxResults);

            // start Layout servlet
            request.setAttribute("MCRLayoutServlet.Input.JDOM", new Document(result.buildXML()));
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        }
    }
}
