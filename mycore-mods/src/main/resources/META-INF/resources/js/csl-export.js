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

function getDownloadLocation(type, format, style) {
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

    return location + transformerQuery + styleQuery;
}

function getSelectInfo(styleSelect, formatSelect) {
    let style = (styleSelect.value || "").toLowerCase();
    let format = (formatSelect.value || "").toLowerCase();
    return {style, format};
}

function changeTriggerState(trigger, enabled) {
    trigger.disabled = !enabled;
    if (enabled) {
        trigger.classList.remove("disabled");
    } else {
        trigger.classList.add("disabled");
    }
}

function setTriggerLoadingState(trigger, loading) {
    const spinnerCode = "<span class=\"spinner-border spinner-border-sm mr-1\" role=\"status\" aria-hidden=\"true\"></span>";
    const spinnerElement = trigger.querySelector("span.spinner-border");
    const isLoading = spinnerElement !== null;
    if (loading && !isLoading) {
        trigger.innerHTML = spinnerCode + trigger.innerHTML;
    }
    if (!loading && isLoading) {
        spinnerElement.remove();
    }
}

function createFetchFunction(trigger, initialSelectInfo, styleSelect, formatSelect) {
    // this function will be called when the request is started
    return function (response) {
        // this function will be called if the the request is started

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        response.blob().then(function(blob){
            // check if the select has changed to different style or format since request start
            let newSelectInfo = getSelectInfo(styleSelect, formatSelect);
            if (newSelectInfo.style === initialSelectInfo.style && newSelectInfo.format === initialSelectInfo.format) {

                trigger.href = URL.createObjectURL(blob);
                trigger.download = 'export.' + initialSelectInfo.format;
                setTriggerLoadingState(trigger, false);
                changeTriggerState(trigger, true);
            }
        });
    };
}

window.addEventListener('load', function () {
    let list = document.querySelectorAll("[data-export]");
    let array = Array.prototype.slice.call(list);

    array.forEach(function (element) {
        let type = element.getAttribute("data-export");
        const styleSelect = element.querySelector("[name='style']");
        const formatSelect = element.querySelector("[name='format']");
        const trigger = element.querySelector("[data-trigger-export='true']");

        if ("fetch" in window) {
            changeTriggerState(trigger, false);
            let onChange = function () {
                changeTriggerState(trigger, false);
                let selectInfo = getSelectInfo(styleSelect, formatSelect);
                if (selectInfo.style.length > 0 && selectInfo.format.length > 0) {
                    setTriggerLoadingState(trigger, true);
                    let location = getDownloadLocation(type, selectInfo.format, selectInfo.style);
                    fetch(location, {method: "GET"})
                        .then(createFetchFunction(trigger, selectInfo, styleSelect, formatSelect))
                        .catch(function (error) {
                            throw error;
                        });
                } else {
                    changeTriggerState(trigger, false);
                }
            }

            styleSelect.addEventListener('change', onChange);
            formatSelect.addEventListener('change', onChange);

        } else {
            // just download on click if no fetch api
            trigger.addEventListener('click', function () {
                let selectInfo = getSelectInfo(styleSelect, formatSelect);
                if (selectInfo.style.trim().length > 0 && selectInfo.format.trim().length > 0) {
                    let location = getDownloadLocation(type, selectInfo.format, selectInfo.style);
                    window.location.assign(location);
                }
            });
        }
    });
});