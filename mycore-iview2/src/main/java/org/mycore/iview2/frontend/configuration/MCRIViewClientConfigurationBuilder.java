package org.mycore.iview2.frontend.configuration;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.mycore.iview2.frontend.configuration.MCRIViewClientConfiguration.ResourceType;

import com.google.common.collect.Multimap;

/**
 * Use this class to build your {@link MCRIViewClientConfiguration}.
 * You can use {@link #mets(HttpServletRequest)} or {@link #pdf(HttpServletRequest)}
 * as entry point and use {@link #mixin(MCRIViewClientConfiguration)} to append
 * additional configuration.
 * 
 * @author Matthias Eichner
 */
public class MCRIViewClientConfigurationBuilder {

    private MCRIViewClientConfiguration internalConfig;

    private HttpServletRequest request;

    private MCRIViewClientConfigurationBuilder(HttpServletRequest request) {
        this.request = request;
        this.internalConfig = new MCRIViewClientConfiguration();
    }

    /**
     * Mix in the configuration into the builder. This overwrites properties and
     * resources with the same name.
     * 
     * @param configuration the configuration to mix in.
     * @return same instance
     */
    public MCRIViewClientConfigurationBuilder mixin(MCRIViewClientConfiguration configuration) {
        configuration.setup(request);
        mixin(internalConfig, configuration);
        return this;
    }

    /**
     * Gets the configuration.
     * 
     * @return
     */
    public MCRIViewClientConfiguration get() {
        return internalConfig;
    }

    /**
     * Mix in the second configuration into the first.
     * 
     * @param conf1
     * @param conf2
     */
    public static void mixin(MCRIViewClientConfiguration conf1, MCRIViewClientConfiguration conf2) {
        Map<String, Object> conf2Props = conf2.getProperties();
        for (Map.Entry<String, Object> property : conf2Props.entrySet()) {
            conf1.setProperty(property.getKey(), property.getValue());
        }
        Multimap<ResourceType, String> resources = conf2.getResources();
        for (Map.Entry<ResourceType, String> resource : resources.entries()) {
            if (ResourceType.script.equals(resource.getKey())) {
                conf1.addScript(resource.getValue());
            } else if (ResourceType.css.equals(resource.getKey())) {
                conf1.addCSS(resource.getValue());
            }
        }
    }

    /**
     * Creates a new configuration builder.
     * 
     * @param request the servlet request
     * @return a new configuration builder instance.
     */
    public static MCRIViewClientConfigurationBuilder build(HttpServletRequest request) {
        MCRIViewClientConfigurationBuilder builder = new MCRIViewClientConfigurationBuilder(request);
        return builder;
    }

    /**
     * Builds the default mets configuration without any plugins.
     * 
     * @param request
     * @return
     */
    public static MCRIViewClientConfigurationBuilder mets(HttpServletRequest request) {
        MCRIViewClientMetsConfiguration metsConfig = new MCRIViewClientMetsConfiguration();
        return MCRIViewClientConfigurationBuilder.build(request).mixin(metsConfig);
    }

    /**
     * Builds the mets configuration with the metadata, piwik and logo plugin.
     * 
     * @param request
     * @return
     */
    public static MCRIViewClientConfigurationBuilder metsAndPlugins(HttpServletRequest request) {
        return mets(request).mixin(plugins(request).get());
    }

    /**
     * Builds the default pdf configuration without any plugins.
     * 
     * @param request
     * @return
     */
    public static MCRIViewClientConfigurationBuilder pdf(HttpServletRequest request) {
        MCRIViewClientPDFConfiguration pdfConfig = new MCRIViewClientPDFConfiguration();
        return MCRIViewClientConfigurationBuilder.build(request).mixin(pdfConfig);
    }

    /**
     * Builds just the plugins (metadata, piwik, logo).
     * 
     * @param request
     * @return
     */
    public static MCRIViewClientConfigurationBuilder plugins(HttpServletRequest request) {
        return MCRIViewClientConfigurationBuilder.build(request).mixin(new MCRIViewClientLogoConfiguration())
            .mixin(new MCRIViewClientMetadataConfiguration()).mixin(new MCRIViewClientPiwikConfiguration());
    }

}
