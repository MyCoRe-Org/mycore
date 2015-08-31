package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.common.MCRJSONUtils;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.resources.MCRLocaleResource;
import org.mycore.services.i18n.MCRTranslation;

/**
 * Use {@link MCRLocaleResource} instead
 */
@Deprecated
public class MCRLocaleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String CHARSET = MCRConfiguration.instance().getString("MCR.Request.CharEncoding", "UTF-8");

    private static final Logger LOGGER = Logger.getLogger(MCRLocaleServlet.class);

    private int cacheTime;

    private long startUpTime;

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        String cacheParam = getInitParameter("cacheTime");
        cacheTime = cacheParam != null ? Integer.parseInt(cacheParam) : (60 * 60 * 24);//default is one day
        startUpTime = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#getLastModified(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected long getLastModified(HttpServletRequest request) {
        return startUpTime;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // remove leading /
        String pathInfo = req.getPathInfo().startsWith("/") ? req.getPathInfo().substring(1) : req.getPathInfo();

        String[] pathParts = pathInfo.split("/");
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        int realPathIndex = 0;

        boolean hasFirstPart = pathParts.length > 0;
        if (!hasFirstPart) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No first Parameter found!");
        }

        boolean isFirstPartLanguage = MCRTranslation.getAvailableLanguages().contains(pathParts[0]);
        if (isFirstPartLanguage) {
            lang = pathParts[0];
            realPathIndex++;
        }

        writeResult(resp, lang, pathParts[realPathIndex]);
    }

    private void writeResult(HttpServletResponse resp, String lang, String key) throws IOException {
        MCRFrontendUtil.writeCacheHeaders(resp, (long) cacheTime, startUpTime, true);
        resp.setCharacterEncoding(CHARSET);
        if (key.endsWith("*")) {
            resp.setContentType("text/json");
            resp.getWriter().print(handlePrefetch(key.substring(0, key.length() - 1), lang));
        } else {
            resp.setContentType("text/plain");
            resp.getWriter().print(handleGetI18n(key, lang));
        }
    }


    private String handleGetI18n(String label, String lang) {
        return MCRTranslation.translate(label, MCRTranslation.getLocale(lang));
    }

    private String handlePrefetch(String prefix, String lang) {
        return MCRJSONUtils.getTranslations(prefix, lang);
    }
}
