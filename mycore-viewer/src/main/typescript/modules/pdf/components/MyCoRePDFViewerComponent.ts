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

/// <reference path="../definitions/pdf.d.ts" />
/// <reference path="PDFSettings.ts" />
/// <reference path="../widgets/PDFPage.ts" />
/// <reference path="../widgets/PDFStructureBuilder.ts" />

namespace mycore.viewer.components {

    export class MyCoRePDFViewerComponent extends ViewerComponent {
        constructor(private _settings:PDFSettings, private container:JQuery) {
            super();
            var that = this;

            // see MV-53
            (<any>PDFJS).disableAutoFetch=true;
        }

        private _structureBuilder:widgets.pdf.PDFStructureBuilder;
        private _pdfDocument:PDFDocumentProxy;
        private _pageCount:number;
        private _structure:widgets.pdf.PDFStructureModel = null;
        private _structureModelLoadedEvent:events.StructureModelLoadedEvent;
        private _pageCache:MyCoReMap<number, widgets.canvas.PDFPage> = new MyCoReMap<number, widgets.canvas.PDFPage>();
        private _pdfUrl:string;
        private _errorModalSynchronize = Utils.synchronize<MyCoRePDFViewerComponent>([ (context:MyCoRePDFViewerComponent)=> {
            return context._languageModel != null && context.error;
        } ], (context:MyCoRePDFViewerComponent)=> {
            var errorText = context._languageModel.getFormatedTranslation("noPDF", "<a href='mailto:"
                + this._settings.adminMail + "'>" + this._settings.adminMail + "</a>");
            var messageBoxTitle = context._languageModel.getTranslation("noPDFShort");
            new mycore.viewer.widgets.modal.ViewerErrorModal(
                this._settings.mobile,
                messageBoxTitle, errorText, this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg", this.container[0]).show();
            context.trigger(new mycore.viewer.components.events.ShowContentEvent(this, jQuery(), mycore.viewer.widgets.layout.IviewBorderLayout.DIRECTION_WEST, 0));
        });

        private error = false;

        private toolbarLanguageSync = Utils.synchronize<MyCoRePDFViewerComponent>([(_self)=> _self._toolbarModel != null, (_self) => _self._languageModel != null], (_self)=> {
            _self.addDownloadButton();
        });

        private _toolbarModel:model.MyCoReBasicToolbarModel = null;
        private _languageModel:model.LanguageModel = null;

        public init() {
            if (this._settings.doctype == "pdf") {
                this._pdfUrl = ViewerFormatString(this._settings.pdfProviderURL, { filePath: this._settings.filePath, derivate: this._settings.derivate });
                var workerURL = this._settings.pdfWorkerURL;
                PDFJS.workerSrc = workerURL;
                var that = this;
                var pdfLocation = this._pdfUrl;
                PDFJS.getDocument(pdfLocation).then((pdfDoc) => {
                    this._pdfDocument = <PDFDocumentProxy>pdfDoc;
                    that._structureBuilder = new mycore.viewer.widgets.pdf.PDFStructureBuilder(that._pdfDocument, this._settings.filePath);
                    var promise = that._structureBuilder.resolve();
                    promise.then((structure:widgets.pdf.PDFStructureModel) => {
                        that._structure = structure;
                        var smle = new events.StructureModelLoadedEvent(that, that._structure);
                        that._structureModelLoadedEvent = smle;
                        that._pageCount = structure._imageList.length;
                        that.trigger(smle);
                    });

                    promise.onreject((err:any)=> {
                        this.error = true;
                        this._errorModalSynchronize(this);
                    });

                }, function (errorReason) {
                    console.log("error");
                });


                this.trigger(new events.WaitForEvent(this, events.ProvideToolbarModelEvent.TYPE));
                this.trigger(new events.WaitForEvent(this, events.LanguageModelLoadedEvent.TYPE));
            }

        }


        public handle(e:mycore.viewer.widgets.events.ViewerEvent) {
            if (e.type == events.RequestPageEvent.TYPE) {
                var rpe = <events.RequestPageEvent> e;

                let pageID = rpe._pageId;
                if (!this._pageCache.has(Number(pageID))) {
                    var promise = this._pdfDocument.getPage(Number(pageID));
                    promise.then((page:PDFPageProxy) => {
                            var pdfPage = new widgets.canvas.PDFPage(rpe._pageId, page);
                            this._pageCache.set(Number(rpe._pageId), pdfPage);
                            rpe._onResolve(rpe._pageId, pdfPage);
                        },
                        (reason:string)=> {
                            console.error("PDF Page Request rejected");
                            console.error("Reason: " + reason);
                        });
                } else {
                    rpe._onResolve(pageID, this._pageCache.get(Number(pageID)));
                }
            }

            if (e.type == events.RequestTextContentEvent.TYPE) {
                var rtce = <events.RequestTextContentEvent>e;

                this.handle(new events.RequestPageEvent(this, rtce._href, (pageId, abstractPage)=> {
                    var page = <widgets.canvas.PDFPage> abstractPage;
                    page.resolveTextContent((tc) => {
                        rtce._onResolve(rtce._href, tc);
                    });
                }));


            }

            if (e.type == events.ProvideToolbarModelEvent.TYPE) {
                var ptme = <events.ProvideToolbarModelEvent> e;
                this._toolbarModel = ptme.model;
                this.toolbarLanguageSync(this);
            }

            if (e.type == events.LanguageModelLoadedEvent.TYPE) {
                var lmle = <events.LanguageModelLoadedEvent>e;
                this._languageModel = lmle.languageModel;
                this.toolbarLanguageSync(this);
                this._errorModalSynchronize(this);
            }

            if (e.type == widgets.toolbar.events.ButtonPressedEvent.TYPE) {
                var bpe = <widgets.toolbar.events.ButtonPressedEvent> e;
                if (bpe.button.id == "PdfDownloadButton") {
                    window.location.assign(this._pdfUrl + "?dl");
                }
            }

            return;
        }

        public get handlesEvents():string[] {
            if (this._settings.doctype == "pdf") {
                return [events.RequestPageEvent.TYPE, events.ProvideToolbarModelEvent.TYPE, events.LanguageModelLoadedEvent.TYPE, widgets.toolbar.events.ButtonPressedEvent.TYPE, events.RequestTextContentEvent.TYPE];
            }

            return [];
        }

        private addDownloadButton() {
            this._toolbarModel._actionControllGroup.addComponent(new mycore.viewer.widgets.toolbar.ToolbarButton("PdfDownloadButton", "", this._languageModel.getTranslation("toolbar.pdfDownload"), "download"));
        }

    }
}

addViewerComponent(mycore.viewer.components.MyCoRePDFViewerComponent);
