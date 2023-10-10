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

// Forced logout from ORCID is handled via prompt=login parameter already
async function orcidOAuth(scope) {
    let uri = `${webApplicationBaseURL}rsc/orcid/oauth/init`;
    if (scope) {
        uri += `?scope=${scope}`;
    }
    // Open ORCID login in popup window
    const width = 540;
    const height = 750;
    const left = window.top.outerWidth/2 + window.top.screenX - (width/2);
    const top = window.top.outerHeight/2 + window.top.screenY - (height/2);
    window.open(uri, '_blank', `toolbar=no, width=${width}, height=${height}, top=${top}, left=${left}`);
}

async function revokeORCID(orcid, redirect_uri) {
    const jwt = await fetchJWT();
    const revokeURI = `${webApplicationBaseURL}api/orcid/v1/revoke/${orcid}`;
    const revoke = await fetch(revokeURI, {
        method: 'POST',
        headers: { Authorization: `Bearer ${jwt}`}
    });
    if (!revoke.ok) {
        throw new Error("Revoke failed");
    }
    if (redirect_uri) {
        window.location.replace(redirect_uri);
    }
}

async function fetchJWT() {
    const response = await fetch(`${webApplicationBaseURL}rsc/jwt`);
    if (!response.ok) {
        throw new Error(`Cannot fetch JWT: ${response.status}`);
    }
    const result = await response.json();
    if (!result.login_success) {
        throw new Error("Login failed");
    }
    return result.access_token;
}
