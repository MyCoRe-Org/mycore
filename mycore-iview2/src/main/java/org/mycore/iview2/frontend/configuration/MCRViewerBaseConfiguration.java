package org.mycore.iview2.frontend.configuration;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Base configuration for the mycore image viewer. Sets the following parameter:
 * <li><b>webApplicationBaseURL:</b> base URL of the mycore system</li>
 * <li><b>derivate:</b> name of the derivate which should be displayed.</li>
 * <li><b>filePath: path of the file which should be displayed (in the derivate)</b></li>
 * <li><b>doctype:</b> the type of the structure e.g. (mets/pdf) </li>
 * <li><b>mobile:</b> should the mobile or the desktop client started.</li>
 * <li><b>i18nURL:</b> URL to the i18n.json</li>
 * <li><b>lang:</b> current selected language</li>
 * 
 * @author Matthias Eichner
 */
public abstract class MCRViewerBaseConfiguration extends MCRViewerConfiguration {

    @Override
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        super.setup(request);
        // property
        setProperty("webApplicationBaseURL", MCRFrontendUtil.getBaseURL());
        setProperty("derivate", getDerivate(request));
        setProperty("filePath", getFilePath(request));
        setProperty("doctype", getDocType(request));
        boolean mobile = isMobile(request);
        setProperty("mobile", mobile);
        setProperty("i18nURL", MCRServlet.getServletBaseURL() + "MCRLocaleServlet/{lang}/component.iview2.*");
        setProperty("lang", MCRSessionMgr.getCurrentSession().getCurrentLanguage());

        // script & css
        addLocalScript("iview-client-base.js");
        if (mobile) {
            addLocalScript("iview-client-mobile.js");
            addLocalCSS("mobile.css");
        } else {
            addLocalScript("iview-client-desktop.js");
            addLocalCSS("default.css");
        }
        return this;
    }

    protected boolean isMobile(HttpServletRequest req) {
        String mobileParameter = req.getParameter("mobile");
        if (mobileParameter != null) {
            return mobileParameter.toLowerCase().equals(Boolean.TRUE.toString());
        } else {
            return req.getHeader("User-Agent").indexOf("Mobile") != -1;
        }
    }

    public abstract String getDocType(HttpServletRequest request);

}
