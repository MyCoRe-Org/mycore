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

package org.mycore.orcid2.auth;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the ORCID OAuth access token response.
 * 
 * See <a href="https://members.orcid.org/api/oauth/3legged-oauth">ORCID documentation</a>
 */
public class MCRORCIDOAuthAccessTokenResponse {

    private String accessToken;

    private String tokenType;

    private String refreshToken;

    private String expiresIn;

    private String name;

    private String scope;

    private String orcid;

    /**
     * Creates empty MCRORCIDOAuthAccessTokenResponse object.
     */
    public MCRORCIDOAuthAccessTokenResponse() {
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
     * Returns the token life time in seconds.
     * 
     * @return token life time
     */
    @JsonProperty("expires_in")
    public String getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the life time in seconds.
     * 
     * @param expiresIn life time in seconds
     */
    public void setExpiresIn(String expiresIn) {
        this.expiresIn = expiresIn;
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
     * Returns the name.
     * 
     * @return name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the ORCID iD.
     * 
     * @return ORCID iD
     */
    @JsonProperty("orcid")
    public String getORCID() {
        return orcid;
    }

    /**
     * Sets the ORCID iD.
     * 
     * @param orcid the ORCID iD
     */
    public void setORCID(String orcid) {
        this.orcid = orcid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, tokenType, refreshToken, expiresIn, name, scope, orcid);
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
        MCRORCIDOAuthAccessTokenResponse other = (MCRORCIDOAuthAccessTokenResponse) obj;
        return Objects.equals(accessToken, other.accessToken) && Objects.equals(expiresIn, other.expiresIn)
            && Objects.equals(name, other.name) && Objects.equals(refreshToken, other.refreshToken)
            && Objects.equals(scope, other.scope) && Objects.equals(tokenType, other.tokenType)
            && Objects.equals(orcid, other.orcid);
    }
}
