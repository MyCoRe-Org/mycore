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

import { MCRAccessKeyConfig } from '@golsch/test/acl/accesskey';

export const BASE_URL =
  import.meta.env.VITE_APP_WEB_APPLICATION_BASE_URL ??
  (window.webApplicationBaseURL as string);

export const CURRENT_LANG =
  import.meta.env.VITE_APP_CURRENT_LANG ?? (window.currentLang as string);

const ALLOWED_SESSION_TYPES =
  'MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes';

export const fetchAccessKeyConfig = async (
  baseUrl: string
): Promise<MCRAccessKeyConfig> => {
  const response = await fetch(`${baseUrl}config.json`);
  if (!response.ok) {
    throw new Error('Failed to load app configuration');
  }
  const config = await response.json();
  return {
    isAccessKeySessionEnabled:
      config[ALLOWED_SESSION_TYPES] !== undefined &&
      config[ALLOWED_SESSION_TYPES].length > 0,
    allowedAccessKeySessionPermissions:
      config[ALLOWED_SESSION_TYPES].split(','),
  };
};
