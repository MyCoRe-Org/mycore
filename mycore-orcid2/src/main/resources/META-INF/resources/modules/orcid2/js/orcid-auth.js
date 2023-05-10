async function orcidOAuth(scope) {
    const oauthURI = await fetchOAuthURI(scope);
    const width = 540;
    const height = 750;
    const left = (window.innerWidth/2)-(width/2);
    const top = (window.innerHeight/2)-(height/2);
    const logout = await fetch('https://orcid.org/userStatus.json?logUserOut=true', {dataType: 'jsonp'});
    if(!logout.ok) {
        console.warn('Could not logout user from ORCID');
        return;
    }
    window.open(oauthURI, '_blank', `toolbar=no, width=${width}, height=${height}, top=${top}, left=${left}`);
}

async function fetchOAuthURI(scope) {
    const jwt = await fetchJWT();
    let uri = `${webApplicationBaseURL}api/orcid/v1/oauth-uri`;
    if(scope) {
        uri += `?scope=${scope}`;
    }
    const response = await fetch(uri, {
        headers: { Authentication: `Bearer ${jwt}`}
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
        headers: { Authentication: `Bearer ${jwt}`}
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
