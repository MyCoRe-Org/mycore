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

package org.mycore.frontend.jersey.resources;

import java.util.Set;

import org.mycore.common.MCRJSONUtils;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.services.i18n.MCRTranslation;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Path("locale")
public class MCRLocaleResource {

    private static final String PARAM_KEY = "key";

    @Context
    private HttpServletResponse resp;

    @Context
    private ServletContext context;

    private long cacheTime;
    private long startUpTime;

    @PostConstruct
    public void init() {
        String cacheParam = context.getInitParameter("cacheTime");
        cacheTime = cacheParam != null ? Long.parseLong(cacheParam) : (60 * 60 * 24); //default is one day
        startUpTime = System.currentTimeMillis();
    }

    /**
     * Returns the current language in ISO 639 (two character) format.
     * 
     * @return current language as plain text
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("language")
    public String language() {
        return MCRTranslation.getCurrentLocale().getLanguage();
    }

    /**
     * Returns all available languages of the application. The codes are in ISO 639 (two character) format.
     * 
     * @return json array of all languages available
     */
    @GET
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF_8)
    @Path("languages")
    @MCRStaticContent
    public String languages() {
        Set<String> availableLanguages = MCRTranslation.getAvailableLanguages();
        return MCRJSONUtils.getJsonArray(availableLanguages).toString();
    }

    /**
     * Translates a set of keys to the given language.
     * 
     * @param lang desired language
     * @param key message key ending with an asterisk (e.g. component.classeditor.*)
     * @return json object containing all keys and their corresponding translation
     */
    @GET
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF_8)
    @Path("translate/{lang}/{key: .*\\*}")
    @MCRStaticContent
    public String translateJSON(@PathParam("lang") String lang, @PathParam(PARAM_KEY) String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRJSONUtils.getTranslations(key, lang);
    }

    /**
     * Translates a set of keys to the current language.
     *
     * @param key message key ending with an asterisk (e.g. component.classeditor.*)
     * @return json object containing all keys and their corresponding translation in current language
     */
    @GET
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF_8)
    @Path("translate/{key: .*\\*}")
    public String translateJSONDefault(@PathParam(PARAM_KEY) String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRJSONUtils.getTranslations(key.substring(0, key.length() - 1),
            MCRSessionMgr.getCurrentSession().getCurrentLanguage());
    }

    /**
     * Translates a single key to the given language.
     * 
     * @param lang desired language
     * @param key the key to translate (e.g. component.classeditor.save.successful)
     * @return translated plain text
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("translate/{lang}/{key: [^\\*]+}")
    @MCRStaticContent
    public String translateText(@PathParam("lang") String lang, @PathParam(PARAM_KEY) String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRTranslation.translateToLocale(key, MCRTranslation.getLocale(lang));
    }

    /**
     * Translates a single key to the current language.
     *
     * @param key the key to translate (e.g. component.classeditor.save.successful)
     * @return translated plain text
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("translate/{key: [^\\*]+}")
    public String translateTextDefault(@PathParam(PARAM_KEY) String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRTranslation.translate(key, MCRTranslation.getCurrentLocale());
    }

}
