package org.mycore.frontend.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRJSONUtils;
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

    private static final Logger LOGGER = LogManager.getLogger();

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
        String returnValue;
        String lang = getLang(req.getPathInfo());
        if (lang.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "\"type\" parameter and language is undefined");
            return;
        }
        String key = getKey(req.getPathInfo());
        if (key.endsWith("*")) {
            returnValue = handlePrefetch(key, lang);
            resp.setContentType("text/json");
        } else {
            returnValue = handleGetI18n(key, lang);
            resp.setContentType("text/plain");
        }
        MCRFrontendUtil.writeCacheHeaders(resp, (long) cacheTime, startUpTime, true);

        resp.setCharacterEncoding(CHARSET);
        resp.getWriter().print(returnValue);
    }

    private String getKey(String pathInfo) {
        int pos = pathInfo.indexOf('/', 1);
        String key = pathInfo.substring(pos + 1);
        LOGGER.info("Getting translations for key " + key);
        return key;
    }

    protected static String getLang(String pathInfo) {
        StringBuilder lang = new StringBuilder(pathInfo.length());
        boolean running = true;
        for (int i = (pathInfo.charAt(0) == '/') ? 1 : 0; (i < pathInfo.length() && running); i++) {
            switch (pathInfo.charAt(i)) {
                case '/':
                    running = false;
                    break;
                default:
                    lang.append(pathInfo.charAt(i));
                    break;
            }
        }
        return lang.toString();
    }

    private String handleGetI18n(String label, String lang) {
        return MCRTranslation.translate(label, MCRTranslation.getLocale(lang));
    }

    private String handlePrefetch(String prefix, String lang) {
        return MCRJSONUtils.getTranslations(prefix, lang);
    }
}
