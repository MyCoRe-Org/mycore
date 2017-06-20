package org.mycore.viewer.configuration;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

/**
 * Base configuration for the mycore image viewer. Sets the following parameter:
 * <dl>
 * <dt>webApplicationBaseURL</dt><dd>base URL of the mycore system</dd>
 * <dt>derivate</dt><dd>name of the derivate which should be displayed.</dd>
 * <dt>filePath</dt><dd>path of the file which should be displayed (in the derivate)</dd>
 * <dt>doctype</dt><dd>the type of the structure e.g. (mets/pdf) </dd>
 * <dt>mobile</dt><dd>should the mobile or the desktop client started.</dd>
 * <dt>i18nURL</dt><dd>URL to the i18n.json</dd>
 * <dt>lang</dt><dd>current selected language</dd>
 * </dl>
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
        setProperty("i18nURL",
            MCRFrontendUtil.getBaseURL(request) + "rsc/locale/translate/{lang}/component.mets.*,component.viewer.*");
        setProperty("derivateURL", MCRServlet.getServletBaseURL() + "MCRFileNodeServlet/" + getDerivate(request) + "/");
        setProperty("lang", MCRSessionMgr.getCurrentSession().getCurrentLanguage());
        setProperty("adminMail", MCRConfiguration.instance().getString("MCR.Mail.Recipients"));

        final String canvasOverviewEnabled = MCRConfiguration.instance().getString("canvas.overview.enabled", "true");
        if (canvasOverviewEnabled != null) {
            setProperty("canvas.overview.enabled", Boolean.valueOf(canvasOverviewEnabled));
        }

        final String canvasOverviewCanvasOverviewMinVisibleSize = MCRConfiguration.instance()
            .getString("MCR.Viewer.canvas.overview.minVisibleSize", null);
        if (canvasOverviewCanvasOverviewMinVisibleSize != null) {
            setProperty("canvas.overview.minVisibleSize", canvasOverviewCanvasOverviewMinVisibleSize);
        }

        final String canvasStartupFitWidth = MCRConfiguration.instance().getString("MCR.Viewer.canvas.startup.fitWidth",
            null);
        if (canvasStartupFitWidth != null && canvasStartupFitWidth.toLowerCase(Locale.ROOT).equals("true")) {
            setProperty("canvas.startup.fitWidth", true);
        }

        String leftShowOnStart = MCRConfiguration.instance().getString("MCR.Viewer.leftShowOnStart", null);
        if (leftShowOnStart != null) {
            setProperty("leftShowOnStart", leftShowOnStart);
        }

        // script & css
        boolean developerMode = isDebugParameterSet(request);
        addLocalScript("iview-client-base.js", developerMode);
        if (mobile) {
            addLocalScript("iview-client-mobile.js", developerMode);
            addLocalCSS("mobile.css");
        } else {
            if (this.isFramed(request)) {
                addLocalScript("iview-client-frame.js", developerMode);
            } else {
                addLocalScript("iview-client-desktop.js", developerMode);
            }

            addLocalCSS("default.css");
        }
        return this;
    }

    protected boolean isMobile(HttpServletRequest req) {
        String mobileParameter = req.getParameter("mobile");
        if (mobileParameter != null) {
            return Boolean.parseBoolean(mobileParameter);
        } else {
            return req.getHeader("User-Agent").indexOf("Mobile") != -1;
        }
    }

    protected boolean isFramed(HttpServletRequest req) {
        String frameParameter = req.getParameter("frame");
        return frameParameter != null && Boolean.parseBoolean(frameParameter);
    }

    public abstract String getDocType(HttpServletRequest request);

}
