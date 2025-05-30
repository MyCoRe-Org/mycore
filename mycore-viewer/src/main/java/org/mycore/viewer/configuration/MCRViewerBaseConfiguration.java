/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.viewer.configuration;

import java.util.Locale;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;

import jakarta.servlet.http.HttpServletRequest;

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
        setProperty("adminMail", MCRConfiguration2.getString("MCR.Mail.Recipients").orElse(""));

        final String canvasOverviewEnabled = MCRConfiguration2.getString("MCR.Viewer.canvas.overview.enabled")
            .orElse("true");
        setProperty("canvas.overview.enabled", Boolean.valueOf(canvasOverviewEnabled));

        final String canvasOverviewCanvasOverviewMinVisibleSize = MCRConfiguration2
            .getString("MCR.Viewer.canvas.overview.minVisibleSize").orElse(null);
        if (canvasOverviewCanvasOverviewMinVisibleSize != null) {
            setProperty("canvas.overview.minVisibleSize", canvasOverviewCanvasOverviewMinVisibleSize);
        }

        final String canvasStartupFitWidth = MCRConfiguration2.getString("MCR.Viewer.canvas.startup.fitWidth")
            .orElse(null);
        if (canvasStartupFitWidth != null && canvasStartupFitWidth.toLowerCase(Locale.ROOT).equals("true")) {
            setProperty("canvas.startup.fitWidth", true);
        }

        String leftShowOnStart = MCRConfiguration2.getString("MCR.Viewer.leftShowOnStart").orElse(null);
        if (leftShowOnStart != null) {
            setProperty("leftShowOnStart", leftShowOnStart);
        }

        // script & css
        boolean developerMode = isDebugMode(request);
        addLocalScript("iview-client-base.es.js", false, true, developerMode);
        final boolean framed = this.isFramed(request);

        if (framed) {
            addLocalScript("iview-client-frame.es.js", false, true, developerMode);
        } else if (this.getEmbeddedParameter(request) != null) {
            addLocalScript("iview-client-frame.es.js", false, true, developerMode);
            setProperty("embedded", "true");
            setProperty("permalink.updateHistory", false);
            setProperty("chapter.showOnStart", false);
        } else {
            addLocalScript("iview-client-desktop.es.js", false, true, developerMode);
        }

        addLocalCSS("default.css");

        String maximalScale = MCRConfiguration2.getString("MCR.Viewer.Canvas.Startup.MaximalPageScale").orElse("");
        if (!maximalScale.isEmpty()) {
            setProperty("maximalPageScale", maximalScale);
        }

        return this;
    }

    private String getEmbeddedParameter(HttpServletRequest request) {
        return request.getParameter("embedded");
    }

    protected boolean isMobile(HttpServletRequest req) {
        String mobileParameter = req.getParameter("mobile");
        if (mobileParameter != null) {
            return Boolean.parseBoolean(mobileParameter);
        } else {
            return req.getHeader("User-Agent").contains("Mobile");
        }
    }

    protected boolean isFramed(HttpServletRequest req) {
        String frameParameter = req.getParameter("frame");
        return Boolean.parseBoolean(frameParameter);
    }

    public abstract String getDocType(HttpServletRequest request);

}
