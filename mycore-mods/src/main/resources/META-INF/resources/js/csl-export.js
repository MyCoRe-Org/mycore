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
const STANDALONE_FORMATS = ["mods", "bibtex", "endnote", "ris", "isi", "mods2csv"];
const SOLR_STANDALONE_FORMATS = ["solr2csv"];
const FORMAT_MAP = {
    "solr2csv": "csv",
    "mods2csv": "mods2csv",
    "mods": "xml",
    "bibtex": "xml"
}

function isStandaloneFormat(format) {
    return STANDALONE_FORMATS.indexOf(format) !== -1;
}

function isSolrStandaloneFormat(format) {
    return SOLR_STANDALONE_FORMATS.indexOf(format) !== -1;
}

function getSolrDownloadLocation(transformer) {
    let location = window.location.href;
    let transformerQuery = "&XSL.Transformer=" + transformer+"&fl=*";
    let hashIndex = location.indexOf("#");
    if (hashIndex !== -1) {
        location = location.substring(0, hashIndex);
    }

    return location + transformerQuery;
}

function getCSLDownloadLocation(type, format, style) {
    let location = window.location.href;
    let transformerQuery = "&XSL.Transformer=" + type + "-csl-" + format;
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
    return location + transformerQuery + "&XSL.style=" + style;
}

function getDownloadLocation(type, selectInfo) {
    let format = selectInfo.format;
    let style = selectInfo.style;
    if (isStandaloneFormat(format)) {
        return window["webApplicationBaseURL"] + "servlets/MCRExportServlet?basket=objects&transformer=" + format;
    }
    if (isSolrStandaloneFormat(format)) {
        return getSolrDownloadLocation(format);
    }

    return getCSLDownloadLocation(type, format, style);
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

function getFileEnding(initialSelectInfo) {
    if(initialSelectInfo.format in FORMAT_MAP) {
        return FORMAT_MAP[initialSelectInfo.format];
    } else {
        return initialSelectInfo.format;
    }
}

function createFetchFunction(trigger, initialSelectInfo, styleSelect, formatSelect) {
    // this function will be called when the request is started
    return function (response) {
        // this function will be called if the the request is started

        if (!response.ok) {
            throw new Error("Network response was not ok");
        }

        response.blob().then(function (blob) {
            // check if the select has changed to different style or format since request start
            let newSelectInfo = getSelectInfo(styleSelect, formatSelect);
            if (newSelectInfo.format === initialSelectInfo.format &&
                (isStandaloneFormat(newSelectInfo.format) || newSelectInfo.style === initialSelectInfo.style)) {
                trigger.href = URL.createObjectURL(blob);
                trigger.download = "export." + getFileEnding(initialSelectInfo);
                setTriggerLoadingState(trigger, false);
                changeTriggerState(trigger, true);
            }
        });
    };
}

function updateStyleSelect(format, styleSelect) {
    if (isStandaloneFormat(format) || isSolrStandaloneFormat(format)) {
        styleSelect.classList.add("d-none");
    } else {
        styleSelect.classList.remove("d-none");
    }
}

window.addEventListener('load', function () {
    let list = document.querySelectorAll("[data-export]");
    let array = Array.prototype.slice.call(list);

    array.forEach(function (element) {
        let type = element.getAttribute("data-export");
        const styleSelect = element.querySelector("[name='style']");
        const formatSelect = element.querySelector("[name='format']");
        const trigger = element.querySelector("[data-trigger-export='true']");

        changeTriggerState(trigger, false);
        let onChange = function () {
            let selectInfo = getSelectInfo(styleSelect, formatSelect);
            updateStyleSelect(selectInfo.format, styleSelect);
            changeTriggerState(trigger, false);
            if (selectInfo.format.length > 0 && (selectInfo.style.length > 0 ||
                isStandaloneFormat(selectInfo.format) ||
                isSolrStandaloneFormat(selectInfo.format))) {
                setTriggerLoadingState(trigger, true);
                let location = getDownloadLocation(type, selectInfo);
                fetch(location, {method: "GET"})
                    .then(createFetchFunction(trigger, selectInfo, styleSelect, formatSelect))
                    .catch(function (error) {
                        throw error;
                    });
            } else {
                changeTriggerState(trigger, false);
            }
        };

        styleSelect.addEventListener("change", onChange);
        formatSelect.addEventListener("change", onChange);
    });
});