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

package org.mycore.restapi.v2;

import java.net.URI;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.InternalServerErrorException;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRJerseyDefaultConfiguration;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFactory;
import org.mycore.frontend.jersey.resources.MCRJerseyExceptionMapper;
import org.mycore.restapi.MCRCORSResponseFilter;
import org.mycore.restapi.MCRRestFeature;
import org.mycore.restapi.MCRSessionFilter;
import org.mycore.restapi.MCRTransactionFilter;
import org.mycore.restapi.converter.MCRWrappedXMLWriter;
import org.mycore.restapi.v1.MCRRestAPIAuthentication;
import org.mycore.restapi.v1.errors.MCRForbiddenExceptionMapper;
import org.mycore.restapi.v1.errors.MCRNotAuthorizedExceptionMapper;
import org.mycore.restapi.v1.errors.MCRRestAPIExceptionMapper;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@ApplicationPath("/api/v2")
public class MCRRestV2App extends ResourceConfig {

    public MCRRestV2App() {
        super();
        setApplicationName(MCRConfiguration2.getString("MCR.NameOfProject").orElse("MyCoRe") + " REST-API v2");
        LogManager.getLogger().error("Initiialize {}", getApplicationName());
        MCRJerseyDefaultConfiguration.setupGuiceBridge(this);
        String[] restPackages = Stream
            .concat(
                Stream.of(MCRWrappedXMLWriter.class.getPackage().getName(),
                    OpenApiResource.class.getPackage().getName()),
                MCRConfiguration.instance().getStrings("MCR.RestAPI.V2.Resource.Packages").stream())
            .toArray(String[]::new);
        packages(restPackages);
        register(MCRRestAPIAuthentication.class); //keep 'unchanged' in v2
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        register(MCRSessionFilter.class);
        register(MCRTransactionFilter.class);
        register(MultiPartFeature.class);
        register(MCRRestFeature.class);
        register(MCRJerseyExceptionMapper.class);
        register(MCRRestAPIExceptionMapper.class);
        register(MCRForbiddenExceptionMapper.class);
        register(MCRNotAuthorizedExceptionMapper.class);
        register(MCRCORSResponseFilter.class);
        register(MCRRequestScopeACLFactory.getBinder());
        getInstances().stream()
            .forEach(LogManager.getLogger()::info);
        setupOAS(restPackages);
    }

    protected void setupOAS(String[] restPackages) {
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
            .resourcePackages(Stream.of(restPackages).collect(Collectors.toSet()))
            .ignoredRoutes(
                MCRConfiguration2.getString("MCR.RestAPI.V2.OpenAPI.IgnoredRoutes")
                    .map(MCRConfiguration2::splitValue)
                    .orElseGet(Stream::empty)
                    .collect(Collectors.toSet()))
            .prettyPrint(true);
        try {
            OpenApiContext oasContext = new JaxrsOpenApiContextBuilder()
                .application(getApplication())
                .openApiConfiguration(oasConfig)
                .buildContext(true);
        } catch (OpenApiConfigurationException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
