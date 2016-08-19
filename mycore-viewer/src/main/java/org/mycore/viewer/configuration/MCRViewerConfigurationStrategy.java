package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

/**
 * Strategy which decides which image viewer configuration should be loaded by the
 * given request.
 * 
 * @author Matthias Eichner
 */
public interface MCRViewerConfigurationStrategy {

    /**
     * Gets the image view configuration.
     */
    public MCRViewerConfiguration get(HttpServletRequest request);

}
