package org.mycore.services.acl;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;

public abstract class MCRAclEditor {
    protected static Logger LOGGER = Logger.getLogger(MCRAclEditor.class);
    
    public abstract Element getACLEditor(HttpServletRequest request);   // Complete ACL-editor
//    public abstract Element getPermEditor(HttpServletRequest request); // Editor for mapping
//    public abstract Element getRuleEditor(HttpServletRequest request); // Editor for rules
    public abstract Element dataRequest(HttpServletRequest request);
    
    public static MCRAclEditor instance(){
        return (MCRAclEditor) MCRConfiguration.instance().getInstanceOf("MCR.ACL.Editor.class");
    }
     
}
