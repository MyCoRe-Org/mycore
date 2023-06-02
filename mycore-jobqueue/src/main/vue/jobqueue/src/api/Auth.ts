import {getBaseURL} from "@/api/BaseURL";

const mode = import.meta.env.MODE;

export interface JWTResponse {
    login_success: boolean;
    access_token: string;
    token_type: string;
}

export const getAuthorizationHeader = async () => {
    const isDev = mode === 'development';
    if(isDev){
        return "Basic " + btoa('administrator:alleswirdgut');
    } else {
        let jwtURL = `${getBaseURL()}rsc/jwt`;
        const response = await fetch(jwtURL);
        const jwt = await response.json() as JWTResponse;

        if(!jwt.login_success){
            throw new Error("Login failed");
        }
        jwtURL = `${jwt.token_type} ${jwt.access_token}`;
        return jwtURL
    }
}