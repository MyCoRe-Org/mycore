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

package org.mycore.restapi.v1.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.restapi.v1.errors.MCRRestAPIError;
import org.mycore.restapi.v1.errors.MCRRestAPIException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Utility functions for handling JSON Web Tokens.
 * 
 * @author Robert Stephan
 * 
 * @version $Revision: $ $Date: $
 *
 */
public class MCRJSONWebTokenUtil {
    public static KeyPair RSA_KEYS = null;

    private static Logger LOGGER = LogManager.getLogger(MCRJSONWebTokenUtil.class);

    public static int EXPIRATION_TIME_MINUTES = 10;

    static {
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);

            RSA_KEYS = keyGenerator.genKeyPair();
        } catch (Exception e) {
            // do nothing
        }
    }

    /** 
     * retrieves username and password from JSON web tocken 
     * 
     * @param token - the serialized JSON web token from login
     * @return username and password (combined by ":")
     */
    public static String retrieveUsernamePasswordFromLoginToken(String token) {
        JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(token);

            // Decrypt with shared key
            jweObject.decrypt(new RSADecrypter(RSA_KEYS.getPrivate()));

            // Extract payload
            SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();
            RSAKey serverPublicKey = RSAKey.parse(signedJWT.getHeader().getJWK().toJSONObject());

            if (signedJWT.verify(new RSASSAVerifier(serverPublicKey))) {
                //Token is valid
                String username = signedJWT.getJWTClaimsSet().getSubject();
                String password = signedJWT.getJWTClaimsSet().getStringClaim("password");
                return username + ":" + password;
            }
        } catch (ParseException | JOSEException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /**
     * retrieves the client public key from Login Token
     * 
     * @param token - the serialized JSON Web Token from login
     * @return the public key as JWK object
     */
    public static JWK retrievePublicKeyFromLoginToken(String token) {
        JWK result = null;
        JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(token);

            // Decrypt with shared key
            jweObject.decrypt(new RSADecrypter(RSA_KEYS.getPrivate()));

            // Extract payload
            SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();

            result = signedJWT.getHeader().getJWK();

            RSAKey publicKey = RSAKey.parse(result.toJSONObject());
            if (signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return result;
            }
        } catch (ParseException | JOSEException e) {
            LOGGER.error(e);
        }
        return null;
    }

    /**
     * retrieves the client public key from Authentication Token
     * 
     * @param signedJWT - the authentication token
     * @return the public key as JWK object
     */
    public static JWK retrievePublicKeyFromAuthenticationToken(SignedJWT signedJWT) {
        JWK result = null;
        try {
            result = JWK.parse(signedJWT.getJWTClaimsSet().getJSONObjectClaim("sub_jwk"));
            RSAKey publicKey = (RSAKey) signedJWT.getHeader().getJWK();
            if (signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return result;
            }
        } catch (ParseException | JOSEException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /**
     * retrieves the username from Authentication Token
     * 
     * @param signedJWT - the authentication token
     * @return the user name
     */
    public static String retrieveUsernameFromAuthenticationToken(SignedJWT signedJWT) {
        try {
            // Extract payload

            RSAKey serverPublicKey = RSAKey.parse(signedJWT.getHeader().getJWK().toJSONObject());
            if (signedJWT.verify(new RSASSAVerifier(serverPublicKey))) {
                //Token is valid
                return signedJWT.getJWTClaimsSet().getSubject();
            }
        } catch (ParseException | JOSEException e) {
            LOGGER.error(e);
        }
        return MCRSystemUserInformation.getGuestInstance().getUserID();
    }

    /**
     * retrieves the username from Authentication Token
     * 
     * @param request the HTTPServletRequest object
     * 
     * @return the user name
     * @throws MCRRestAPIException
     */
    public static String retrieveUsernameFromAuthenticationToken(HttpServletRequest request)
        throws MCRRestAPIException {
        try {
            SignedJWT signedJWT = retrieveAuthenticationToken(request);
            return MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(signedJWT);
        } catch (Exception e) {
            return MCRSystemUserInformation.getGuestInstance().getUserID();
        }
    }

    /**
     * creates an empty JSON Web Token
     * 
     * @param webAppBaseURL - the base url of the application
     * 
     * @return the JSON WebToken
     */
    public static SignedJWT createEmptyJWTwithPublicKey(String webAppBaseURL) {

        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(webAppBaseURL).jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(currentTime.toInstant())).build();
        String keyID = UUID.randomUUID().toString();
        JWK jwk = new RSAKey.Builder((RSAPublicKey) RSA_KEYS.getPublic()).keyID(keyID).build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).jwk(jwk).build();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
        try {
            signedJWT.sign(new RSASSASigner(RSA_KEYS.getPrivate()));
        } catch (JOSEException e) {
            LOGGER.error(e);
        }
        return signedJWT;

    }

    /**
     * creates a JSON Web Token with user id, roles and client public key
     * 
     * @param user - the user that should be returned
     * @param roles - the roles that should be returned
     * @param webAppBaseURL - the base url of the application
     * @param clientPublicKey -  the client public key as JSON Web Key
     * 
     * @return the JSON WebToken
     */
    public static SignedJWT createJWT(String user, List<String> roles, String webAppBaseURL, JWK clientPublicKey) {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(webAppBaseURL).jwtID(UUID.randomUUID().toString())
            .expirationTime(Date.from(currentTime.plusMinutes(EXPIRATION_TIME_MINUTES).toInstant()))
            .issueTime(Date.from(currentTime.toInstant()))
            .notBeforeTime(Date.from(currentTime.minusMinutes(EXPIRATION_TIME_MINUTES).toInstant())).subject(user)
            // additional claims/attributes about the subject can be added
            // claims.setClaim("email", "mail@example.com");
            // multi-valued claims work too and will end up as a JSON array
            .claim("roles", roles).claim("sub_jwk", clientPublicKey).build();

        String keyID = UUID.randomUUID().toString();
        JWK jwk = new RSAKey.Builder((RSAPublicKey) RSA_KEYS.getPublic()).keyID(keyID).build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).jwk(jwk).build();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
        try {
            signedJWT.sign(new RSASSASigner(RSA_KEYS.getPrivate()));
        } catch (JOSEException e) {
            // TODO Auto-generated catch block
            LOGGER.error(e);
        }
        System.out.println("JWT: " + signedJWT.serialize());
        return signedJWT;
    }

    /**
     * creates a JSON Web Token with user id, roles and client public key
     * 
     * @param oldJWT - the given JSON Web Token
     * 
     * @return the new JSON WebToken
     */
    private static SignedJWT createJWT(SignedJWT oldJWT) {
        if (oldJWT == null) {
            return null;
        }
        String submittedUser = MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(oldJWT);
        JWK clientPubKey = MCRJSONWebTokenUtil.retrievePublicKeyFromAuthenticationToken(oldJWT);
        if (submittedUser != null && clientPubKey != null) {
            return MCRJSONWebTokenUtil.createJWT(submittedUser, Collections.singletonList("restapi"),
                MCRFrontendUtil.getBaseURL(),
                clientPubKey);
        }
        return null;
    }

    /**
     * returns a fresh JSON Web Token as String to be used in HTTP Header 'Authorization"
     * @param oldJWT the given JSON Web Token
     * @return the new JSON Web Token as String with prefix 'Bearer '
     */
    public static String createJWTAuthorizationHeader(SignedJWT oldJWT) {
        if (oldJWT != null) {
            SignedJWT newJWT = createJWT(oldJWT);
            if (newJWT != null) {
                return "Bearer " + newJWT.serialize();
            }
        }
        return null;
    }

    /**
     * returns the access token from Request Header "Authorization"
     * if the token is invalid an MCRRestAPIException is thrown
     * 
     * @param request - the HTTPServletRequest object
     * @return the JSON Web Token or null, if not provided in request
     * @throws MCRRestAPIException
     */
    public static SignedJWT retrieveAuthenticationToken(HttpServletRequest request) throws MCRRestAPIException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String authToken = auth.substring(7).trim();
            try {
                JWSObject jwsObj = JWSObject.parse(authToken);
                SignedJWT signedJWT = jwsObj.getPayload().toSignedJWT();
                // JWK class does equals only by object id
                if (signedJWT.verify(new RSASSAVerifier((RSAPublicKey) MCRJSONWebTokenUtil.RSA_KEYS.getPublic()))
                    && jwsObj.getHeader().getJWK().toJSONString()
                        .equals(JWK.parse(signedJWT.getJWTClaimsSet().getJSONObjectClaim("sub_jwk")).toJSONString())) {
                    Date expires = signedJWT.getJWTClaimsSet().getExpirationTime();
                    if (Instant.now().isBefore(expires.toInstant())) {
                        return signedJWT;
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            .withLocale(Locale.GERMANY).withZone(ZoneId.systemDefault());

                        throw new MCRRestAPIException(Status.UNAUTHORIZED,
                            new MCRRestAPIError(MCRRestAPIError.CODE_INVALID_AUTHENCATION,
                                "The Authentication Token expired at " + formatter.format(expires.toInstant()),
                                "Please log-in again."));
                    }

                } else {
                    throw new MCRRestAPIException(Status.UNAUTHORIZED,
                        new MCRRestAPIError(MCRRestAPIError.CODE_INVALID_AUTHENCATION,
                            "The signature of the Authentication Token could not be verified.", null));
                }
            } catch (ParseException | JOSEException e) {
                LOGGER.error(e);
                throw new MCRRestAPIException(Status.UNAUTHORIZED, new MCRRestAPIError(
                    MCRRestAPIError.CODE_INVALID_AUTHENCATION, "Authentication is invalid.", e.getMessage()));
            }
        } else {
            return null;
        }
    }
}
