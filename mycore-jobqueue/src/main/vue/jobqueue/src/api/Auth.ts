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
