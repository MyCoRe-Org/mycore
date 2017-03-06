package org.mycore.frontend.jersey.resources;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.mycore.common.MCRJSONUtils;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyUtil;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.services.i18n.MCRTranslation;

@Path("locale")
@MCRStaticContent
public class MCRLocaleResource {

    @Context
    private HttpServletResponse resp;

    @Context
    private ServletContext context;

    private long cacheTime, startUpTime;

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
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF8)
    @Path("languages")
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
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF8)
    @Path("translate/{lang}/{key: .*\\*}")
    public String translateJSON(@PathParam("lang") String lang, @PathParam("key") String key) {
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
    @Produces(MCRJerseyUtil.APPLICATION_JSON_UTF8)
    @Path("translate/{key: .*\\*}")
    public String translateJSONDefault(@PathParam("key") String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRJSONUtils.getTranslations(key.substring(0, key.length() - 1), MCRSessionMgr.getCurrentSession().getCurrentLanguage());
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
    public String translateText(@PathParam("lang") String lang, @PathParam("key") String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRTranslation.translate(key, MCRTranslation.getLocale(lang));
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
    public String translateTextDefault(@PathParam("key") String key) {
        MCRFrontendUtil.writeCacheHeaders(resp, cacheTime, startUpTime, true);
        return MCRTranslation.translate(key, MCRTranslation.getCurrentLocale());
    }

}
