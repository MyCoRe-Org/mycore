/**
 * WCMSGetStaticHTMLServlet.java
 *
 * @author: Michael Brendel, Andreas Trappe
 * @contact: michael.brendel@uni-jena.de, andreas.trappe@uni-jena.de
 *
 * Copyright (C) 2003 University of Jena, Germany
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
 * along with this program, normally in the file sources/gpl.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package wcms;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;

public class WCMSGetStaticHTMLServlet extends HttpServlet {
    MCRConfiguration mcrConf = MCRConfiguration.instance();
    private Namespace ns = Namespace.XML_NAMESPACE; //xml Namespace for the language attribute lang
    File hrefFile = null;
    char fs = File.separatorChar;
    String href = null;
    String lang = null;
    String error = null;
    List contentList;
    List contentOutput;
    private String currentLang = null;     //
    private String defaultLang = null;     //
    boolean validXHTML = true;
    
    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        /* Validate if user has been authentificated */
        
            processRequest(request, response, request.getSession(false));
        
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        /* Validate if user has been authentificated */
        if ( request.getSession(false) != null ) {
            processRequest(request, response, request.getSession(false));
        }
        else {
            response.sendRedirect(mcrConf.getString("sessionError"));
        }
    }

    /** Destroys the servlet.
     */
    public void destroy() {

    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, HttpSession session)
    throws ServletException, IOException {
        href = request.getParameter("href");
        lang = request.getParameter("lang");
        contentOutput = new Vector();

        try {
            SAXBuilder sax = new SAXBuilder();
            Document ed = sax.build(getServletContext().getRealPath("")+fs+href);
            Element begin = ed.getRootElement();
            List contentElements = begin.getChildren("section");
            Iterator contentElementsIterator = contentElements.iterator();
            while (contentElementsIterator.hasNext()) {
                Element content = (Element)contentElementsIterator.next();
                if (content.getAttributeValue("lang", ns) != null) {
                    System.out.println("Übergebene Sprache: "+ lang + "    gefundene Sprache " + content.getAttributeValue("lang", ns));
                    contentList = content.getContent();
                    validXHTML = true;
                    if (content.getAttributeValue("lang", ns).equals(lang)) {
                        System.out.println("drinn");
                        for( int i = (contentList.size() - 1); i >= 0; i-- ) {
                            Object o = contentList.get(i);
                            //String u = null;
                            if (o instanceof Element) ((Element)o).detach();
                            if (o instanceof Text) ((Text)o).detach();
                            if (o instanceof Comment) ((Comment)o).detach();
                            contentOutput.add(o);
                        }
                        reverse(contentOutput);
                        //break;
                    }
                }
                contentList.clear();
            }
            if (contentOutput != null && validXHTML) {
                StringWriter sw = new StringWriter();
                               
                XMLOutputter contentOut = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                contentOut.output(contentOutput, sw);
                ServletOutputStream sos = response.getOutputStream();

                String completeOutput = new String();
                completeOutput = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html><head></head><body>"
                							+ sw.toString()+"</body></html>";
                
                sos.println(completeOutput);
                
                //System.out.println("WCMSGetStaticHTMLServlet-Output: "+sw.toString());
                System.out.println("WCMSGetStaticHTMLServlet-Output: "+completeOutput);
                
                sos.flush();
                sos.close();
                sw.flush();
                sw.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void reverse(List l) {
		ListIterator fwd = l.listIterator(),
					 rev = l.listIterator(l.size());
		for (int i=0, n=l.size()/2; i<n; i++) {
		    Object tmp = fwd.next();
		    fwd.set(rev.previous());
		    rev.set(tmp);
		}
	}
}
    