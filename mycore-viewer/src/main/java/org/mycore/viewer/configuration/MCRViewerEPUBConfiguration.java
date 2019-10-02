package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.MCRFrontendUtil;

public class MCRViewerEPUBConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        final boolean debugMode = isDebugMode(request);
        addLocalScript("lib/epubjs/epub.js", true, debugMode);
        addLocalScript("lib/jszip/jszip.js", true, debugMode);
        addLocalScript("iview-client-epub.js", true, debugMode);
        addLocalScript("lib/es6-promise/es6-promise.auto.js", true, debugMode);

        final String derivate = getDerivate(request);
        final String filePath = getFilePath(request);

        // put / at the end to let epubjs know its extracted
        setProperty("epubPath", MCRFrontendUtil.getBaseURL(request) + "rsc/epub/" + derivate + filePath + "/");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "epub";
    }
}
