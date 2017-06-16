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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    /** username and password, combined by ":"
     * 
     * @param token
     * @return
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

    public static String retrieveUsernameFromAuthenticationToken(SignedJWT signedJWT) {
        try {
            // Extract payload

            RSAKey serverPublicKey = RSAKey.parse(signedJWT.getHeader().getJWK().toJSONObject());
            if (signedJWT.verify(new RSASSAVerifier(serverPublicKey))) {

                //Token is valid
                String username = signedJWT.getJWTClaimsSet().getSubject();
                return username;
            }
        } catch (ParseException | JOSEException e) {
            LOGGER.error(e);
        }

        return null;
    }

    public static String createEmptyJWTwithPublicKey(String webAppBaseURL) {

        String jwt = null;
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(webAppBaseURL).jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(currentTime.toInstant())).build();
        String keyID = UUID.randomUUID().toString();
        JWK jwk = new RSAKey.Builder((RSAPublicKey) RSA_KEYS.getPublic()).keyID(keyID).build();
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).jwk(jwk).build();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
        try {
            signedJWT.sign(new RSASSASigner(RSA_KEYS.getPrivate()));
            jwt = signedJWT.serialize();
        } catch (JOSEException e) {
            LOGGER.error(e);
        }

        System.out.println("JWT: " + jwt);
        return jwt;

    }

    public static SignedJWT createJWT(String user, List<String> roles, String webAppBaseURL, JWK clientPubKey) {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(webAppBaseURL).jwtID(UUID.randomUUID().toString())
            .expirationTime(Date.from(currentTime.plusMinutes(EXPIRATION_TIME_MINUTES).toInstant()))
            .issueTime(Date.from(currentTime.toInstant()))
            .notBeforeTime(Date.from(currentTime.minusMinutes(EXPIRATION_TIME_MINUTES).toInstant())).subject(user)
            // additional claims/attributes about the subject can be added
            // claims.setClaim("email", "mail@example.com");
            // multi-valued claims work too and will end up as a JSON array
            .claim("roles", roles).claim("sub_jwk", clientPubKey).build();

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

    public static SignedJWT createJWT(SignedJWT oldJWT) {
        String submittedUser = MCRJSONWebTokenUtil.retrieveUsernameFromAuthenticationToken(oldJWT);
        JWK clientPubKey = MCRJSONWebTokenUtil.retrievePublicKeyFromAuthenticationToken(oldJWT);
        if (submittedUser != null && clientPubKey != null) {
            return MCRJSONWebTokenUtil.createJWT(submittedUser, Arrays.asList("rest-api"), MCRFrontendUtil.getBaseURL(),
                clientPubKey);
        }
        return null;
    }

    /**
     * returns the access token from Request Header "Authorization"
     * if the token is invalid an MCRRestAPIException is thrown
     * 
     * @param request
     * @return the JSON Web Token
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

                        throw new MCRRestAPIException(
                            MCRRestAPIError.create(Status.UNAUTHORIZED, MCRRestAPIError.CODE_INVALID_AUTHENCATION,
                                "The Authentication Token expired at " + formatter.format(expires.toInstant()),
                                "Please log-in again."));
                    }

                } else {
                    throw new MCRRestAPIException(
                        MCRRestAPIError.create(Status.UNAUTHORIZED, MCRRestAPIError.CODE_INVALID_AUTHENCATION,
                            "The signature of the Authentication Token could not be verified.", null));
                }
            } catch (ParseException | JOSEException e) {
                LOGGER.error(e);
                throw new MCRRestAPIException(MCRRestAPIError.create(Status.UNAUTHORIZED,
                    MCRRestAPIError.CODE_INVALID_AUTHENCATION, "Authentication is invalid.", e.getMessage()));
            }
        }
        throw new MCRRestAPIException(MCRRestAPIError.create(Status.UNAUTHORIZED,
            MCRRestAPIError.CODE_INVALID_AUTHENCATION, "Authentication Token is missing.",
            "Please login via /auth/login and provide the authentication token as HTTP Request Header 'Authorization"));
    }
}
