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

jQuery(document).ready(function () {
    const CHANGES_ATTR_NAME = "data-alto-changes";
    const DATA_BASE_URL = "data-base-url";

    let changesTable = jQuery("[" + CHANGES_ATTR_NAME + "]");
    let baseUrl = changesTable.attr(DATA_BASE_URL);
    let url = changesTable.attr(CHANGES_ATTR_NAME);
    let data = {start: 0, changes: null};
    let tbody = jQuery("tbody[data-id=list-table-body]");

    const template = function (str, data) {
        return str.replace(/\${(.*?)}/g, function (_, code) {
            var scoped = code.replace(/(["'\.\w\$]+)/g, function (match) {
                return /["']/.test(match[0]) ? match : 'scope.' + match;
            });
            try {
                return new Function('scope', 'return ' + scoped)(data);
            } catch (e) {
                return '';
            }
        });
    };

    let updateView = function () {
        if (data.changes !== null) {
            let tableHTML = data.changes.map(function (change) {
                change.displayDate = new Date(change.created).toString();
                return template('<tr data-change-derivate="${derivateID}" data-change-pid="${pid}">' +
                    '<td>${sessionID}</td>' +
                    '<td>${objectTitle}</td>' +
                    '<td>${derivateID}</td>' +
                    '<td>${displayDate}</td>' +
                    '</tr>', change);
            }).join("");

            tbody.html(tableHTML);
            tbody.on('click', 'tr', function (e) {
                let target = jQuery(e.currentTarget);
                let derivate = target.attr("data-change-derivate");
                let pid = target.attr("data-change-pid");

                let link = template('${baseUrl}rsc/viewer/${derivate}?altoChangeID=${pid}', {
                    baseUrl: baseUrl,
                    derivate: derivate,
                    pid: pid
                });
                window.open(link).focus();
            });
        }
    };


    jQuery.ajax(url, {
        data: {
            "start": data.start
        },
        type: "GET"
    }).done(function (obj) {
        data.changes = obj;
        updateView();
    }).fail(function (obj) {
        console.log(arguments);
    });
});
