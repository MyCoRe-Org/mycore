/**
 * WCMSGetStaticHTMLServlet.java
 *
 * @author: Michael Brendel, Andreas Trappe, Thomas Scheffler (yagee)
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class WCMSGetStaticHTMLServlet extends WCMSServlet {
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
    

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
  			MCRSession mcrSession= MCRSessionMgr.getCurrentSession();
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
                    //System.out.println("�bergebene Sprache: "+ lang + "    gefundene Sprache " + content.getAttributeValue("lang", ns));
                    contentList = content.getContent();
                    validXHTML = true;
                    if (content.getAttributeValue("lang", ns).equals(lang)) {
                        //System.out.println("drinn");
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
                Document doc=new Document();
                doc.setDocType(new DocType("html","-//W3C//DTD XHTML 1.0 Transitional//EN","http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"));
                Element root=new Element("html");
                Element meta1=new Element("meta").setAttribute("http-equiv","Pragma").setAttribute("content","no-cache");
                Element meta2=new Element("meta").setAttribute("http-equiv","Cache-Control").setAttribute("content","no-cache, must-revalidate");
                root.addContent(new Element("head").addContent(meta1).addContent(meta2)).addContent(new Element("body").addContent(contentOutput));
                doc.setRootElement(root);
                XMLOutputter xmlout= new XMLOutputter(Format.getRawFormat().setEncoding(OUTPUT_ENCODING));
                ServletOutputStream sos = response.getOutputStream();
                               
                response.setContentType( "text/html; charset="+OUTPUT_ENCODING );
                xmlout.output(doc,sos);
                sos.flush();
                sos.close();
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
    