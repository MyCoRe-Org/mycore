async function orcidOAuth(scope) {
    // Forced logout from ORCID is handled via prompt=login parameter already

    // Prepare ORCID OAuth
    const oauthURI = await fetchOAuthURI(scope);

    // Open ORCID login in popup window
    const width = 540;
    const height = 750;
    const left = window.top.outerWidth/2 + window.top.screenX - (width/2);
    const top = window.top.outerHeight/2 + window.top.screenY - (height/2);
    window.open(oauthURI, '_blank', `toolbar=no, width=${width}, height=${height}, top=${top}, left=${left}`);
}

async function fetchOAuthURI(scope) {
    const jwt = await fetchJWT();
    let uri = `${webApplicationBaseURL}api/orcid/v1/oauth-uri`;
    if(scope) {
        uri += `?scope=${scope}`;
    }
    const response = await fetch(uri, {
        headers: { Authorization: `Bearer ${jwt}`}
    });
    if (!response.ok) {
        throw new Error(`Cannot fetch OAuthURI: ${response.status}`);
    }
    return await response.text();
}

async function revokeORCID(orcid) {
    const jwt = await fetchJWT();
    const revoke = fetch(`${webApplicationBaseURL}api/orcid/v1/revoke/${orcid}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${jwt}`}
    });
    if (!revoke.ok) {
        throw new Error("Revoke failed");
    }
}

async function fetchJWT() {
    const response = await fetch(`${webApplicationBaseURL}rsc/jwt`);
    if (!response.ok) {
        throw new Error(`Cannot fetch JWT: ${response.status}`);
    }
    const result = await response.json();
    if (result.login_success) {
       return result.access_token;
    } else {
        throw new Error("Login failed");
    }
}
