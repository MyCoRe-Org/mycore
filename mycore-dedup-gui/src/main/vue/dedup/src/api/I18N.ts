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

import { getCurrentLanguage, getWebApplicationBaseURL } from "@/api/BaseURL";

const cache: { [key: string]: string | Promise<string>; } = {};

/**
 * Fills the given reactive object with the translations of its keys, resolved via the MyCoRe locale
 * service. Translations are cached across components.
 */
export function resolveI18N(i18n: { [key: string]: string; }): void {
  const baseUrl = getWebApplicationBaseURL();
  const language = getCurrentLanguage();
  Object.keys(i18n).forEach(key => {
    const cached = cache[key];
    if (cached !== undefined) {
      if (cached instanceof Promise) {
        cached.then(translation => {
          i18n[key] = translation;
        });
      } else {
        i18n[key] = cached;
      }
      return;
    }
    const promise = fetch(`${baseUrl}rsc/locale/translate/${language}/${key}`)
      .then(response => response.ok ? response.text() : key);
    cache[key] = promise;
    promise.then(translation => {
      i18n[key] = translation;
      cache[key] = translation;
    });
  });
}
