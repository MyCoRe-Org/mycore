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

// The MCRVueRootServlet injects the MyCoRe web application base URL and the current language into the
// page as window.mycore.webApplicationBaseURL / window.mycore.currentLang.

export function getWebApplicationBaseURL(): string {
  if (import.meta.env.DEV) {
    return "http://localhost:8291/mir/";
  }
  if ((window as any).mycore?.webApplicationBaseURL) {
    return (window as any).mycore.webApplicationBaseURL as string;
  }
  throw new Error("Fatal error: 'mycore.webApplicationBaseURL' is not set.");
}

export function getCurrentLanguage(): string {
  if (import.meta.env.DEV) {
    return "de";
  }
  return ((window as any).mycore?.currentLang as string) || "de";
}
