package org.mycore.viewer.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.MCRFrontendUtil;

public class MCRViewerEPUBConfiguration extends MCRViewerBaseConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);

        addLocalScript("lib/epubjs/epub.js", true, isDebugMode(request));
        addLocalScript("lib/jszip/jszip.js", true, isDebugMode(request));
        addLocalScript("iview-client-epub.js", true, isDebugMode(request));

        final String derivate = getDerivate(request);
        final String filePath = getFilePath(request);

        // put / at the end to let epubjs know its extracted
        setProperty("epubPath",  MCRFrontendUtil.getBaseURL(request) + "rsc/epub/" + derivate + filePath + "/");

        return this;
    }

    @Override
    public String getDocType(HttpServletRequest request) {
        return "epub";
    }
}
