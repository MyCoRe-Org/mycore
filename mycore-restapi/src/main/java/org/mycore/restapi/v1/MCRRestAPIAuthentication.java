/*
 * $Revision: 29635 $ $Date: 2014-04-10 10:55:06 +0200 (Do, 10 Apr 2014) $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.restapi.v1;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.jersey.MCRStaticContent;
import org.mycore.restapi.v1.utils.MCRJSONWebTokenUtil;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;

/**
 * Rest Controller that handles authentication.
 * 
 * @author Robert Stephan
 *
 */
@Path("/v1/auth")
@MCRStaticContent
public class MCRRestAPIAuthentication {

    /**
     * return the server public key as Java Web Token
     * 
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    public Response initAuthorization() {
        String jwt = MCRJSONWebTokenUtil.createEmptyJWTwithPublicKey("http:/localhost:8080");
        StringBuffer msg = new StringBuffer();
        msg.append("{");
        msg.append("\n    \"access_token\": \"" + jwt + "\",");
        msg.append("\n}");

        return Response.ok(msg.toString()).type("application/json; charset=UTF-8")
            .header("Authorization", "Bearer " + jwt).build();
    }

    /**
     * Validation: https://jwt.io/ Public Key: http://localhost:8080/api/v1/auth/public_key.txt
     *
     * Unauthenticated requests should return a response whose header contains a HTTP 401 Unauthorized status and a
     * WWW-Authenticate field.
     * 
     * 200 OK Content-Type: application/json;charset=UTF-8
     * 
     * { "access_token": "NgCXRK...MzYjw", "token_type": "Bearer", "expires_at": 1372700873, "refresh_token":
     * "NgAagA...Um_SHo" }
     * 
     * Returning the JWT (Java Web Token to the client is not properly specified). We use the "Authorization" Header in
     * the response, which is unusual but not strictly forbidden.
     * 
     * @param authorization
     * @return
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON + ";charset=UTF-8" })
    @Path("/login")
    public Response authorize(@DefaultValue("") @HeaderParam("Authorization") String authorization) {
        String username = null;
        String password = null;
        JWK clientPubKey = null;
        String userPwd = null;
        if (authorization.startsWith("Basic ")) {
            byte[] encodedAuth = authorization.substring(6).trim().getBytes(StandardCharsets.ISO_8859_1);
            userPwd = new String(Base64.getDecoder().decode(encodedAuth), StandardCharsets.ISO_8859_1);

        }
        if (authorization.startsWith("Bearer ")) {
            userPwd = MCRJSONWebTokenUtil.retrieveUsernamePasswordFromLoginToken(authorization.substring(7).trim());
            clientPubKey = MCRJSONWebTokenUtil.retrievePublicKeyFromLoginToken(authorization.substring(7).trim());
        }

        if (userPwd != null && userPwd.contains(":")) {
            int splitPos = userPwd.indexOf(":");
            username = userPwd.substring(0, splitPos);
            password = userPwd.substring(splitPos + 1);
        }
        if (username != null && password != null) {

            String jwt = validateUser(username, password, clientPubKey);
            if (jwt != null) {
                StringBuffer msg = new StringBuffer();
                msg.append("{");
                msg.append("\n    \"login_successful\":true,");
                msg.append("\n    \"access_token\": \"" + jwt + "\",");
                msg.append("\n    \"token_type\": \"Bearer\"");
                msg.append("\n}");

                return Response.ok(msg.toString()).type("application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + jwt).build();
            }
        }

        StringBuffer msg = new StringBuffer();
        msg.append("{");
        msg.append("\n    \"login_successful\":false,");
        msg.append("\n    \"error\": \"login_failed\"");
        msg.append(
            "\n    \"error_description\": \"Login failed. Please provider proper user name and password via HTTP Basic Authentication.\"");
        msg.append("\n}");

        return Response.status(Status.FORBIDDEN).header("WWW-Authenticate", "Basic realm=\"MyCoRe REST API\"")
            .entity(msg.toString()).type("application/json; charset=UTF-8").build();
    }

    @GET
    @Path("/public_key.jwk")
    public Response sendPublicKey() {
        JWK jwk = new RSAKey.Builder((RSAPublicKey) MCRJSONWebTokenUtil.RSA_KEYS.getPublic()).build();
        return Response.ok(jwk.toJSONString()).type("application/json; charset=UTF-8").build();
    }

    @GET
    @Path("/api/v1/auth/public_key.txt")
    public Response sendPublicKeyasText() {
        String txt = "-----BEGIN PUBLIC KEY-----\n"
            + Base64.getEncoder().encodeToString(MCRJSONWebTokenUtil.RSA_KEYS.getPublic().getEncoded())
            + "\n-----END PUBLIC KEY-----";
       return Response.ok(txt).type("text/plain; charset=UTF-8").build();
    }
    
    private String validateUser(String username, String password, JWK clientPubKey) {
        MCRUser user = MCRUserManager.checkPassword(username, password);
        if (user!=null) {
            return MCRJSONWebTokenUtil.createJWT(username, Arrays.asList("rest-api"), "http://localhost:8080/",
                clientPubKey);
        }
        return null;
    }

    //TODO validate Token (Signatur und Zeitliche Gültigkeit)
    private boolean validateToken(String token) {
        JWTClaimsSetVerifier claimsVerifier = new DefaultJWTClaimsVerifier<SecurityContext>();

        return true;
    }

    @POST
    @Path("/renew")
    public Response renew(@DefaultValue("") String data,
        @DefaultValue("")@HeaderParam("Authorization") String authorization) {
        if (authorization.startsWith("Bearer ")) {
            String authToken = authorization.substring(7).trim();
            try {
                JWSObject jwsObj = JWSObject.parse(authToken);
                SignedJWT signedJWT = jwsObj.getPayload().toSignedJWT();
                // JWK class does equals only by object id
                if (signedJWT.verify(new RSASSAVerifier((RSAPublicKey) MCRJSONWebTokenUtil.RSA_KEYS.getPublic()))
                    && jwsObj.getHeader().getJWK().toJSONString()
                        .equals(JWK.parse(signedJWT.getJWTClaimsSet().getJSONObjectClaim("sub_jwk")).toJSONString())) {

                    String submittedUser = MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT);

                    JWK clientPubKey = MCRJSONWebTokenUtil.retrievePublicKeyFromAuthenticationToken(signedJWT);
                    if (submittedUser != null && clientPubKey != null) {
                        String jwt = MCRJSONWebTokenUtil.createJWT(submittedUser, Arrays.asList("rest-api"),
                            MCRFrontendUtil.getBaseURL(), clientPubKey);
                        if (jwt != null) {
                            StringBuffer msg = new StringBuffer();
                            msg.append("{");
                            msg.append("\n    \"executed\":true,");
                            msg.append("\n    \"access_token\": \"" + jwt + "\",");
                            msg.append("\n    \"token_type\": \"Bearer\",");
                            msg.append("\n    \"data\": \"" + data + "\",");

                            msg.append("\n}");

                            return Response.ok(msg.toString()).type("application/json; charset=UTF-8").build();
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return Response.status(Status.FORBIDDEN).entity("Session cannot be renewed!").type("text/plain; charset=UTF-8").build();
        }

        StringBuffer msg = new StringBuffer();
        msg.append("{");
        msg.append("\n    \"executed\":false,");
        msg.append("\n    \"error\": \"permission_denied\"");
        msg.append("\n    \"error_description\": \"Please provide a valid JWT Token for the session.\"");
        msg.append("\n}");
        
        return Response.status(Status.FORBIDDEN).entity(msg.toString()).type("application/json; charset=UTF-8").build();
    }

}