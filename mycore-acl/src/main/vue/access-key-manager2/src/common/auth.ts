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

import { fetchJwt as mcrFetchJwt } from '@mycore-test/js-common/auth';

export const fetchJWT = async (
  baseUrl: string,
  reference?: string,
  isSessionEnabled?: boolean
): Promise<string> => {
  const attributeName = `acckey_${reference}`;
  return await mcrFetchJwt(baseUrl, {
    userAttributes: [attributeName],
    sessionAttributes: isSessionEnabled ? [attributeName] : undefined,
  });
};
