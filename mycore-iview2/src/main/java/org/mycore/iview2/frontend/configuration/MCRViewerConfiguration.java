package org.mycore.iview2.frontend.configuration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRXMLContent;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iview2.services.MCRIView2Tools;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Base class for the iview client configuration. You can add properties, javascript files and css files.
 * To retrieve the configuration in xml call {@link #toXML()}. To get it in json call {@link #toJSON()}. 
 * 
 * @author Matthias Eichner
 */
public class MCRViewerConfiguration {

    public static enum ResourceType {
        script, css
    }

    private Multimap<ResourceType, String> resources;

    private Map<String, Object> properties;

    private static boolean DEBUG_MODE;

    static {
        DEBUG_MODE = Boolean.parseBoolean(MCRIView2Tools.getIView2Property("DeveloperMode", "false"));
    }

    public MCRViewerConfiguration() {
        resources = LinkedListMultimap.create();
        properties = new HashMap<>();
    }

    /**
     * Returns the properties of this configuration.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Returns a multimap containing all resources (javascript and css url's).
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
     * return true if iview2.debug=true
     */
    protected boolean isDebugParameterSet(HttpServletRequest request) {
        return Boolean.TRUE.toString().toLowerCase(Locale.ROOT).equals(request.getParameter("iview2.debug"));
    }

    /**
     * Returns true if the debug/developer mode is active.
     */
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    /**
     * Helper method to get the derivate id of the given request.
     * 
     * @param request http request
     * @return the derivate id embedded in the path of the request
     */
    public static String getDerivate(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        int i = requestURI.indexOf("_derivate_");
        if (i == -1) {
            return null;
        }
        return requestURI.substring(requestURI.lastIndexOf("/", i) + 1, i + 18);
    }

    /**
     * Helper method to get the path to the start file. The path is not
     * url encoded.
     * 
     * @param request http request
     * @return path to the file
     */
    public static String getFilePath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        int i = pathInfo.indexOf("_derivate_");
        if (i == -1) {
            return null;
        }
        return pathInfo.substring(i + 18);
    }

    /**
     * Adds a new javascript file which should be included by the image viewer.
     */
    public void addScript(final String url) {
        this.resources.put(ResourceType.script, url);
    }

    /**
     * Shorthand MCRViewerConfiguration#addLocalScript(String, true)
     */
    public void addLocalScript(final String file) {
        this.addLocalScript(file, true);
    }

    /**
     * Adds a local (based in modules/iview2/js/) javascript file which should be included
     * by the image viewer. This method takes care of the debug mode. You should always
     * call this method with the base file version. E.g. addLocalScript("iview-client-mobile.js");.
     * When debug mode is activated, this method injects a ".min" by default.
     * So the included file will look like "iview-client-mobile.min.js".
     * 
     * @param file to include
     * @param hasMinified only uses the minified version if true
     */
    public void addLocalScript(final String file, final boolean hasMinified) {
        String baseURL = MCRFrontendUtil.getBaseURL();
        StringBuffer scriptURL = new StringBuffer(baseURL);
        scriptURL.append("modules/iview2/js/");
        if (isDebugMode() || !hasMinified) {
            scriptURL.append(file);
        } else {
            scriptURL.append(file.substring(0, file.lastIndexOf(".")));
            scriptURL.append(".min.js");
        }
        addScript(scriptURL.toString());
    }

    /**
     * Adds a new css file which should be included by the image viewer.
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
        StringBuffer cssURL = new StringBuffer(baseURL);
        cssURL.append("modules/iview2/css/").append(file);
        addCSS(cssURL.toString());
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
     * @return json
     */
    public String toJSON() {
        final Gson gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapSerializer()).create();
        return gson.toJson(this);
    }

    /**
     * Returns the configuration in xml content format.
     */
    public MCRXMLContent toXML() throws JAXBException {
        MCRIViewClientXMLConfiguration xmlConfig = new MCRIViewClientXMLConfiguration(resources, properties);
        MCRJAXBContent<MCRIViewClientXMLConfiguration> config = new MCRJAXBContent<MCRIViewClientXMLConfiguration>(
            JAXBContext.newInstance(xmlConfig.getClass()), xmlConfig);
        return config;
    }

    @XmlRootElement(name = "xml")
    private static class MCRIViewClientXMLConfiguration {

        private Multimap<ResourceType, String> resources;

        private Map<String, Object> properties;

        @SuppressWarnings("unused")
        public MCRIViewClientXMLConfiguration() {
        }

        public MCRIViewClientXMLConfiguration(Multimap<ResourceType, String> resources, Map<String, Object> properties) {
            this.resources = resources;
            this.properties = properties;
        }

        @XmlElements({ @XmlElement(name = "resource") })
        @XmlElementWrapper(name = "resources")
        public final List<MCRIViewClientResource> getResources() {
            List<MCRIViewClientResource> resourceList = new ArrayList<>();
            for (Entry<ResourceType, String> entry : resources.entries()) {
                resourceList.add(new MCRIViewClientResource(entry.getKey(), entry.getValue()));
            }
            return resourceList;
        }

        @XmlElements({ @XmlElement(name = "property") })
        @XmlElementWrapper(name = "properties")
        public final List<MCRIViewClientProperty> getProperties() {
            List<MCRIViewClientProperty> propertyList = new ArrayList<>();
            for (Entry<String, Object> entry : properties.entrySet()) {
                propertyList.add(new MCRIViewClientProperty(entry.getKey(), entry.getValue()));
            }
            return propertyList;
        }

    }

    @XmlRootElement(name = "resource")
    private static class MCRIViewClientResource {

        @XmlAttribute(name = "type", required = true)
        public ResourceType type;

        @XmlValue
        public String url;

        @SuppressWarnings("unused")
        public MCRIViewClientResource() {
        }

        public MCRIViewClientResource(ResourceType type, String url) {
            this.type = type;
            this.url = url;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MCRIViewClientResource))
                return false;
            if (obj == this)
                return true;
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
        public MCRIViewClientProperty() {
        }

        public MCRIViewClientProperty(String name, Object value) {
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
