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

package wcms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class WCMSAdminServlet extends WCMSServlet {
	private boolean back;
	
	private String backaddr = null;
	
	private HttpServletRequest request;

    private HttpServletResponse response;
    
     /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String action = request.getParameter("action");
        List rootNodes = (List) mcrSession.get("rootNodes");
        File[] contentTemplates = new File((CONFIG.getString("MCR.WCMS.templatePath") + "content/").replace('/', File.separatorChar)).listFiles();
        Element rootOut = new Element("cms");
        Document jdom = new Document(rootOut);
        rootOut.addContent(new Element("session").setText(action));
        rootOut.addContent(new Element("userID").setText(mcrSession.get("userID").toString()));
        rootOut.addContent(new Element("userClass").setText(mcrSession.get("userClass").toString()));
        
        setReq(request);
        setResp(response);
        setback();
        setbackaddr();

        if (action!=null) {
        	if (action.equals("choose")) {
        		request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
                generateXML_managPage(mcrSession, rootOut, rootNodes, contentTemplates);
    	        } else if (action.equals("logs")) {
    	        	request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
    	            generateXML_logs(request, rootOut);
    		        } else if (action.equals("managGlobal") && mcrSession.get("userClass").equals("admin")) {
    		        	request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
    		            generateXML_managGlobal(rootOut);
    			        } else if (action.equals("saveGlobal") && mcrSession.get("userClass").equals("admin")) {
    			        	request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
    			            generateXML_saveGlobal(request, response);
    			        }
    				        else if (action.equals("view") 
    				        		&& (request.getParameter("file")!=null && !request.getParameter("file").equals(""))) {
    				        	
    				        	// live content version requested
    				            if (request.getParameter("file").toString().subSequence(0,4).equals("http")) {
    				            	String url = request.getParameter("file");
    				            	url = url + "?XSL.href=" + request.getParameter("XSL.href");
    				            	// archived navi version requested
    				            	if (request.getParameter("XSL.navi")!=null && !request.getParameter("XSL.navi").equals("")
    				            			&& !request.getParameter("XSL.navi").toString().subSequence(0,4).equals("http")) {
    				            		url = url + "&XSL.navi=" + request.getParameter("XSL.navi");
    				            		response.sendRedirect(response.encodeRedirectURL(url));
									}
    				            	else {
    				            		response.sendRedirect(response.encodeRedirectURL(url));
    				            	}
    				            } 
    				            // archived content version requested
								else {
									request.setAttribute("MCRLayoutServlet.Input.FILE", new File(request.getParameter("file")));
								}
    						}	
		}
        
        if(getback())
        {
         response.sendRedirect(response.encodeRedirectURL(getbackaddr()));
        }	
      else
        {	
        //request.setAttribute("XSL.Style", "xml");
          RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
          rd.forward(request, response);
        }
        
    }

    public void generateXML_managPage(MCRSession mcrSession, Element rootOut, List rootNodes, File[] contentTemplates) {
        rootOut.addContent(new Element("userRealName").setText(mcrSession.get("userRealName").toString()));
        rootOut.addContent(new Element("userClass").setText(mcrSession.get("userClass").toString()));
        rootOut.addContent(new Element("error").setText(""));

        Iterator rootNodesIterator = rootNodes.iterator();

        while (rootNodesIterator.hasNext()) {
            Element rootNode = (Element) rootNodesIterator.next();
            rootOut.addContent(new Element("rootNode").setAttribute("href", rootNode.getAttributeValue("href")).setText(rootNode.getTextTrim()));
        }

        Element templates = new Element("templates");
        Element contentTemp = new Element("content");

        for (int i = 0; i < contentTemplates.length; i++) {
            if (!contentTemplates[i].isDirectory()) {
                contentTemp.addContent(new Element("template").setText(contentTemplates[i].getName()));
            }
        }

        templates.addContent(contentTemp);
        rootOut.addContent(templates);
    }

    public void generateXML_logs(HttpServletRequest request, Element rootOut) {
        String sort = request.getParameter("sort");
        String sortOrder = request.getParameter("sortOrder");
        String error;

        try {
            File logFile = new File(CONFIG.getString("MCR.WCMS.logFile").replace('/', File.separatorChar));

            if (!logFile.exists()) {
                error = "Logfile nicht gefunden!";
            }

            Element root = new SAXBuilder().build(logFile).getRootElement();
            Element test = (Element) root.clone();
            rootOut.addContent(test);
        } catch (Exception e) {
            error = e.getMessage();

            System.out.println(error);
        }

        rootOut.addContent(new Element("sort").setAttribute("order", sortOrder).setText(sort));
    }

    public void generateXML_managGlobal(Element rootOut) {
        // generate template list
        rootOut.addContent(getTemplates());
    }

    public void generateXML_saveGlobal(HttpServletRequest request, HttpServletResponse response) {
        try {
            String pathToNavi = new String(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
            Document naviBase = new Document();
            naviBase = XMLFile2JDOM(pathToNavi);

            Element NaviBaseRoot = naviBase.getRootElement();

            // save default template if changed
            // get default template from navigatioBase
            String defaultTemplateNaviBase = XPath.newInstance("/navigation/@template").valueOf(naviBase);

            // get set def. templ. by aif
            String defaultTemplateAIF = new String();

            if ((request.getParameter("defTempl") != null) && !(request.getParameter("defTempl").equals(""))) {
                defaultTemplateAIF = request.getParameter("defTempl");
            }

            if (!(defaultTemplateNaviBase.equals(defaultTemplateAIF))) {
                // save changed naviBase
                NaviBaseRoot.setAttribute("template", defaultTemplateAIF);

                File navigationBase = new File(CONFIG.getString("MCR.WCMS.navigationFile").replace('/', File.separatorChar));
                XMLOutputter xmlOut = new XMLOutputter(Format.getRawFormat().setTextMode(Format.TextMode.PRESERVE).setEncoding("UTF-8"));
                xmlOut.output(naviBase, new FileOutputStream(navigationBase));
            }

            // forward to strarting page
            String address = new String();
            StringBuffer buffer = request.getRequestURL();
            String queryString = request.getQueryString();

            if (queryString != null) {
                buffer.append("?").append(queryString);
            }

            address = buffer.toString();

            String contextPath = request.getContextPath() + "/";
            int pos = address.indexOf(contextPath, 9);
            address = address.substring(0, pos) + contextPath + "servlets/WCMSLoginServlet";
            response.sendRedirect(response.encodeRedirectURL(response.encodeURL(address)));
        } catch (JDOMException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setReq(HttpServletRequest req) {
        this.request = req;
    }

    public HttpServletRequest getReq() {
        return request;
    }

    public void setResp(HttpServletResponse resp) {
        this.response = resp;
    }

    public HttpServletResponse getResp() {
        return response;
    }
    
    public boolean getback() {
		return this.back;
	}

	public void setback() {
		if(request.getParameter("back")!=null &&
				request.getParameter("back").equals("true")	) {
			this.back=true;
		}
		else
		{	
			this.back = false;
		}	
	}
	public String getbackaddr() {
		return this.backaddr;
	}

	public void setbackaddr() {
		this.backaddr=request.getParameter("address");
	}
}
