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

import axios, { AxiosResponse } from "axios";

export function generateRandomString(length: number): string {
  const keylistalpha = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  const keylistint = "123456789";
  const keylistspec = "!@#_%$";
  let temp = '';
  let len = length / 2;
  len = len - 1;
  const lenspec = length - len - len;
  for (let i = 0; i < len; i++) {
    temp += keylistalpha.charAt(Math.floor(Math.random() * keylistalpha.length));
  }
  for (let i = 0; i < lenspec; i++) {
    temp += keylistspec.charAt(Math.floor(Math.random() * keylistspec.length));
  }
  for (let i = 0; i < len; i++) {
    temp += keylistint.charAt(Math.floor(Math.random() * keylistint.length));
  }
  temp = temp.split('').sort(function() {
    return 0.5 - Math.random()
  }).join('');
  return temp;
}

declare global {
  interface Window { 
    webApplicationBaseURL: string; 
    objectID: string; 
    currentLang: string; 
  }
}

export async function fetchJWT(webApplicationBaseURL: string, objectID: string): Promise<AxiosResponse> {
  return await axios.get(`${webApplicationBaseURL}rsc/jwt`, { params: {"ua": `acckey_${objectID}` } });
}

export async function fetchDict(baseURL:string, locale: string): Promise<AxiosResponse> {
  return await axios.get(`${baseURL}rsc/locale/translate/${locale}/mcr.accessKey.*`);
}

export function getWebApplicationBaseURL(): string {
  return window.webApplicationBaseURL;
}

function getParameterByName(name: string, url = window.location.href.toLowerCase()): string {
    name = name.replace(/[[\]]/g, '\\$&');
    const regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

export function getObjectID(): string {
  if (window.objectID != null) {
    return window.objectID;
  }
  return getParameterByName("objectid");
}

export function getLocale(): string {
  if (window.currentLang != null) {
    return window.currentLang;
  } else if (document.querySelector('html').getAttribute('lang') != null){
    return document.querySelector('html').getAttribute('lang');
  } else if (navigator.languages != null) {
    return navigator.languages[0];
  } else if (navigator.language != null) {
    return navigator.language;
  } else {
    return "en";
  }
}

const webApplicationBaseURL = getWebApplicationBaseURL();
const objectID = getObjectID();
const locale = getLocale();

export { webApplicationBaseURL, objectID, locale }


