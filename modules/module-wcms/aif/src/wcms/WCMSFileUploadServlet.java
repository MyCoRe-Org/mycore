/**
 * WCMSLoginServlet.java
 *
 * @author: Michael Brendel, Andreas Trappe
 * @contact: michael.brendel@uni-jena.de, andreas.trappe@uni-jena.de
 * @version: 0.81
 * @last update: 11/25/2003
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
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;

public class WCMSFileUploadServlet extends HttpServlet {
    MCRConfiguration mcrConf = MCRConfiguration.instance();
    char fs = File.separatorChar;
    private static Set props=null;
    private String fileName, filePath, contentType, fileContentType, error, status, action = null;
    private String documentPath = mcrConf.getString("documentPath").replace('/', File.separatorChar);
    private String imagePath = mcrConf.getString("imagePath").replace('/', File.separatorChar);
    private String savePath = null;
    private Dictionary fields;
    private int fileSize, fileMaxSize;

    public String getFilename() {
        return fileName;
    }

    public String getFilepath() {
        return filePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFieldValue(String fieldName) {
        if (fields == null || fieldName == null)
            return null;
        return (String) fields.get(fieldName);
    }

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

    }

    /**
     * Handles the HTTP GET Method.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        /* Validate if user has been authentificated */
        if ( request.getSession(false) != null ) {
            doGetPost(request, response, request.getSession(false));
        }
        else {
            response.sendRedirect(mcrConf.getString("sessionError"));
        }
    }

    /**
     * Handles the HTTP POST Method.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

        /* Validate if user has been authentificated */
        if ( request.getSession(false) != null ) {
            doGetPost(request, response, request.getSession(false));
        }
        else {
            response.sendRedirect(mcrConf.getString("sessionError"));
        }
    }

    /**
     * Main program called by doGet and doPost.
     */
    protected void doGetPost(HttpServletRequest request, HttpServletResponse response, HttpSession session)
    throws ServletException, IOException {
        action = request.getParameter("action");
        if (action.equals("upload")) {
            fileMaxSize = mcrConf.getInt("maxUploadFileSize");
            fileSize = request.getContentLength();
            fileContentType = request.getContentType();

            if (fileSize == 0){
                status = "failed";
                error = "0";
                throw new ServletException("No legal request object. Please select a valid File for uploading.");
            }

            if (fileSize > fileMaxSize){
                status = "failed";
                error = "2";
                //aborting POST request
                throw new IOException("Upload File too large");
            }
            try {
                ServletInputStream in = request.getInputStream();
                byte[] line = new byte[1024];
                int i = in.readLine(line, 0, 1024);
                if (i<3) return;
                int boundaryLength = i-2;
                String boundary = new String(line, 0, boundaryLength);
                while (i != -1) {
                    String newLine = new String(line, 0, i);
                    if (newLine.startsWith("Content-Disposition: form-data; name=\"")) {
                        if (newLine.indexOf("filename=\"") != -1) {
                            setFilename(new String(line, 0, i-2));
                            if (fileName==null)
                            return;
                            //this is the file content
                            i = in.readLine(line, 0, 1024);
                            setContentType(new String(line, 0, i-2));
                            i = in.readLine(line, 0, 1024);
                            // blank line
                            i = in.readLine(line, 0, 1024);
                            newLine = new String(line, 0, i);
                            if (contentType.startsWith("image/")) savePath = imagePath;
                            else savePath = documentPath;
                            System.out.println(savePath+fileName);
                            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(savePath+fileName)));
                            while (i != -1 && !newLine.startsWith(boundary)) {
                                // the problem is the last line of the file content
                                // contains the new line character.
                                // So, we need to check if the current line is
                                // the last line.
                                i = in.readLine(line, 0, 1024);
                                if ((i==boundaryLength+2 || i==boundaryLength+4) // + 4 is eof
                                && (new String(line, 0, i).startsWith(boundary)))
                                    pw.print(newLine.substring(0, newLine.length()-2));
                                else pw.print(newLine);
                                newLine = new String(line, 0, i);
                            }
                            pw.close();
                        }
                        else {
                            //this is a field
                            // get the field name
                            int pos = newLine.indexOf("name=\"");
                            String fieldName = newLine.substring(pos+6, newLine.length()-3);
                            //System.out.println("fieldName:" + fieldName);
                            // blank line
                            i = in.readLine(line, 0, 1024);
                            i = in.readLine(line, 0, 1024);
                            newLine = new String(line, 0, i);
                            StringBuffer fieldValue = new StringBuffer(1024);
                            while (i != -1 && !newLine.startsWith(boundary)) {
                                // The last line of the field
                                // contains the new line character.
                                // So, we need to check if the current line is
                                // the last line.
                                i = in.readLine(line, 0, 1024);
                                if ((i==boundaryLength+2 || i==boundaryLength+4) // + 4 is eof
                                && (new String(line, 0, i).startsWith(boundary)))
                                    fieldValue.append(newLine.substring(0, newLine.length()-2));
                                else fieldValue.append(newLine);
                                newLine = new String(line, 0, i);
                            }
                            //System.out.println("fieldValue:" + fieldValue.toString());
                            fields.put(fieldName, fieldValue.toString());
                        }
                    }
                    i = in.readLine(line, 0, 1024);
                } // end while
            	status = "done";
             } //end try

             catch (IOException e){
				 e.printStackTrace();
                 System.out.println(e.getMessage());
             }
            /*Logfile
            WCMSActionServlet wcms = new WCMSActionServlet();
            wcms.writeToLogFile("File upload", fileName);
            */
            //JDOM Output
            Element rootOut = new Element("cms");
            Document jdom = new Document(rootOut);
            rootOut.addContent(new Element("session").setText("fileUpload"));
            rootOut.addContent(new Element("userID").setText((String)session.getAttribute("userID")));
            rootOut.addContent(new Element("userClass").setText((String)session.getAttribute("userClass")));
            rootOut.addContent(new Element("status").setText(status));
            rootOut.addContent(new Element("error").setText(error));
            rootOut.addContent(new Element("contentType").setText(contentType));
            rootOut.addContent(new Element("path").setText(savePath+fileName));
            request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            //request.setAttribute("XSL.Style", "xml");
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        }
        else {
            Element rootOut = new Element("cms");
            Document jdom = new Document(rootOut);
            rootOut.addContent(new Element("session").setText("fileUpload"));
            rootOut.addContent(new Element("status").setText("upload"));
            rootOut.addContent(new Element("error").setText(error));
            request.setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
            //request.setAttribute("XSL.Style", "xml");
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(request, response);
        }
  }

    private void setFilename(String s) {
        if (s==null)
            return;

        int pos = s.indexOf("filename=\"");
        if (pos != -1) {
            filePath = s.substring(pos+10, s.length()-1);
            // Windows browsers include the full path on the client
            // But Linux/Unix and Mac browsers only send the filename
            // test if this is from a Windows browser
            pos = filePath.lastIndexOf("\\");
        if (pos != -1)
            fileName = filePath.substring(pos + 1);
        else
            fileName = filePath;
        }
    }

    private void setContentType(String s) {
        if (s==null)
            return;

        int pos = s.indexOf(": ");
        if (pos != -1)
            contentType = s.substring(pos+2, s.length());
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }

    /** Destroys the servlet.
     */
    public void destroy() {

    }
}