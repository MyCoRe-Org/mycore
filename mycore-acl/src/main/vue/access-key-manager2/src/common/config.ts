/*!
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

interface RawConfig {
  webApplicationBaseURL: string;
  currentLang: string;
  'MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes': string;
}

const rawConfig = (
  import.meta.env.DEV === true
    ? Object.fromEntries(
        Object.entries(import.meta.env)
          .filter(([key]) => key.startsWith('VITE_APP_CONFIG_'))
          .map(([key, value]) => [key.replace('VITE_APP_CONFIG_', ''), value])
      )
    : window.mycore
) as RawConfig;

export interface AppConfig {
  baseUrl: string;
  currentLang: string;
}

export const appConfig: AppConfig = {
  baseUrl: rawConfig.webApplicationBaseURL,
  currentLang: rawConfig.currentLang,
};

export interface AccessKeyConfig {
  allowedAccessKeySessionPermissions: string[];
}

export const accessKeyConfig: AccessKeyConfig = {
  allowedAccessKeySessionPermissions:
    rawConfig['MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes'].split(
      ','
    ),
};

export const I18N_PREFIX = 'component.acl.accesskey.*';
