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

import { ToolbarComponent } from "./ToolbarComponent";
import { ViewerProperty } from "../../../Utils";

export class ToolbarImage extends ToolbarComponent {
  constructor(id: string, href: string) {
    super(id);
    this.addProperty(new ViewerProperty<string>(this, "href", href));
  }

  public get href(): string {
    return this.getProperty("href").value;
  }

  public set href(href: string) {
    this.getProperty("href").value = href;
  }
}
