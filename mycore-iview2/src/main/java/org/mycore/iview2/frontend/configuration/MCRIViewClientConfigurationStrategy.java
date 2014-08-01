package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

/**
 * Strategy which decides which image viewer configuration should be loaded by the
 * given request.
 * 
 * @author Matthias Eichner
 */
public interface MCRIViewClientConfigurationStrategy {

    /**
     * Gets the image view configuration.
     * 
     * @param request
     * @return
     */
    public MCRIViewClientConfiguration get(HttpServletRequest request);

}
