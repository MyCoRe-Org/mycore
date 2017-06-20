package org.mycore.restapi.v1;

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.mycore.common.MCRCoreVersion;
import org.mycore.frontend.jersey.MCRStaticContent;

@Path("/v1/mycore")
@MCRStaticContent
public class MCRInfo {

    @GET
    @Path("version")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Properties getGitInfos() {
        Properties properties = new Properties();
        properties.putAll(MCRCoreVersion.getVersionProperties());
        return properties;
    }
}
