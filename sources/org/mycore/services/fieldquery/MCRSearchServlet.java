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


import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import java.io.IOException;
import java.util.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.filter.ElementFilter;
import org.mycore.common.MCRConfigurationException;
import org.mycore.frontend.editor.*;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.apache.log4j.Logger;


/**
 * This servlet executes queries and presents result
 * pages.
 *
 * @author Harald Richter
 */
public class MCRSearchServlet extends MCRServlet {
  private static final long serialVersionUID = 1L;
  private static Logger LOGGER = Logger.getLogger(MCRSearchServlet.class);

  /**
   * The initialization method for this servlet. This read the default
   * language from the configuration.
   */
  public void init() throws MCRConfigurationException, ServletException {
      super.init();
  }

  /**
   * This method handles HTTP GET/POST requests and resolves them to output.
   * 
   * @param job
   *            MCRServletJob containing request and response objects
   * @exception IOException
   *                for java I/O errors.
   * @exception ServletException
   *                for errors from the servlet engine.
   */
  public void doGetPost(MCRServletJob job) throws IOException, ServletException
  {
    HttpServletRequest request = job.getRequest();
    HttpServletResponse response = job.getResponse();

    MCREditorSubmission sub = (MCREditorSubmission) (request.getAttribute("MCREditorSubmission"));
    if (null != sub)
    {
      org.jdom.Document input = sub.getXML();
      XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
      LOGGER.debug(out.outputString(input));

      org.jdom.Element root = input.getRootElement();

      // Remove condition fields without values
      ElementFilter fi = new ElementFilter("condition");
      Iterator it = input.getDescendants(fi);
      Vector help = new Vector();
      int anz = 0;
      while (it.hasNext())
      {
        anz++;
        Element condition = (Element) it.next();
        if (condition.getAttribute("value") == null)
        {
          LOGGER.debug("Remove condition field without value : " + condition.getAttribute("field"));
          help.add(condition);
        }
      }

      //  no conditions found 
      if (anz == help.size())
      {
        String url = getBaseURL() + "editor_form_search-simpledocument.xml";
        //TODO        request.sendRedirect( url );
        return;
      }

      //  delete conditions without values 
      for (int i = help.size() - 1; i >= 0; i--)
      {
        Element condition = (Element) (help.get(i));
        if (condition.getAttribute("value") == null)
          condition.detach();
      }

      String index = root.getAttributeValue("index");
      MCRSearcher ls = MCRSearcherFactory.getSearcher(index);

      MCRCondition cond = new MCRQueryParser().parse((Element) root.getChild("conditions")
          .getChildren().get(0));
      int maxResults = Integer.parseInt(root.getAttributeValue("maxResults", "100"));
      MCRResults result = ls.search(cond, null, maxResults);

      Element resXML = result.buildXML();
      // start Layout servlet
      request.setAttribute("MCRLayoutServlet.Input.JDOM", new Document( resXML ) );

      RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
      rd.forward(request, response);
    }
  }
    
}

