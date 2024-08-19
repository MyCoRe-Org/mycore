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


import {ViewerComponent} from "../../base/components/ViewerComponent";
import {MetsSettings} from "./MetsSettings";
import {ViewerPrintModalWindow} from "../widgets/modal/ViewerPrintModalWindow";
import {StructureImage} from "../../base/components/model/StructureImage";
import {StructureModel} from "../../base/components/model/StructureModel";
import {LanguageModel} from "../../base/components/model/LanguageModel";
import {ToolbarButton} from "../../base/widgets/toolbar/model/ToolbarButton";
import {ButtonPressedEvent} from "../../base/widgets/toolbar/events/ButtonPressedEvent";
import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {ImageChangedEvent} from "../../base/components/events/ImageChangedEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {ComponentInitializedEvent} from "../../base/components/events/ComponentInitializedEvent";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {StructureChapter} from "../../base/components/model/StructureChapter";
import {ViewerFormatString} from "../../base/Utils";

export class MyCoRePrintComponent extends ViewerComponent {

    constructor(private _settings: MetsSettings) {
        super();
    }

    private _modalWindow: ViewerPrintModalWindow;
    private _currentImage: StructureImage;
    private _structureModel: StructureModel;
    private _languageModel: LanguageModel;
    private _maxPages: number;
    private _printButton: ToolbarButton;

    private get _enabled() {
        return (this._settings.pdfCreatorStyle != null && this._settings.pdfCreatorStyle.length != 0) ||
            this._settings.pdfCreatorURI;
    }

    public get handlesEvents(): string[] {
        if (this._settings.doctype == 'mets' && this._enabled) {
            return [ButtonPressedEvent.TYPE, LanguageModelLoadedEvent.TYPE, StructureModelLoadedEvent.TYPE, ImageChangedEvent.TYPE, ProvideToolbarModelEvent.TYPE];
        } else {
            return [];
        }
    }

    public init() {
        if (this._settings.doctype == 'mets' && this._enabled) {
            this._resolveMaxRequests();
            this.initModalWindow();

            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, StructureModelLoadedEvent.TYPE));
            this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
            this.trigger(new ComponentInitializedEvent(this));
        }
    }

    public handle(e: ViewerEvent) {
        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            this._printButton = new ToolbarButton("PrintButton", "PDF", "", "");


            this._printButton.icon = "file-pdf";
            this._printButton.label = "";

            if (ptme.model.name == "MyCoReFrameToolbar") {
                ptme.model._zoomControllGroup.addComponent(this._printButton);
            } else {
                ptme.model._actionControllGroup.addComponent(this._printButton);
            }
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            const languageModelLoadedEvent = e as LanguageModelLoadedEvent;
            const languageModel = languageModelLoadedEvent.languageModel;
            this._printButton.tooltip = languageModel.getTranslation("toolbar.pdf");
            this._modalWindow.closeLabel = languageModel.getTranslation("createPdf.cancel");
            this._modalWindow.currentPageLabel = languageModel.getTranslation("createPdf.range.currentPage");
            this._modalWindow.allPagesLabel = languageModel.getTranslation("createPdf.range.allPages");
            this._modalWindow.rangeLabel = languageModel.getTranslation("createPdf.range.manual");
            this._modalWindow.chapterLabel = languageModel.getTranslation("createPdf.range.chapter");
            this._modalWindow.title = languageModel.getTranslation("createPdf.title");
            this._languageModel = languageModel;
            this._modalWindow.maximalPageMessage = languageModel.getTranslation("createPdf.maximalPages");
        }

        if (e.type == ButtonPressedEvent.TYPE) {
            const bpe = e as ButtonPressedEvent;
            if (bpe.button.id == "PrintButton") {
                if (this._settings.doctype == 'pdf') {
                    window.location.href = this._settings.metsURL;
                } else {
                    this._modalWindow.show();
                }
            }
        }

        if (e.type == StructureModelLoadedEvent.TYPE) {
            const smle = e as StructureModelLoadedEvent;
            this._structureModel = smle.structureModel;
            this._modalWindow.setChapterTree(this.getChapterViewModel());
        }

        if (e.type == ImageChangedEvent.TYPE) {
            const ice = e as ImageChangedEvent;
            this._currentImage = ice.image;
            if (this._modalWindow.currentChecked) {
                if (typeof this._currentImage != "undefined") {
                    this._currentImage.requestImgdataUrl((url: string) => {
                        this._modalWindow.previewImageSrc = url;
                    });
                }
            }

        }

    }

    public getChapterViewModel(chapter: StructureChapter = this._structureModel.rootChapter, indent: number = 0): Array<{
        id: string;
        label: string
    }> {
        const chapterVM = [];

        let indentStr = "";
        for (let i = 0; i < indent; i++) {
            indentStr += "&nbsp;&nbsp;";
        }

        let combinedLabel = indentStr + chapter.label;
        let MAX_LENGHT = 25 + indentStr.length;
        if (combinedLabel.length > MAX_LENGHT) {
            combinedLabel = combinedLabel.substr(0, MAX_LENGHT) + "...";
        }
        chapterVM.push({id: chapter.id, label: combinedLabel});

        const indentIncr = indent + 1;
        for (const childChapter of chapter.chapter) {
            chapterVM.push.apply(chapterVM, this.getChapterViewModel(childChapter, indentIncr));
        }

        return chapterVM;
    }

    private initModalWindow() {
        this._modalWindow = new ViewerPrintModalWindow(this._settings.mobile);

        this._modalWindow.checkEventHandler = (wich: string) => {
            if (wich == "range") {
                this.handleRangeChecked();
            } else if (wich == "chapter") {
                this.handleChapterChecked();
            } else {
                this._modalWindow.rangeInputEventHandler = null;
                this._modalWindow.chapterInputEventHandler = null;
                this._modalWindow.rangeInputEnabled = false;

                if (wich == "all") {
                    this.handleAllChecked();
                } else if (wich == "current") {
                    this.handleCurrentChecked();
                }

            }
        };

        this._modalWindow.okayClickHandler = () => {
            let page;

            if (this._modalWindow.currentChecked) {
                page = this._currentImage.order;
            }
            if (this._modalWindow.allChecked) {
                page = "1-" + this._structureModel._imageList.length;
            }
            if (this._modalWindow.rangeChecked) {
                page = this._modalWindow.rangeInputVal;
            }

            if (this._modalWindow.chapterChecked) {
                let chapter = this.findChapterWithID(this._modalWindow.chapterInputVal + "");
                page = this.getRangeOfChapter(chapter);
            }

            window.location.href = this.buildPDFRequestLink(page);
        };
        this._modalWindow.currentChecked = true;
    }

    private handleChapterChecked() {
        this._modalWindow.rangeInputEnabled = false;
        this._modalWindow.validationMessage = "";
        this._modalWindow.previewImageSrc = null;

        this._modalWindow.chapterInputEventHandler = (chapterID: string) => {
            let chapter = this.findChapterWithID(chapterID);
            let range: string = this.getRangeOfChapter(chapter);
            const validationResult = this.validateRange(range);

            if (validationResult.valid) {
                this._modalWindow.validationMessage = "";
                this._modalWindow.validationResult = true;
                this._structureModel.imageList[validationResult.firstPage].requestImgdataUrl((url: string) => {
                    this._modalWindow.previewImageSrc = url;
                });
            } else {
                this._modalWindow.validationMessage = validationResult.text;
                this._modalWindow.validationResult = validationResult.valid;
                this._modalWindow.previewImageSrc = null;
            }
        };

        this._modalWindow.chapterInputEventHandler(this._modalWindow.chapterInputVal + "");
    }

    private handleCurrentChecked() {
        this._modalWindow.validationMessage = "";
        this._currentImage.requestImgdataUrl((url: string) => {
            this._modalWindow.previewImageSrc = url;
        });

        this._modalWindow.validationResult = true;
    }

    private handleAllChecked() {
        const allCount = this._structureModel.imageList.length + 1;
        const maxRange = this._maxPages;
        if (allCount > maxRange) {
            this._modalWindow.validationMessage = this._languageModel.getTranslation("createPdf.errors.tooManyPages");
            this._modalWindow.validationResult = false;
            this._modalWindow.previewImageSrc = null;
        } else {
            this._modalWindow.validationResult = true;
            this._structureModel.imageList[0].requestImgdataUrl((url: string) => {
                this._modalWindow.previewImageSrc = url;
            });
        }
    }

    private handleRangeChecked() {
        this._modalWindow.rangeInputEnabled = true;
        this._modalWindow.validationMessage = "";
        this._modalWindow.previewImageSrc = null;

        this._modalWindow.rangeInputEventHandler = (ip: string) => {
            const validationResult = this.validateRange(ip);

            if (validationResult.valid) {
                this._modalWindow.validationMessage = "";
                this._modalWindow.validationResult = true;
                this._structureModel.imageList[validationResult.firstPage].requestImgdataUrl((url: string) => {
                    this._modalWindow.previewImageSrc = url;
                });
            } else {
                this._modalWindow.validationMessage = validationResult.text;
                this._modalWindow.validationResult = validationResult.valid;
                this._modalWindow.previewImageSrc = null;
            }
        };
    }

    private buildPDFRequestLink(pages?: string) {
        let metsLocationFormatString = "{metsURL}/mets.xml?XSL.Style={pdfCreatorStyle}";
        let defaultFormatString = "{pdfCreatorURI}?mets={metsLocation}&pages={pages}";

        const metsLocation = encodeURIComponent(ViewerFormatString(metsLocationFormatString, this._settings));

        this._settings["metsLocation"] = metsLocation;
        this._settings["pages"] = pages;

        return ViewerFormatString(this._settings.pdfCreatorFormatString || defaultFormatString, this._settings);
    }

    private buildRestrictionLink() {
        let defaultFormatString = "{pdfCreatorURI}?getRestrictions";
        return ViewerFormatString(this._settings.pdfCreatorRestrictionFormatString || defaultFormatString, this._settings);
    }

    private _resolveMaxRequests() {
        const that = this;
        jQuery.ajax({
            type: 'GET',
            dataType: 'json',
            url: this.buildRestrictionLink(),
            crossDomain: true,
            complete: function (jqXHR, textStatus) {
                //jQuery.support.cors = corsSupport;
            },
            success: function (data: any) {
                that._maxPages = parseInt(data.maxPages);
                that._modalWindow.maximalPages = that._maxPages.toString();
            }
        });
    }

    private findChapterWithID(id: string, chapter: StructureChapter = this._structureModel.rootChapter): StructureChapter {
        if (chapter.id == id) return chapter;
        for (const child of chapter.chapter) {
            let foundChapter = this.findChapterWithID(id, child);
            if (foundChapter != null) {
                return foundChapter;
            }
        }
        return null;
    }

    /**
     * Validates the range input
     * ranges: range+;
     * range:  page | pageRange;
     * pageRange: page + ' '* + '-' + ' '* + page;
     * page: [0-10]+;
     *
     * @param {string} range
     * @returns {{valid: boolean; text: string; firstPage?: number}}
     */
    private validateRange(range: string): {
        valid: boolean; text: string
        firstPage?: number
    } {
        const ranges = range.split(",");
        let firstPage = 99999;

        if (range.length == 0) {
            return {valid: false, text: this._languageModel.getTranslation("createPdf.errors.noPages")}
        }
        let pc = 0;
        let maxRange = this._maxPages;
        for (const range of ranges) {
            // check page or pageRange
            if (range.indexOf("-") == -1) {
                // page
                if (!this.isValidPage(range)) {
                    return {
                        valid: false,
                        text: this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                    };
                }
                const page = parseInt(range);
                if (page < firstPage) {
                    firstPage = page;
                }
                pc++;
            } else {
                const pages = range.split("-");
                if (pages.length != 2) {
                    return {
                        valid: false,
                        text: this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                    };
                }

                const startPageString = pages[0];
                const endPageString = pages[1];

                if (!this.isValidPage(startPageString)) {
                    const msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.rangeInvalid"), {"0": startPageString});
                    return {valid: false, text: msg};
                }

                if (!this.isValidPage(endPageString)) {
                    const msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.rangeInvalid"), {"0": endPageString});
                    return {valid: false, text: msg};
                }

                const startPage = parseInt(startPageString);
                const endPage = parseInt(endPageString);

                if (startPage >= endPage) {
                    return {
                        valid: false,
                        text: this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                    };
                }

                pc += endPage - startPage;
                if (pc > maxRange) {
                    const msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.tooManyPages"), {"0": maxRange.toString()});
                    return {valid: false, text: msg};
                }

                if (startPage < firstPage) {
                    firstPage = startPage;
                }
            }
        }

        return {valid: true, text: "", firstPage: firstPage - 1};
    }

    private isValidPage(page: string) {
        if (typeof this._structureModel._imageList[parseInt(page) - 1] != "undefined") {
            return !isNaN(<any>page);
        }
        return false;
    }


    private getRangeOfChapter(chapter: StructureChapter) {
        let imageToChapterMap = this._structureModel._imageToChapterMap;

        let ranges = [];

        const chapterEqOrContains = (root: StructureChapter, child: StructureChapter) => {
            if (root == child) {
                return true;
            }
            if (child.parent != null) {
                return chapterEqOrContains(root, child.parent);
            }

            return false;
        };

        let start = null;
        let last = null;

        for (const img of this._structureModel.imageList) {
            if (imageToChapterMap.has(img.id)) {
                let linkedChapter = imageToChapterMap.get(img.id);

                if (chapterEqOrContains(chapter, linkedChapter)) {
                    if (start == null) {
                        start = img;
                    } else {
                        last = img;
                    }
                    continue;
                }
            }

            // case end current
            if (start != null && last != null) {
                ranges.push(`${start.order}-${last.order}`);
            } else if (start != null) {
                ranges.push((start.order) + "");
            } else {
            }
            start = last = null;
        }

        if (start != null && last != null) {
            ranges.push(`${start.order + 1}-${last.order + 1}`);
        } else if (start != null) {
            ranges.push((start.order + 1) + "");
        } else {
        }


        return ranges.join(",")
    }


}


