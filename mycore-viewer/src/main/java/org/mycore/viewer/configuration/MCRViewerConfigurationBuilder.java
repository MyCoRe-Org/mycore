package org.mycore.viewer.configuration;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.mycore.viewer.configuration.MCRViewerConfiguration.ResourceType;

import com.google.common.collect.Multimap;

/**
 * Use this class to build your {@link MCRViewerConfiguration}.
 * You can use {@link #mets(HttpServletRequest)} or {@link #pdf(HttpServletRequest)}
 * as entry point and use {@link #mixin(MCRViewerConfiguration)} to append
 * additional configuration.
 * 
 * @author Matthias Eichner
 */
public class MCRViewerConfigurationBuilder {

    private MCRViewerConfiguration internalConfig;

    private HttpServletRequest request;

    private MCRViewerConfigurationBuilder(HttpServletRequest request) {
        this.request = request;
        this.internalConfig = new MCRViewerConfiguration();
    }

    /**
     * Mix in the configuration into the builder. This overwrites properties and
     * resources with the same name.
     * 
     * @param configuration the configuration to mix in.
     * @return same instance
     */
    public MCRViewerConfigurationBuilder mixin(MCRViewerConfiguration configuration) {
        configuration.setup(request);
        mixin(internalConfig, configuration);
        return this;
    }

    /**
     * Gets the configuration.
     */
    public MCRViewerConfiguration get() {
        return internalConfig;
    }

    /**
     * Mix in the second configuration into the first.
     */
    public static void mixin(MCRViewerConfiguration conf1, MCRViewerConfiguration conf2) {
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
    public static MCRViewerConfigurationBuilder build(HttpServletRequest request) {
        MCRViewerConfigurationBuilder builder = new MCRViewerConfigurationBuilder(request);
        return builder;
    }

    /**
     * Builds the default mets configuration without any plugins.
     */
    public static MCRViewerConfigurationBuilder mets(HttpServletRequest request) {
        MCRViewerMetsConfiguration metsConfig = new MCRViewerMetsConfiguration();
        return MCRViewerConfigurationBuilder.build(request).mixin(metsConfig);
    }

    /**
     * Builds the mets configuration with the metadata, piwik and logo plugin.
     */
    public static MCRViewerConfigurationBuilder metsAndPlugins(HttpServletRequest request) {
        return mets(request).mixin(plugins(request).get());
    }

    /**
     * Builds the default pdf configuration without any plugins.
     */
    public static MCRViewerConfigurationBuilder pdf(HttpServletRequest request) {
        MCRViewerPDFConfiguration pdfConfig = new MCRViewerPDFConfiguration();
        return MCRViewerConfigurationBuilder.build(request).mixin(pdfConfig);
    }

    /**
     * Builds just the plugins (metadata, piwik, logo).
     */
    public static MCRViewerConfigurationBuilder plugins(HttpServletRequest request) {
        return MCRViewerConfigurationBuilder.build(request).mixin(new MCRViewerLogoConfiguration())
            .mixin(new MCRViewerMetadataConfiguration()).mixin(new MCRViewerPiwikConfiguration());
    }

}
