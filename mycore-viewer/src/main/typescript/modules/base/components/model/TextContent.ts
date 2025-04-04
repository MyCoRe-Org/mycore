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

import { Position2D, Rect, Size2D } from "../../Utils";

export interface TextContentModel {
  content: Array<TextElement>;
  links: Link[];
  internLinks: InternLink[];
}

export interface Link {
  url: string;
  rect: Rect;
}

export interface InternLink {
  rect: Rect;

  pageNumberResolver(callback: (page: string) => void): void;
}

export interface TextElement {
  fromBottomLeft?: boolean;
  angle?: number;
  size: Size2D;
  text: string;
  pos: Position2D;
  fontFamily?: string;
  fontSize?: number;
  pageHref: string;
  mouseenter?: () => void;
  mouseleave?: () => void;

  toString(): string;
}

