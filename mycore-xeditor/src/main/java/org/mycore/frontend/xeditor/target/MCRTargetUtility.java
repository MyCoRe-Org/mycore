package org.mycore.frontend.xeditor.target;

import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.JDOMException;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRTargetUtility {
    static Map<String, String[]> getSubmittedValues(MCRServletJob job, String baseXPath) throws JDOMException, JaxenException {
        Map<String, String[]> valuesToSet = new HashMap<String, String[]>();

        for (String paramName : job.getRequest().getParameterMap().keySet()) {
            if (!paramName.startsWith("_xed_")) {
                String xPath = baseXPath + "/" + paramName;
                String[] values = job.getRequest().getParameterValues(paramName);
                valuesToSet.put(xPath, values);
            }
        }
        return valuesToSet;
    }
}
