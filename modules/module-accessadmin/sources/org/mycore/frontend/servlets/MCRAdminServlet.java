package org.mycore.frontend.servlets;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.mcrimpl.MCRAccessRule;
import org.mycore.access.mcrimpl.MCRAccessStore;
import org.mycore.access.mcrimpl.MCRRuleMapping;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.frontend.servlets.MCRServlet;

public class MCRAdminServlet extends MCRServlet{
	
//	private static boolean isDefaultAccessRuleCreated ;
	protected final static Logger LOGGER = Logger.getLogger(MCRAdminServlet.class);

	public void init() throws ServletException {
		super.init();
//		isDefaultAccessRuleCreated = createAdminDefaultRule();
	}
    
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
                }
                stylepath += token + "/";
             }
            
            request.setAttribute("basepath", stylepath + "administration/" );
            stylepath +="administration/css/admin.css";
            request.setAttribute("stylepath", stylepath );

            context.getRequestDispatcher("/admin/index.jsp").forward(request, response);

            
        }catch(Exception e){
            System.out.println(e);
        }
        
    }
    
	/**
	 * sets a default-rule for the use of the MCRAdminServlet
	 * 
	 * @return boolean  false if there was an Exception
	 */
	public static boolean createAdminDefaultRule() {
		try {
			MCRAccessStore accessStore = (MCRAccessStore) Class.forName(CONFIG.getString("MCR.accessstore_class_name")).newInstance();
			MCRRuleStore ruleStore = (MCRRuleStore) Class.forName(CONFIG.getString("MCR.rulestore_class_name")).newInstance();			
			String ruleID = "STANDARD-ACCESS-ADMIN-RULE";
			MCRAccessRule rule = ruleStore.getRule(ruleID);
			if (rule == null || rule.equals("")) {
				StringBuffer ruleExpression = new StringBuffer("(user administrator)");
				rule = new MCRAccessRule(ruleID, "administrator", 
						new Date(),ruleExpression.toString(),
						"PoolRight only for the user administrator'");
				ruleStore.createRule(rule);	
				LOGGER.info("Rule created: " + ruleID);
			}

			MCRRuleMapping ruleMapping = new MCRRuleMapping();
			ruleMapping.setCreator("administrator");
			ruleMapping.setCreationdate(new Date());
			ruleMapping.setPool("modify");
			ruleMapping.setRuleId(ruleID);
			ruleMapping.setObjId("MCRAdminServlet");
			
			String anotherRuleID = accessStore.getRuleID("MCRAdminServlet","modify");
			if(anotherRuleID == null || anotherRuleID.equals("")) {
				accessStore.createAccessDefinition(ruleMapping);
				LOGGER.info("Following rule was created for StringID MCRAdminServlet" 
						+ " in the pool 'modify': " + ruleID);
			}
		} catch (Exception e) {
			return false;
		}				
		return true;
	}    
}
