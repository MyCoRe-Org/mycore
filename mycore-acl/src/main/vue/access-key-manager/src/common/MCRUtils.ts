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

import axios, { AxiosResponse } from 'axios';

export function generateRandomString(length: number): string {
  const keylistalpha = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  const keylistint = '123456789';
  const keylistspec = '!@#_%$';
  let temp = '';
  let len = length / 2;
  len -= 1;
  const lenspec = length - len - len;
  for (let i = 0; i < len; i += 1) {
    temp += keylistalpha.charAt(Math.floor(Math.random() * keylistalpha.length));
  }
  for (let i = 0; i < lenspec; i += 1) {
    temp += keylistspec.charAt(Math.floor(Math.random() * keylistspec.length));
  }
  for (let i = 0; i < len; i += 1) {
    temp += keylistint.charAt(Math.floor(Math.random() * keylistint.length));
  }
  temp = temp.split('').sort(() => 0.5 - Math.random()).join('');
  return temp;
}

declare global {
  interface Window {
    webApplicationBaseURL: string;
    objectID: string;
    derivateID: string;
    currentLang: string;
    accessKeySession: string;
  }
}

export function urlEncode(value: string): string {
  return btoa(value)
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

export function urlDecode(value: string): string {
  return atob(value.replace(/_/g, '/').replace(/-/g, '+'));
}

export async function fetchJWT(webApplicationBaseURL: string, objectID: string,
  derivateID: string, includeSession: boolean): Promise<AxiosResponse> {
  const params = new URLSearchParams();
  params.append('ua', `acckey_${objectID}`);
  if (derivateID) {
    params.append('ua', `acckey_${derivateID}`);
  }
  if (includeSession) {
    params.append('sa', `acckey_${objectID}`);
    if (derivateID) {
      params.append('sa', `acckey_${derivateID}`);
    }
  }
  return axios.get(`${webApplicationBaseURL}rsc/jwt`, { params });
}

export async function fetchDict(baseURL:string, locale: string): Promise<AxiosResponse> {
  return axios.get(`${baseURL}rsc/locale/translate/${locale}/mcr.accessKey.*`);
}

export function getWebApplicationBaseURL(): string {
  return window.webApplicationBaseURL;
}

function getParameterByName(name: string, url = window.location.href.toLowerCase()): string {
  const nameFixed = name.replace(/[[\]]/g, '\\$&');
  const regex = new RegExp(`[?&]${nameFixed}(=([^&#]*)|&|#|$)`);
  const results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

export function getObjectID(): string {
  if (window.objectID != null) {
    return window.objectID;
  }
  return getParameterByName('objectid');
}

export function getDerivateID(): string {
  if (window.derivateID != null) {
    return window.derivateID;
  }
  return getParameterByName('derivateid');
}

export function getLocale(): string {
  if (window.currentLang != null) {
    return window.currentLang;
  } if (document.querySelector('html').getAttribute('lang') != null) {
    return document.querySelector('html').getAttribute('lang');
  } if (navigator.languages != null) {
    return navigator.languages[0];
  } if (navigator.language != null) {
    return navigator.language;
  }
  return 'en';
}

export function getIsSessionEnabled(): boolean {
  return window.accessKeySession !== 'false';
}

const webApplicationBaseURL = getWebApplicationBaseURL();
const objectID = getObjectID();
const derivateID = getDerivateID();
const locale = getLocale();
const isSessionEnabled = getIsSessionEnabled();

export {
  webApplicationBaseURL, objectID, derivateID, locale, isSessionEnabled,
};
