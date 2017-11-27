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

/* globals angular */
{
    "use strict";
    angular.module("ng-image-zoom", []).directive('imageZoom', function () {
        var checkAttributes = function (attrs, el) {
            if (!("highResSrc" in attrs)) {
                throw "no high res src found for " + el.toString();
            }
            if (!("lowResSrc" in attrs)) {
                throw "no high res src found for " + el.toString();
            }
        };

        return {
            restrict: 'A',
            link: function (scope, el, attrs) {
                checkAttributes(attrs, el);
                var highResSrc = attrs.highResSrc;
                var lowResSrc = attrs.lowResSrc;
                var parent = el.parent();

                var update = function () {
                    highResSrc = attrs.highResSrc;
                    lowResSrc = attrs.lowResSrc;
                    el.css({
                        "background-image": "url(\"" + lowResSrc + "\")",
                        "background-size": "contain",
                        "background-repeat": "no-repeat"
                    });
                };

                scope.$watch(function () {
                    return attrs.lowResSrc;
                }, function () {
                    update();
                });


                scope.$watch(function () {
                    return attrs.highResSrc;
                }, function () {
                    update();
                });


                var mouseMoveHandler = function (e) {
                    var el = jQuery(this);
                    var posX = e.offsetX || e.pageX - el.offset().left;
                    var posY = e.offsetY || e.pageY - el.offset().top;
                    var width = el.width();
                    var height = el.height();


                    var percentX = (posX / width) * 100;
                    var percentY = (posY / height) * 100;

                    el.css({
                        "background-position": percentX + "% " + percentY + "%"
                    });
                };

                var stopHandler = function (e) {
                    var el = jQuery(this);
                    el.unbind("mousemove", mouseMoveHandler);
                    el.css({
                        "background-image": "url(\"" + lowResSrc + "\")",
                        "background-size": "contain",
                        "background-position": ""
                    });

                };

                var startHandler = function (e) {
                    var el = jQuery(this);
                    el.attr("alt", "zoomedImage");
                    el.css({
                        "background-image": "url(\"" + highResSrc + "\")",
                        "background-size": "auto"
                    });
                    el.mousemove(mouseMoveHandler);
                    mouseMoveHandler.call(this, e);
                };

                el.on("mousedown", startHandler);
                el.on("mouseup", stopHandler);


                scope.$on("destroy", function () {
                    el.off("mousedown", startHandler);
                    el.off("mouseup", stopHandler);
                    el.off("mousemove", mouseMoveHandler);
                });
            }
        };

    });

}
