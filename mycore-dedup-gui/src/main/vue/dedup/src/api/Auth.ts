/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import { getWebApplicationBaseURL } from "@/api/BaseURL";

export interface JWTResponse {
  login_success: boolean;
  access_token: string;
  token_type: string;
}

/**
 * Exchanges the MyCoRe session cookie for a JWT at {@code rsc/jwt} and returns the value for the
 * {@code Authorization} header of the deduplication API requests. In development mode there is no
 * MyCoRe session for the Vite dev server, so basic authentication is used instead.
 */
export async function getAuthorizationHeader(): Promise<string> {
  if (import.meta.env.DEV) {
    return "Basic " + btoa("administrator:alleswirdgut");
  }
  const response = await fetch(`${getWebApplicationBaseURL()}rsc/jwt`, { cache: "no-store" });
  const jwt = await response.json() as JWTResponse;
  if (!jwt.login_success) {
    throw new Error("Could not obtain a JWT for the current session.");
  }
  return `${jwt.token_type} ${jwt.access_token}`;
}
