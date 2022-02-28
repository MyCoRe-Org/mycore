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

import org.mycore.common.MCRCoreVersion;
import org.mycore.common.config.MCRConfiguration2;
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

    public MCRRestV2App() {
        super();
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
        setupOAS();
    }

    @Override
    protected String getVersion() {
        return "v2";
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
}
