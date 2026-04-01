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

package org.mycore.restapi.v2;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.restapi.MCRApiDraftFilter;
import org.mycore.restapi.MCRContentNegotiationViaExtensionFilter;
import org.mycore.restapi.MCRDropSessionFilter;
import org.mycore.restapi.MCRJerseyRestApp;
import org.mycore.restapi.MCRNoFormDataPutFilter;
import org.mycore.restapi.MCRNormalizeMCRObjectIDsFilter;
import org.mycore.restapi.MCRRemoveMsgBodyFilter;
import org.mycore.restapi.converter.MCRWrappedXMLWriter;
import org.mycore.restapi.v1.MCRRestAPIAuthentication;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.InternalServerErrorException;

@ApplicationPath("/api/v2")
public class MCRRestV2App extends MCRJerseyRestApp {

    private final BindingsResolver serviceBindingsResolver;

    public MCRRestV2App() {
        super();

        serviceBindingsResolver = MCRConfiguration2.getInstanceOfOrThrow(BindingsResolver.class,
            "MCR.RestAPI.V2.Services.Class");

        register(MCRContentNegotiationViaExtensionFilter.class);
        register(MCRNormalizeMCRObjectIDsFilter.class);
        register(MCRRestAPIAuthentication.class); // keep 'unchanged' in v2
        register(MCRRemoveMsgBodyFilter.class);
        register(MCRNoFormDataPutFilter.class);
        register(MCRDropSessionFilter.class);
        register(MCRExceptionMapper.class);
        register(MCRApiDraftFilter.class);
        //after removing the following line, test if json output from MCRRestClassification is still OK
        register(JacksonXmlBindJsonProvider.class); //jetty >= 2.31, do not use DefaultJacksonJaxbJsonProvider
        registerServices();
        setupOAS();
    }

    @Override
    protected String getVersion() {
        return "v2";
    }

    private void registerServices() {
        Collection<Binding> bindings = this.serviceBindingsResolver.resolve();
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindings.forEach(binding -> bind(binding.implementationClass).to(binding.apiClass));
            }
        });
    }

    @Override
    protected String[] getRestPackages() {
        return Stream
            .concat(
                Stream.of(MCRWrappedXMLWriter.class.getPackage().getName(),
                    OpenApiResource.class.getPackage().getName()),
                MCRConfiguration2.getOrThrow("MCR.RestAPI.V2.Resource.Packages", MCRConfiguration2::splitValue))
            .toArray(String[]::new);
    }

    private void setupOAS() {
        OpenAPI oas = new OpenAPI();
        Info oasInfo = new Info();
        oas.setInfo(oasInfo);
        oasInfo.setVersion(MCRCoreVersion.getVersion());
        oasInfo.setTitle(getApplicationName());
        License oasLicense = new License();
        oasLicense.setName("GNU General Public License, version 3");
        oasLicense.setUrl("http://www.gnu.org/licenses/gpl-3.0.txt");
        oasInfo.setLicense(oasLicense);
        URI baseURI = URI.create(MCRFrontendUtil.getBaseURL());
        Server oasServer = new Server();
        oasServer.setUrl(baseURI.resolve("api").toString());
        oas.addServersItem(oasServer);
        SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .resourcePackages(Stream.of(getRestPackages()).collect(Collectors.toSet()))
            .ignoredRoutes(
                MCRConfiguration2.getString("MCR.RestAPI.V2.OpenAPI.IgnoredRoutes")
                    .map(MCRConfiguration2::splitValue)
                    .orElseGet(Stream::empty)
                    .collect(Collectors.toSet()))
            .prettyPrint(true);
        try {
            new JaxrsOpenApiContextBuilder()
                .application(getApplication())
                .openApiConfiguration(oasConfig)
                .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @MCRConfigurationProxy(proxyClass = Binding.Factory.class)
    public record Binding(Class<?> implementationClass, Class<?> apiClass) {

        public static final class Factory implements Supplier<Binding> {

            @MCRProperty(name = "From")
            public String implementationClass;

            @MCRProperty(name = "To")
            public String apiClass;

            private String property;

            @MCRPostConstruction(MCRPostConstruction.Value.CANONICAL)
            public void init(String property) {
                this.property = property;
            }

            @Override
            public Binding get() {
                Class<?> implementationClass = toClass(this.implementationClass, "From");
                Class<?> apiClass = toClass(this.apiClass, "To");
                if (!apiClass.isAssignableFrom(implementationClass)) {
                    throw new IllegalArgumentException("Implementation " + this.implementationClass + " configured in "
                        + property + ".From does not implement/extend " + this.apiClass + " configured in "
                        + property + ".To");
                }
                return new Binding(implementationClass, apiClass);
            }

            private Class<?> toClass(String className, String key) {
                try {
                    return MCRClassTools.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new MCRConfigurationException("Cannot load class " + className + "configured in "
                        + property + "." + key, e);
                }
            }

        }

    }

    /**
     * Scans entries like:
     * <ul>
     *   <li>MCR.RestAPI.V2.Services.ObjectLockService.From = <b>impl</b></li>
     *   <li>MCR.RestAPI.V2.Services.ObjectLockService.To   = <b>api</b></li>
     * </ul>
     * <p>
     * and returns a map of From -> To classes for DI registration.
     */
    public static final class BindingsResolver {

        @MCRInstanceMap(valueClass = Binding.class)
        public Map<String, Binding> bindings;

        public Collection<Binding> resolve() {
            return bindings.values();
        }

    }

}
