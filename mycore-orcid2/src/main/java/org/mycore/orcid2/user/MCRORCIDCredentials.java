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

package org.mycore.orcid2.user;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the orcid credentials with all important properties.
 * Maps the access token json response.
 * 
 * See <a href="https://members.orcid.org/api/oauth/3legged-oauth">ORCID documentation</a>
 */
public class MCRORCIDCredentials implements Cloneable {

    private String orcid;

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private String expiresIn;

    private String name;

    private String scope;

    private LocalDate expiration;

    /**
     * Creates MCRORCIDCredentials object with orcid and access token.
     * 
     * @param orcid the orcid
     * @param accessToken the access token
     */
    public MCRORCIDCredentials(String orcid, String accessToken) {
        this.orcid = orcid;
        this.accessToken = accessToken;
    }

    /**
     * Creates MCRORCIDCredentials object with orcid.
     * 
     * @param orcid the orcid
     */
    public MCRORCIDCredentials(String orcid) {
        this.orcid = orcid;
    }

    /**
     * Creates empty MCRORCIDCredentials object.
     */
    public MCRORCIDCredentials() {
    }

    /**
     * @return orcid
     */
    @JsonProperty("orcid")
    public String getORCID() {
        return orcid;
    }

    /**
     * @param orcid orcid to set
     */
    public void setORCID(String orcid) {
        this.orcid = orcid;
    }

    /**
     * @return access token
     */
    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @param accessToken access token to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * @return refresh token
     */
    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * @param refreshToken refresh token to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * @return token type
     */
    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    /**
     * @param tokenType token type to set
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * @return expires in
     */
    @JsonProperty("expires_in")
    public String getExpiresIn() {
        return expiresIn;
    }

    /**
     * @param expiresIn expires in to set
     */
    public void setExpiresIn(String expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * @return name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * @param name name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return scope
     */
    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    /**
     * @param scope scope to set
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @return expiration
     */
    public LocalDate getExpiration() {
        return expiration;
    }

    /**
     * @param expiration expiration to set
     */
    public void setExpiration(LocalDate expiration) {
        this.expiration = expiration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, expiresIn, name, orcid, refreshToken, scope, tokenType, expiration);
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
        MCRORCIDCredentials other = (MCRORCIDCredentials) obj;
        return Objects.equals(accessToken, other.accessToken) && Objects.equals(expiresIn, other.expiresIn)
            && Objects.equals(name, other.name) && Objects.equals(orcid, other.orcid)
            && Objects.equals(refreshToken, other.refreshToken) && Objects.equals(scope, other.scope)
            && Objects.equals(tokenType, other.tokenType) && Objects.equals(expiration, other.expiration);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.ROOT, "orcid: %s\n", orcid));
        builder.append(String.format(Locale.ROOT, "access token: %s\n", accessToken));
        builder.append(String.format(Locale.ROOT, "refresh token: %s\n", refreshToken));
        builder.append(String.format(Locale.ROOT, "expires in: %s\n", expiresIn));
        builder.append(String.format(Locale.ROOT, "name: %s\n", name));
        builder.append(String.format(Locale.ROOT, "scope: %s", scope));
        return builder.toString();
    }
}
