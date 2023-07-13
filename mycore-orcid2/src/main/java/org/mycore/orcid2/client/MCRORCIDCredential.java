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

package org.mycore.orcid2.client;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the ORCID credential including access token.
 */
public class MCRORCIDCredential {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private String scope;

    private LocalDate expiration;

    /**
     * Creates MCRORCIDCredential object with access token.
     * 
     * @param accessToken the access token
     */
    public MCRORCIDCredential(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Creates empty MCRORCIDCredential object.
     */
    public MCRORCIDCredential() {
    }

    /**
     * Returns the access token.
     * 
     * @return access token
     */
    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     * 
     * @param accessToken the access token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns the refresh token.
     * 
     * @return refresh token
     */
    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     * 
     * @param refreshToken the refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Returns the token type.
     * 
     * @return token type
     */
    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     * 
     * @param tokenType the token type
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Returns the scope.
     * 
     * @return scope
     */
    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     * 
     * @param scope scope to set
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Returns the expire date.
     * 
     * @return expiration
     */
    public LocalDate getExpiration() {
        return expiration;
    }

    /**
     * Sets the expire date.
     * 
     * @param expiration expiration to set
     */
    public void setExpiration(LocalDate expiration) {
        this.expiration = expiration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, scope, tokenType, expiration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRORCIDCredential other = (MCRORCIDCredential) obj;
        return Objects.equals(accessToken, other.accessToken) && Objects.equals(refreshToken, other.refreshToken)
            && Objects.equals(scope, other.scope) && Objects.equals(tokenType, other.tokenType)
            && Objects.equals(expiration, other.expiration);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.ROOT, "access token: %s\n", accessToken));
        builder.append(String.format(Locale.ROOT, "refresh token: %s\n", refreshToken));
        builder.append(String.format(Locale.ROOT, "scope: %s", scope));
        return builder.toString();
    }
}
