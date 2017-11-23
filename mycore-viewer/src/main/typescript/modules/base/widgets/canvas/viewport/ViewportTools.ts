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

/// <reference path="../../../Utils.ts" />
/// <reference path="Viewport.ts" />
/// <reference path="../model/PageArea.ts" />
/// <reference path="../../../components/model/AbstractPage.ts" />

namespace mycore.viewer.widgets.canvas {
    export class ViewportTools {

        public static centerViewportOnPage(vp: Viewport, pageAreaInformation: PageAreaInformation) {
            vp.position = pageAreaInformation.position;
        }

        public static fitViewportOverPage(vp: Viewport, pageAreaInformation: PageAreaInformation, page: model.AbstractPage) {
            if (vp.size.width != 0 && vp.size.height != 0) {
                ViewportTools.centerViewportOnPage(vp, pageAreaInformation);
                var vpRotated = vp.size.getRotated(vp.rotation);
                vp.scale = Math.min(vpRotated.width / page.size.width, vpRotated.height / page.size.height);
            } else {
                /**
                 * This is used on start of the viewer.
                 * When the viewport width is null it waits until the viewport has a usable size and then runs the function.
                 * @type {{propertyChanged: (function(IviewProperty<Size2D>, ViewerProperty<Size2D>): undefined)}}
                 */
                var changeObs = {
                    propertyChanged(_old: ViewerProperty<Size2D>, _new: ViewerProperty<Size2D>) {
                        ViewportTools.fitViewportOverPage(vp, pageAreaInformation, page);
                        vp.sizeProperty.removeObserver(changeObs)
                    }
                };
                vp.sizeProperty.addObserver(changeObs);
            }
        }

        public static fitViewportOverPageWidth(vp: Viewport, pageAreaInformation: PageAreaInformation, page: model.AbstractPage) {
            if (vp.size.width != 0 && vp.size.height != 0) {

                var pageSize = page.size.getRotated(vp.rotation).scale(pageAreaInformation.scale);
                vp.scale = vp.size.width / (pageSize.width);

                var vpSize = vp.size.getRotated(vp.rotation);
                var vpPosition = (vp.rotation == 0 || vp.rotation == 180) ? vp.position : new Position2D(vp.position.y, vp.position.x);
                var yPosition = Math.max(vpPosition.y, pageAreaInformation.position.y - (pageSize.height / 2) + vp.size.scale(1 / vp.scale).height / 2);
                yPosition = Math.min(yPosition, pageAreaInformation.position.y + (pageSize.height / 2) - vp.size.scale(1 / vp.scale).height / 2);

                if (vp.size.height > pageSize.scale(vp.scale).width) {
                    yPosition = 0;
                }

                if (vp.rotation == 0 || vp.rotation == 180) {
                    vp.position = new Position2D(pageAreaInformation.position.x, yPosition);
                } else {
                    vp.position = new Position2D(yPosition, pageAreaInformation.position.x);
                }

            }
        }

    }
}
