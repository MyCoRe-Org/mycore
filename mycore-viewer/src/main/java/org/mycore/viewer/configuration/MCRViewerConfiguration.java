/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRXMLContent;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * Base class for the iview client configuration. You can add properties, javascript files and css files.
 * To retrieve the configuration in xml call {@link #toXML()}. To get it in json call {@link #toJSON()}. 
 * 
 * @author Matthias Eichner
 */
public class MCRViewerConfiguration {

    private static Logger LOGGER = LogManager.getLogger(MCRViewerConfiguration.class);

    public enum ResourceType {
        script, css
    }

    private Multimap<ResourceType, String> resources;

    private Map<String, Object> properties;

    private static boolean DEBUG_MODE;

    private static Pattern REQUEST_PATH_PATTERN;

    static {
        DEBUG_MODE = Boolean.parseBoolean(MCRConfiguration2.getString("MCR.Viewer.DeveloperMode").orElse("false"));
        REQUEST_PATH_PATTERN = Pattern.compile("/(\\w+_derivate_\\d+)(/.*)?");
    }

    public MCRViewerConfiguration() {
        resources = LinkedListMultimap.create();
        properties = new HashMap<>();
    }

    /**
     * Returns the properties of this configuration.
     *
     * @return map of all properties set in this configuration
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Returns a multimap containing all resources (javascript and css url's).
     *
     * @return map of resources
     */
    public Multimap<ResourceType, String> getResources() {
        return resources;
    }

    /**
     * Setup's the configuration with the request.
     * 
     * @param request the request which should be parsed to build this configuration.
     * @return itself
     */
    public MCRViewerConfiguration setup(HttpServletRequest request) {
        return this;
    }

    /**
     * Returns true if the debug/developer mode is active.
     * 
     * <ul>
     *   <li>URL parameter iview2.debug = true</li>
     *   <li>MCR.Viewer.DeveloperMode property = true</li>
     * </ul>
     *
     * @param request the request to check the iview2.debug parameter
     * @return true if the debug mode is active, otherwise false
     */
    public static boolean isDebugMode(HttpServletRequest request) {
        return DEBUG_MODE ||
            Boolean.TRUE.toString().toLowerCase(Locale.ROOT).equals(request.getParameter("iview2.debug"));
    }

    /**
     * Helper method to get the derivate id of the given request. Returns null
     * if no derivate identifier could be found in the request object.
     * 
     * @param request http request
     * @return the derivate id embedded in the path of the request
     */
    public static String getDerivate(HttpServletRequest request) {
        try {
            return getFromPath(request.getPathInfo(), 1);
        } catch (Exception exc) {
            LOGGER.warn("Unable to get the derivate id of request {}", request.getRequestURI());
            return null;
        }
    }

    /**
     * Helper method to get the path to the start file. The path is
     * URI decoded and starts with a slash.
     * 
     * @param request http request
     * @return path to the file or null if the path couldn't be retrieved
     */
    public static String getFilePath(HttpServletRequest request) {
        try {
            String fromPath = getFromPath(request.getPathInfo(), 2);
            if (fromPath == null || fromPath.isEmpty() || fromPath.equals("/")) {
                String derivate = getDerivate(request);
                MCRDerivate deriv = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derivate));
                String nameOfMainFile = deriv.getDerivate().getInternals().getMainDoc();
                return "/" + nameOfMainFile;
            }
            return fromPath;
        } catch (Exception exc) {
            LOGGER.warn("Unable to get the file path of request {}", request.getRequestURI());
            return null;
        }
    }

    /**
     * Gets the group from the {@link #REQUEST_PATH_PATTERN}.
     * 
     * @param path uri decoded path
     * @param groupNumber the group number which should be returnd
     * @return the value of the regular expression group
     */
    private static String getFromPath(String path, int groupNumber) {
        Matcher matcher = REQUEST_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(groupNumber);
        }
        return null;
    }

    /**
     * Adds a new javascript file which should be included by the image viewer.
     */
    public void addScript(final String url) {
        this.resources.put(ResourceType.script, url);
    }

    /**
     * Shorthand MCRViewerConfiguration#addLocalScript(String, true, false)
     *
     * @param file the local javascript file to include
     */
    public void addLocalScript(final String file) {
        this.addLocalScript(file, true, false);
    }

    /**
     * Shorthand MCRViewerConfiguration#addLocalScript(file, hasMinified, false)
     *
     * @param file the local javascript file to include
     * @param hasMinified is a minified version available
     */
    public void addLocalScript(final String file, boolean hasMinified) {
        this.addLocalScript(file, hasMinified, false);
    }

    /**
     * Adds a local (based in modules/iview2/js/) javascript file which should be included
     * by the image viewer. You should always call this method with the base file version.
     * E.g. addLocalScript("iview-client-mobile.js");.
     *
     * <p>This method uses the minified (adds a .min) version only if hasMinified is true and debugMode is false.</p>.
     *
     * @param file the local javascript file to include
     * @param hasMinified is a minified version available
     * @param debugMode if the debug mode is active or not
     */
    public void addLocalScript(final String file, final boolean hasMinified, final boolean debugMode) {
        String baseURL = MCRFrontendUtil.getBaseURL();
        StringBuilder scriptURL = new StringBuilder(baseURL);
        scriptURL.append("modules/iview2/js/");
        if (hasMinified && !debugMode) {
            scriptURL.append(file, 0, file.lastIndexOf("."));
            scriptURL.append(".min.js");
        } else {
            scriptURL.append(file);
        }
        addScript(scriptURL.toString());
    }

    /**
     * Adds a new css file which should be included by the image viewer.
     *
     * @param url the url to add e.g. baseURL + "modules/iview2/css/my.css"
     */
    public void addCSS(final String url) {
        this.resources.put(ResourceType.css, url);
    }

    /**
     * Adds a local (based in modules/iview2/css/) css file.
     * 
     * @param file to include
     */
    public void addLocalCSS(final String file) {
        String baseURL = MCRFrontendUtil.getBaseURL();
        addCSS(baseURL + "modules/iview2/css/" + file);
    }

    /**
     * Sets a new property.
     * 
     * @param name name of the property
     * @param value value of the property
     */
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }

    /**
     * Removes a property by name.
     * 
     * @param name name of the property which should be removed
     * @return the removed value or null when nothing is done
     */
    public Object removeProperty(String name) {
        return this.properties.remove(name);
    }

    /**
     * Returns the configuration in json format.
     * 
     * @return json the configuration as json string
     */
    public String toJSON() {
        final Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapSerializer()).create();
        return gson.toJson(this);
    }

    /**
     * Returns the configuration in xml content format.
     *
     * @return the configuration as xml content
     */
    public MCRXMLContent toXML() throws JAXBException {
        MCRIViewClientXMLConfiguration xmlConfig = new MCRIViewClientXMLConfiguration(resources, properties);
        return new MCRJAXBContent<>(
            JAXBContext.newInstance(xmlConfig.getClass()), xmlConfig);
    }

    @XmlRootElement(name = "xml")
    private static class MCRIViewClientXMLConfiguration {

        private Multimap<ResourceType, String> resources;

        private Map<String, Object> properties;

        @SuppressWarnings("unused")
        MCRIViewClientXMLConfiguration() {
        }

        MCRIViewClientXMLConfiguration(Multimap<ResourceType, String> resources,
            Map<String, Object> properties) {
            this.resources = resources;
            this.properties = properties;
        }

        @XmlElements({ @XmlElement(name = "resource") })
        @XmlElementWrapper(name = "resources")
        public final List<MCRIViewClientResource> getResources() {
            return resources.entries()
                .stream()
                .map(entry -> new MCRIViewClientResource(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        }

        @XmlElements({ @XmlElement(name = "property") })
        @XmlElementWrapper(name = "properties")
        public final List<MCRIViewClientProperty> getProperties() {
            return properties.entrySet()
                .stream()
                .map(entry -> new MCRIViewClientProperty(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        }

    }

    @XmlRootElement(name = "resource")
    private static class MCRIViewClientResource {

        @XmlAttribute(name = "type", required = true)
        public ResourceType type;

        @XmlValue
        public String url;

        @SuppressWarnings("unused")
        MCRIViewClientResource() {
        }

        MCRIViewClientResource(ResourceType type, String url) {
            this.type = type;
            this.url = url;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MCRIViewClientResource)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            MCRIViewClientResource rhs = (MCRIViewClientResource) obj;
            return new EqualsBuilder().append(type, rhs.type).append(url, rhs.url).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 31).append(type.ordinal()).append(url).toHashCode();
        }

    }

    @XmlRootElement(name = "property")
    private static class MCRIViewClientProperty {

        @XmlAttribute(name = "name", required = true)
        public String name;

        @XmlTransient
        public Object value;

        @XmlValue
        public String getXMLValue() {
            return (value == null) ? "" : value.toString();
        }

        @SuppressWarnings("unused")
        MCRIViewClientProperty() {
        }

        MCRIViewClientProperty(String name, Object value) {
            this.name = name;
            this.value = value;
        }

    }

    private static final class MultimapSerializer implements JsonSerializer<Multimap<ResourceType, String>> {

        @SuppressWarnings("serial")
        private static final Type T = new TypeToken<Map<ResourceType, Collection<String>>>() {
        }.getType();

        @Override
        public JsonElement serialize(Multimap<ResourceType, String> arg0, Type arg1, JsonSerializationContext arg2) {
            return arg2.serialize(arg0.asMap(), T);
        }

    }

}
