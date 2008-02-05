package org.mycore.frontend.servlets;


import java.io.IOException;
import java.util.ArrayList;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.mycore.access.mcrimpl.MCRRuleStore;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRAccRuleServlet extends MCRServlet{
    private static final long serialVersionUID = 1L;

    /**
     * operation:
     *  - select: returns rule as XML-String needs ruleid
     *  - selectall: returns all rules as XML-String
     *  - delete: deletes rule with given ruleid
     *  - create: creates new rule
     *  - update: updates rule with given ruleid
     *  
     * ruleid:
     *  ruleid as string       
     */
    public void doGetPost(MCRServletJob job) throws IOException {
        //get parameter
        String operation = getProperty(job.getRequest(), "operation");
        String ruleid = getProperty(job.getRequest(), "ruleid");

        
        Document jdom = new Document(new Element("mcraccessrules"));
        if (operation.equals("select")){
            // select single rule
            Element el = getRule(ruleid);
            jdom.getRootElement().addContent(el);
        }else if(operation.equals("selectall")){
            // select all rules
            ArrayList rules = getAllRules();
            for (int i=0; i<rules.size(); i++ ){
                jdom.getRootElement().addContent((Element) rules.get(i));
            }
        }else if(operation.equals("delete")){
            // delete rule
            MCRRuleStore.getInstance().deleteRule(ruleid);
            Element el = new Element("mcraccessrule");
            el.setAttribute(new Attribute("operation","deleted"));
        }

        getLayoutService().doLayout(job.getRequest(),job.getResponse(),jdom);
    }

    /**
     * Helper method returns rule as Element by given ruleid
     * @param ruleid ruleid as string
     * @return xmlelement representation of MCRAccessRule
     */
    private Element getRule(String ruleid){
        return MCRRuleStore.getInstance().retrieveRule(ruleid).getRuleElement();
    }
    
    /**
     * returns an arraylist with all ruleids
     * @return arraylist with all rules as jdom element
     */
    private ArrayList getAllRules(){
        ArrayList ret = new ArrayList();
        ArrayList tmp = MCRRuleStore.getInstance().retrieveAllIDs();
        for (int i=0; i<tmp.size(); i++){
            ret.add(getRule((String) tmp.get(i)));
        }
        return ret;
    }
 
    
    
}
