async function orcidOAuth(scope) {
    var resURI = 'rsc/orcid/auth';
    if(scope) {
        resURI += `?scope=${scope}`;
    }
    var width = 540;
    var height = 750;
    var left = (window.innerWidth/2)-(width/2);
    var top = (window.innerHeight/2)-(height/2);
    const logout = await fetch('https://orcid.org/userStatus.json?logUserOut=true', {dataType: 'jsonp'});
    if(!logout.ok) {
        console.warn('Could not logout user from ORCID');
        return;
    }
    window.open(webApplicationBaseURL + resURI, '_blank', `toolbar=no, width=${width}, height=${height}, top=${top}, left=${left}`);
}
