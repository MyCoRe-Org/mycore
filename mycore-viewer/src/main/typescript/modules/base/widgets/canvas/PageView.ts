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

export class PageView {

  public container: HTMLElement = document.createElement("div");
  public drawCanvas: HTMLCanvasElement = null;
  public markCanvas: HTMLCanvasElement = null;

  constructor(public drawImage: boolean = true, public drawHTML: boolean = true) {
    const drawFilter = drawImage && drawHTML ? "grayscale(1) contrast(1000%)" : null;
    this.drawCanvas = PageView.createCanvas(1, drawFilter);
    this.markCanvas = PageView.createCanvas(4);

    this.container.append(this.drawCanvas);
    this.container.append(this.markCanvas);

    this.container.style.position = "absolute";
    this.container.style.top = "0px";
    this.container.style.left = "0px";
    this.container.style.bottom = "0px";
    this.container.style.right = "0px";
    this.container.style.overflow = "hidden";

    const ctx1 = <CanvasRenderingContext2D>this.drawCanvas.getContext("2d");
    const ctx2 = <CanvasRenderingContext2D>this.markCanvas.getContext("2d");

    ctx1.imageSmoothingEnabled = false;
    ctx2.imageSmoothingEnabled = false;
  }

  private static createCanvas(zIndex: number = 1, filter?: string) {
    const canvas: HTMLCanvasElement = document.createElement("canvas");
    canvas.style.transform = "scale(1.0)";
    canvas.style.position = "absolute";
    canvas.style.zIndex = zIndex + "";
    if (filter) {
      canvas.style.filter = filter;
    }
    return canvas;
  }

}

