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
 *
 *
 */
const CSL_EXPORT_ROWS = "MCR.Export.CSL.Rows";

window.addEventListener('load', function () {
    let list = document.querySelectorAll("[data-export]");
    let array = Array.prototype.slice.call(list);

    array.forEach(function (element) {
        let type = element.getAttribute("data-export");
        let styleSelect = element.querySelector("[name='style']");
        let formatSelect = element.querySelector("[name='format']");

        element.querySelector("[data-trigger-export='true']")
            .addEventListener('click', function () {
                let style = (styleSelect.value || "").toLowerCase();
                let format = (formatSelect.value || "").toLowerCase();

                if (style.trim().length > 0 && format.trim().length > 0) {
                    let transformerQuery = "&XSL.Transformer=" + type + "-csl-" + format;
                    let styleQuery = "&XSL.style=" + style;

                    let location = window.location.href;
                    let hashIndex = location.indexOf("#");
                    if (hashIndex !== -1) {
                        location = location.substring(0, hashIndex);
                    }
                    // check if solr search and set rows to 500
                    if (type.indexOf("response") !== -1) {
                        let rows = CSL_EXPORT_ROWS in window ? window[CSL_EXPORT_ROWS] : "500";
                        if (location.indexOf("rows") !== -1) {
                            location = location.replace(/([?&])(rows=)[0-9]+/g, "$1$2" + rows);
                        } else {
                            let joinSign = location.indexOf("?") === -1 ? "?" : "&";
                            location += joinSign + "rows=" + rows;
                        }
                    }

                    location += transformerQuery + styleQuery;
                    window.location.assign(location);
                }
            });
    });
});