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

package org.mycore.frontend.jersey;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.events.MCRStartupHandler;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class MCRJWTUtil implements MCRStartupHandler.AutoExecutable {
    @Override
    public String getName() {
        return "JSON WebToken Services";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            File sharedSecretFile = MCRConfigurationDir.getConfigFile("jwt.secret");
            byte[] secret;
            if (!sharedSecretFile.isFile()) {
                secret = new byte[4096];
                try {
                    SecureRandom.getInstanceStrong().nextBytes(secret);
                    Files.write(sharedSecretFile.toPath(), secret, StandardOpenOption.CREATE_NEW);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new MCRConfigurationException(
                        "Could not create shared secret in file: " + sharedSecretFile.getAbsolutePath(), e);
                }
            } else {
                try {
                    secret = Files.readAllBytes(sharedSecretFile.toPath());
                } catch (IOException e) {
                    throw new MCRConfigurationException(
                        "Could not create shared secret in file: " + sharedSecretFile.getAbsolutePath(), e);
                }
            }
            SHARED_SECRET = Algorithm.HMAC512(secret);
        }
    }

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    public static final String JWT_CLAIM_ROLES = "mcr:roles";

    public static final String JWT_CLAIM_IP = "mcr:ip";

    private static final String ROLES_PROPERTY = "MCR.Rest.JWT.Roles";

    private static Algorithm SHARED_SECRET;

    public static JWTCreator.Builder getJWTBuilder(MCRUserInformation userInformation) {
        String[] roles = MCRConfiguration2.getOrThrow(ROLES_PROPERTY, MCRConfiguration2::splitValue)
            .filter(userInformation::isUserInRole)
            .toArray(String[]::new);
        String subject = userInformation.getUserID();
        String email = userInformation.getUserAttribute(MCRUserInformation.ATT_EMAIL);
        String name = userInformation.getUserAttribute(MCRUserInformation.ATT_REAL_NAME);
        return JWT.create()
            .withIssuedAt(new Date())
            .withSubject(subject)
            .withArrayClaim("mcr:roles", roles)
            .withClaim("email", email)
            .withClaim("name", name);
    }

    public static Algorithm getJWTAlgorithm() {
        return SHARED_SECRET;
    }

    public static Response getJWTLoginSuccessResponse(String jwt) throws IOException {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator jsonGenerator = JSON_FACTORY.createGenerator(sw);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBooleanField("login_success", true);
            jsonGenerator.writeStringField("access_token", jwt);
            jsonGenerator.writeStringField("token_type", "Bearer");
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
            jsonGenerator.close();
            return Response.status(Response.Status.OK)
                .header("Authorization", "Bearer " + jwt)
                .entity(sw.toString())
                .build();
        }
    }

    public static Response getJWTRenewSuccessResponse(String jwt) throws IOException {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator jsonGenerator = JSON_FACTORY.createGenerator(sw);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBooleanField("executed", true);
            jsonGenerator.writeStringField("access_token", jwt);
            jsonGenerator.writeStringField("token_type", "Bearer");
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
            jsonGenerator.close();
            return Response.status(Response.Status.OK)
                .header("Authorization", "Bearer " + jwt)
                .entity(sw.toString())
                .build();
        }
    }

    public static Response getJWTLoginErrorResponse(String errorDescription) throws IOException {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator jsonGenerator = JSON_FACTORY.createGenerator(sw);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeBooleanField("login_success", false);
            jsonGenerator.writeStringField("error", "login_failed");
            jsonGenerator.writeStringField("error_description", errorDescription);
            jsonGenerator.writeEndObject();
            jsonGenerator.flush();
            jsonGenerator.close();
            return Response.status(Response.Status.FORBIDDEN)
                .entity(sw.toString())
                .build();
        }
    }

}
