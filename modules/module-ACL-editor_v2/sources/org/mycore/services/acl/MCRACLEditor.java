package org.mycore.services.acl;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.xml.MCRLayoutService;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public abstract class MCRACLEditor {
    protected MCRLayoutService layoutService = MCRServlet.getLayoutService();
    
    /**
     * Return a full blown up ACL permission editor
     * 
     * @param job
     * @param output TODO
     */
    public abstract void getPermEditor(MCRServletJob job);
    
    /**
     * Return a smaller (embedded) ACL permission editor
     * @param job
     * @param output TODO
     */
    public abstract void getEmbPermEditor(MCRServletJob job);
    
    /**
     * Return a full blown up ACL rule editor
     * 
     * @param job
     * @param output TODO
     */
    public abstract void getRuleEditor(MCRServletJob job);
    
    /**
     * Return a smaller (embedded) ACL permission editor
     * 
     * @param job
     * @param output TODO
     */
    public abstract void getEmbRuleEditor(MCRServletJob job);
    
    /**
     * Process the incomming data from editor
     * 
     * @param job
     * @param output TODO
     */
    public abstract void processingInput(MCRServletJob job);
    
    
    public static MCRACLEditor instance(){
        return (MCRACLEditor) MCRConfiguration.instance().getInstanceOf("MCR.ACL.Editor.class");
    }
}
