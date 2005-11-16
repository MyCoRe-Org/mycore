package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.query.MCRQueryCache;

public class MCRAdminServlet extends MCRServlet{
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try{
            ServletContext context = this.getServletContext();
            
            String page = request.getParameter("path");
            if (page == null || page.equals("")){
                page = "main.jsp";
            }else{
                page += ".jsp";
            }
            request.setAttribute("page", page );
            
            
            
            
            String requestPath = request.getPathInfo();
            if (requestPath == null || requestPath.equals("/")){
                requestPath = "/main";
            }
            request.setAttribute("path", requestPath );
            
            //Stylepath
            String stylepath = "/";
            java.util.StringTokenizer st = new java.util.StringTokenizer(request.getRequestURI(), "/");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.equals("admin")){
                    break;
                }else{
                    stylepath += token + "/";
                }
             }
            
            request.setAttribute("basepath", stylepath + "administration/" );
            stylepath +="administration/css/admin.css";
            request.setAttribute("stylepath", stylepath );

            context.getRequestDispatcher("/admin/index.jsp").forward(request, response);

            
        }catch(Exception e){
            System.out.println(e);
        }
        
    }
}
