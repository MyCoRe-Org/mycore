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

/// <reference path="MetsSettings.ts" />
/// <reference path="../widgets/modal/IviewPrintModalWindow.ts" />

namespace mycore.viewer.components {
    export class MyCoRePrintComponent extends ViewerComponent {

        constructor(private _settings: MetsSettings) {
            super();
        }

        private _modalWindow: widgets.modal.IviewPrintModalWindow;
        private _currentImage: model.StructureImage;
        private _structureModel: model.StructureModel;
        private _languageModel: model.LanguageModel;
        private _maxPages: number;
        private _printButton: widgets.toolbar.ToolbarButton;
        private _enabled = (this._settings.pdfCreatorStyle != null && this._settings.pdfCreatorStyle.length != 0) ||
            this._settings.pdfCreatorURI;

        private buildPDFRequestLink(pages?: string) {
            let metsLocationFormatString = "{metsURL}/mets.xml?XSL.Style={pdfCreatorStyle}";
            let defaultFormatString = "{pdfCreatorURI}?mets={metsLocation}&pages={pages}";

            let metsLocation = encodeURIComponent(ViewerFormatString(metsLocationFormatString, this._settings));

            this._settings[ "metsLocation" ] = metsLocation;
            this._settings[ "pages" ] = pages;

            return ViewerFormatString(this._settings.pdfCreatorFormatString || defaultFormatString, this._settings);
        }

        private buildRestrictionLink(){
            let defaultFormatString = "{pdfCreatorURI}?getRestrictions";
            return ViewerFormatString(this._settings.pdfCreatorRestrictionFormatString || defaultFormatString, this._settings);
        }

        public init() {
            if (this._settings.doctype == 'mets' && this._enabled) {
                this._resolveMaxRequests();
                this._modalWindow = new mycore.viewer.widgets.modal.IviewPrintModalWindow(this._settings.mobile);
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.StructureModelLoadedEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
                this.trigger(new events.ComponentInitializedEvent(this));
            }
        }


        public handle(e: mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent>e;
                this._printButton = new widgets.toolbar.ToolbarButton("PrintButton", "PDF", "", "");


                if (this._settings.mobile) {
                    this._printButton.icon = "file-pdf-o";
                    this._printButton.label = "";
                }
                if(ptme.model.name == "MyCoReFrameToolbar"){
                    ptme.model._zoomControllGroup.addComponent(this._printButton);
                } else {
                    ptme.model._actionControllGroup.addComponent(this._printButton);
                }
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var languageModelLoadedEvent = <events.LanguageModelLoadedEvent>e;
                this._printButton.tooltip = languageModelLoadedEvent.languageModel.getTranslation("toolbar.pdf");
                this._modalWindow.closeLabel = languageModelLoadedEvent.languageModel.getTranslation("createPdf.cancel");
                this._modalWindow.currentPageLabel = languageModelLoadedEvent.languageModel.getTranslation("createPdf.range.currentPage");
                this._modalWindow.allPagesLabel = languageModelLoadedEvent.languageModel.getTranslation("createPdf.range.allPages");
                this._modalWindow.rangeLabel = languageModelLoadedEvent.languageModel.getTranslation("createPdf.range.manual");
                this._modalWindow.title = languageModelLoadedEvent.languageModel.getTranslation("createPdf.title");
                this._languageModel = languageModelLoadedEvent.languageModel;
                this._modalWindow.maximalPageMessage = languageModelLoadedEvent.languageModel.getTranslation("createPdf.maximalPages")
                var that = this;

                this._modalWindow.checkEventHandler = (wich: string) => {
                    if (wich == "range") {
                        that._modalWindow.rangeInputEnabled = false;
                        this._modalWindow.validationMessage = "";
                        that._modalWindow.previewImageSrc = null;

                        that._modalWindow.rangeInputEventHandler = (ip: string) => {
                            var validationResult = that.validateRange(ip);

                            if (validationResult.valid) {
                                that._modalWindow.validationMessage = "";
                                that._modalWindow.validationResult = true;
                                that._structureModel.imageList[ validationResult.firstPage ].requestImgdataUrl((url: string) => {
                                    that._modalWindow.previewImageSrc = url;
                                });
                            } else {
                                that._modalWindow.validationMessage = validationResult.text;
                                that._modalWindow.validationResult = validationResult.valid;
                                that._modalWindow.previewImageSrc = null;
                            }
                        };
                    } else {
                        that._modalWindow.rangeInputEventHandler = null;
                        that._modalWindow.rangeInputEnabled = false;

                        if (wich == "all") {
                            var allCount = this._structureModel.imageList.length + 1;
                            var maxRange = this._maxPages;
                            if (allCount > maxRange) {
                                var msg = that._languageModel.getTranslation("createPdf.errors.tooManyPages");
                                that._modalWindow.validationMessage = msg;
                                that._modalWindow.validationResult = false;
                                that._modalWindow.previewImageSrc = null;
                            } else {
                                that._modalWindow.validationResult = true;
                                that._structureModel.imageList[ 0 ].requestImgdataUrl((url: string) => {
                                    that._modalWindow.previewImageSrc = url;
                                });
                            }
                        } else if (wich == "current") {
                            that._modalWindow.validationMessage = "";
                            this._currentImage.requestImgdataUrl((url: string) => {
                                that._modalWindow.previewImageSrc = url;
                            });

                            that._modalWindow.validationResult = true;
                        }

                    }
                };

                this._modalWindow.okayClickHandler = () => {

                    var page;

                    if (that._modalWindow.currentChecked) {
                        page = that._currentImage.order;
                    }
                    if (that._modalWindow.allChecked) {
                        page = "1-" + that._structureModel._imageList.length;
                    }
                    if (that._modalWindow.rangeChecked) {
                        page = that._modalWindow.rangeInputVal;
                    }

                    window.location.href = that.buildPDFRequestLink(page);
                };
                this._modalWindow.currentChecked = true;

            }

            if (e.type == widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var bpe = <widgets.toolbar.events.ButtonPressedEvent>e;
                if (bpe.button.id == "PrintButton") {
                    if (this._settings.doctype == 'pdf') {
                        window.location.href = this._settings.metsURL;
                    } else {
                        this._modalWindow.show();
                    }
                }
            }

            if (e.type == events.StructureModelLoadedEvent.TYPE) {
                var smle = <events.StructureModelLoadedEvent> e;
                this._structureModel = smle.structureModel;
            }

            if (e.type == events.ImageChangedEvent.TYPE) {
                var ice = <events.ImageChangedEvent>e;
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

        private _resolveMaxRequests() {
            var that = this;
            jQuery.ajax({
                type : 'GET',
                dataType : 'json',
                url : this.buildRestrictionLink(),
                crossDomain : true,
                complete : function (jqXHR, textStatus) {
                    //jQuery.support.cors = corsSupport;
                },
                success : function (data: any) {
                    that._maxPages = parseInt(data.maxPages);
                    that._modalWindow.maximalPages = that._maxPages.toString();
                }
            });
        }

        /**
         * Validates the range input
         * ranges: range+;
         * range:  page | pageRange;
         * pageRange: page + ' '* + '-' + ' '* + page;
         * page: [0-10]+;
         *
         * @param range
         * @returns {valid:boolean;text:string;firstPage?:number}
         */
        private validateRange(range: string): {
            valid: boolean; text: string
            firstPage?: number
        } {
            var ranges = range.split(",");
            var firstPage = 99999;

            if (range.length == 0) {
                return {valid : false, text : this._languageModel.getTranslation("createPdf.errors.noPages")}
            }
            var pc = 0;
            var maxRange = this._maxPages;
            for (var rangeIndex in ranges) {
                var range = ranges[ rangeIndex ];
                // check page or pageRange


                if (range.indexOf("-") == -1) {
                    // page
                    if (!this.isValidPage(range)) {
                        return {
                            valid : false,
                            text : this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                        };
                    }
                    var page = parseInt(range);
                    if (page < firstPage) {
                        firstPage = page;
                    }
                    pc++;
                    continue;
                } else {
                    var pages = range.split("-");
                    if (pages.length != 2) {
                        return {
                            valid : false,
                            text : this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                        };
                    }

                    var startPageString = pages[ 0 ];
                    var endPageString = pages[ 1 ];

                    if (!this.isValidPage(startPageString)) {
                        var msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.rangeInvalid"), {"0" : startPageString});
                        return {valid : false, text : msg};
                    }

                    if (!this.isValidPage(endPageString)) {
                        var msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.rangeInvalid"), {"0" : endPageString});
                        return {valid : false, text : msg};
                    }

                    var startPage = parseInt(startPageString);
                    var endPage = parseInt(endPageString);

                    if (startPage >= endPage) {
                        return {
                            valid : false,
                            text : this._languageModel.getTranslation("createPdf.errors.rangeInvalid")
                        };
                    }

                    pc += endPage - startPage;
                    if (pc > maxRange) {
                        var msg = ViewerFormatString(this._languageModel.getTranslation("createPdf.errors.tooManyPages"), {"0" : maxRange.toString()});
                        return {valid : false, text : msg};
                    }

                    if (startPage < firstPage) {
                        firstPage = startPage;
                    }

                    continue;
                }
            }

            return {valid : true, text : "", firstPage : firstPage - 1};
        }

        private isValidPage(page: string) {
            if (typeof this._structureModel._imageList[ parseInt(page) - 1 ] != "undefined") {
                return !isNaN(<any>page);
            }
            return false;
        }

        public get handlesEvents(): string[] {
            if (this._settings.doctype == 'mets' && this._enabled) {
                return [ widgets.toolbar.events.ButtonPressedEvent.TYPE, events.LanguageModelLoadedEvent.TYPE, events.StructureModelLoadedEvent.TYPE, events.ImageChangedEvent.TYPE, events.ProvideToolbarModelEvent.TYPE ];
            } else {
                return [];
            }
        }


    }
}

addViewerComponent(mycore.viewer.components.MyCoRePrintComponent);
