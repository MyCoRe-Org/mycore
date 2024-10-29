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
package org.mycore.webtools.vue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRURLContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRContentServlet;
import org.mycore.tools.MyCoReWebPageProvider;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>This Servlet can be bound to a URL where a Vue Build App with a createWebHistory() router is present.</p>
 * <pre>
 * <code>
 *
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;MCRVueRootServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.mycore.webtools.vue.MCRVueRootServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;MCRVueRootServlet&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/modules/webtools/texteditor/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </code>
 * </pre>
 * <p>It will pass through resources that exists at this path like javascript and css, but at every other path it will
 * deliver a modified version of the index.html.
 * The index.html is convert to xhtml and then will be wrapped with a MyCoReWebPage, which will produce the surrounding
 * default layout.</p>
 * <p>In the example the vue app is located in: src/main/vue/texteditor</p>
 * <p>The Router needs to be configured like this:</p>
 * <pre>
 * <code>
 *
 * function getContext() {
 *   if (import.meta.env.DEV) {
 *     return import.meta.env.BASE_URL;
 *   }
 *   const el = document.createElement('a');
 *   el.href = getWebApplicationBaseURL();
 *   return el.pathname + "modules/webtools/texteditor/";
 * }
 * const router = createRouter({
 *   history: createWebHistory(getContext()),
 *   routes
 * })
 * </code>
 * </pre>
 * <p>The String "modules/webtools/texteditor/" is the location of the vue app below the java application context.</p>
 * <p>To change the output destination of the vue compiler process you need to change the vue.config.js,
 * if you use vue-cli</p>
 * <pre>
 * <code>
 *
 * const { defineConfig } = require('@vue/cli-service')
 * module.exports = defineConfig({
 *   transpileDependencies: true,
 *   outputDir: "../../../../target/classes/META-INF/resources/modules/webtools/texteditor",
 *   publicPath: "./"
 * });
 * </code>
 * </pre>
 * <p>If you use vite you have to change the vite.config.ts, to change the output destination of the vite compiler
 * process.</p>
 * <a href="https://vitejs.dev/config/">vite config</a>
 * <pre>
 * <code>
 * export default defineConfig({
 *   ...
 *   build: {
 *     outDir: "../../../../target/classes/META-INF/resources/modules/webtools/texteditor",
 *   },
 *   base: "./"
 * });
 * </code>
 * </pre>
 *
 * @author Sebastian Hofmann
 * @author Matthias Eichner
 */
public class MCRVueRootServlet extends MCRContentServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public MCRContent getContent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String indexHtmlPage = getIndexPage();
        String indexHtmlPath = req.getServletPath() + "/" + indexHtmlPage;
        URL resource = getServletContext().getResource(req.getServletPath() + pathInfo);

        if (resource != null && !pathInfo.endsWith("/") && !pathInfo.endsWith(indexHtmlPage)) {
            return new MCRURLContent(resource);
        } else {
            URL indexResource = getServletContext().getResource(indexHtmlPath);
            org.jdom2.Document mycoreWebpage = getIndexDocument(indexResource, getAbsoluteServletPath(req));
            if (pathInfo != null && pathInfo.endsWith("/404")) {
                /* if there is a requested route which does not exist, the app should
                 * redirect to this /404 route the get the actual 404 Code.
                 * see also https://www.youtube.com/watch?v=vjj8B4sq0UI&t=1815s
                 * */
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            try {
                return getLayoutService().getTransformedContent(req, resp, new MCRJDOMContent(mycoreWebpage));
            } catch (TransformerException | SAXException e) {
                throw new IOException(e);
            }
        }
    }

    protected String getIndexPage() {
        return "index.html";
    }

    protected String getAbsoluteServletPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        servletPath = servletPath.startsWith("/") ? servletPath.substring(1) : servletPath;
        return MCRFrontendUtil.getBaseURL() + servletPath;
    }

    protected org.jdom2.Document getIndexDocument(URL indexResource, String absoluteServletPath) throws IOException {
        try (InputStream indexFileStream = indexResource.openStream()) {
            Document document = Jsoup.parse(indexFileStream, StandardCharsets.UTF_8.toString(), "");
            document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            document.outerHtml();
            return buildMCRWebpage(absoluteServletPath, new StringReader(document.outerHtml()));
        } catch (JDOMException e) {
            throw new MCRException(e);
        }
    }

    /**
     * Injects properties into the script. Override if you want to use own properties. By default, only the
     * webApplicationBaseURL is provided.
     *
     * @return properties for javascript injection
     */
    protected Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("webApplicationBaseURL", MCRFrontendUtil.getBaseURL());
        return properties;
    }

    protected org.jdom2.Document buildMCRWebpage(String absoluteServletPath, Reader reader)
        throws JDOMException, IOException {
        org.jdom2.Document jdom = new SAXBuilder().build(reader);
        Element jdomRoot = jdom.getRootElement();

        List<Content> scriptAndLinks = buildScriptAndLinks(absoluteServletPath, jdomRoot);
        List<Content> bodyContent = buildBodyContent(jdomRoot);

        List<Content> content = Stream.of(scriptAndLinks, bodyContent)
            .flatMap(Collection::stream)
            .toList();

        MyCoReWebPageProvider mycoreWebPage = new MyCoReWebPageProvider();
        mycoreWebPage.addSection("", content, "de");
        return mycoreWebPage.getXML();
    }

    protected List<Content> buildBodyContent(Element root) {
        List<Element> body = root.getChild("body").getChildren();
        return new ArrayList<>(body).stream()
            .map(Element::detach)
            .collect(Collectors.toList());
    }

    protected List<Content> buildScriptAndLinks(String absoluteServletPath, Element root) {
        // inject properties
        Element propertiesScript = buildPropertiesScript();

        // script and links from vue's index.html
        List<Element> vueScriptAndLink = buildVueScriptAndLink(absoluteServletPath, root);

        // combine
        List<Content> scriptAndLinks = new ArrayList<>();
        scriptAndLinks.add(propertiesScript);
        scriptAndLinks.addAll(vueScriptAndLink);
        return scriptAndLinks;
    }

    /**
     * Creates a new script tag embedding the given {@link #getProperties()} as javascript variables. The variables
     * are stored under the mycore variable e.g. 'mycore.webApplicationBaseURL'.
     *
     * @return script element tag with properties javascript variables
     */
    protected Element buildPropertiesScript() {
        Element propertiesScript = new Element("script");
        propertiesScript.setAttribute("type", "text/javascript");
        StringBuilder propertiesJson = new StringBuilder("var mycore = mycore || {};");
        propertiesJson.append(System.lineSeparator());
        getProperties().forEach((key, value) -> propertiesJson.append("mycore.").append(key).append("=\"").append(value)
            .append("\";").append(System.lineSeparator()));
        propertiesScript.setText(propertiesJson.toString());
        return propertiesScript;
    }

    /**
     * Extracts the script and link elements out of the vue index.html.
     *
     * @param absoluteServletPath the absolute servlet path
     * @param root the root element
     * @return list of script and link elements
     */
    protected List<Element> buildVueScriptAndLink(String absoluteServletPath, Element root) {
        return root.getChild("head").getChildren().stream()
            .filter(el -> "script".equals(el.getName()) || "link".equals(el.getName()))
            .toList()
            .stream().map(Element::detach).peek(el -> {
                String hrefAttr = el.getAttributeValue("href");
                if (hrefAttr != null) {
                    el.setAttribute("href", absoluteServletPath + "/" + hrefAttr);
                }
                String srcAttr = el.getAttributeValue("src");
                if (srcAttr != null) {
                    el.setAttribute("src", absoluteServletPath + "/" + srcAttr);
                }
            }).toList();
    }

}
