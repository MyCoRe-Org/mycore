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
import java.sql.Date;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return null;
    }

    public static JWK retrievePublicKeyFromAuthenticationToken(SignedJWT signedJWT) {
        JWK result = null;
        try {
            result = signedJWT.getHeader().getJWK();
            RSAKey publicKey = RSAKey.parse(result.toJSONObject());
            if (signedJWT.verify(new RSASSAVerifier(publicKey))) {
                return result;
            }
        } catch (ParseException | JOSEException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            e.printStackTrace();
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
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

        System.out.println("JWT: " + jwt);
        return jwt;

    }

    public static String createJWT(String user, List<String> roles, String webAppBaseURL, JWK clientPubKey) {
        String jwt = null;

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
            jwt = signedJWT.serialize();
        } catch (JOSEException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

        System.out.println("JWT: " + jwt);
        return jwt;

    }

    public static boolean validateToken(String token) {
        return true;
    }

    public static boolean validateToken(HttpServletRequest request) {
        String authToken = request.getHeader("Authentication");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            authToken = authToken.substring(7).trim();
            return validateToken(authToken);
        }
        return false;
    }
    
    public static SignedJWT retrieveAuthenticationToken(HttpServletRequest request) {
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
                    return signedJWT;
                }
            } catch (ParseException | JOSEException e) {
                //ignore
            }
        }
        return null;
    }
}
