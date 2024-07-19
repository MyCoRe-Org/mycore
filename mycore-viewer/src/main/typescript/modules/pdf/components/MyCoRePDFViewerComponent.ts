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
import {PDFSettings} from "./PDFSettings";
import {PDFStructureBuilder} from "../widgets/PDFStructureBuilder";
import {MyCoReMap, Utils, ViewerFormatString} from "../../base/Utils";
import {StructureModelLoadedEvent} from "../../base/components/events/StructureModelLoadedEvent";
import {PDFStructureModel} from "../widgets/PDFStructureModel";
import {PDFPage} from "../widgets/PDFPage";
import {ViewerErrorModal} from "../../base/widgets/modal/ViewerErrorModal";
import {ShowContentEvent} from "../../base/components/events/ShowContentEvent";
import {ViewerBorderLayout} from "../../base/widgets/layout/ViewerBorderLayout";
import {MyCoReBasicToolbarModel} from "../../base/components/model/MyCoReBasicToolbarModel";
import {LanguageModel} from "../../base/components/model/LanguageModel";
import {WaitForEvent} from "../../base/components/events/WaitForEvent";
import {LanguageModelLoadedEvent} from "../../base/components/events/LanguageModelLoadedEvent";
import {ProvideToolbarModelEvent} from "../../base/components/events/ProvideToolbarModelEvent";
import {ViewerEvent} from "../../base/widgets/events/ViewerEvent";
import {RequestPageEvent} from "../../base/components/events/RequestPageEvent";
import {RequestTextContentEvent} from "../../base/components/events/RequestTextContentEvent";
import {ButtonPressedEvent} from "../../base/widgets/toolbar/events/ButtonPressedEvent";
import {ToolbarButton} from "../../base/widgets/toolbar/model/ToolbarButton";
import {getDocument, GlobalWorkerOptions, PDFDocumentProxy, PDFPageProxy} from "pdfjs-dist";


export class MyCoRePDFViewerComponent extends ViewerComponent {
    constructor(private _settings: PDFSettings, private container: JQuery) {
        super();
        var that = this;

    }

    private _structureBuilder: PDFStructureBuilder;
    private _pdfDocument: PDFDocumentProxy;
    private _pageCount: number;
    private _structure: PDFStructureModel = null;
    private _structureModelLoadedEvent: StructureModelLoadedEvent;
    private _pageCache: MyCoReMap<number, PDFPage> = new MyCoReMap<number, PDFPage>();
    private _pdfUrl: string;
    private _errorModalSynchronize = Utils.synchronize<MyCoRePDFViewerComponent>([(context: MyCoRePDFViewerComponent) => {
        return context._languageModel != null && context.error;
    }], (context: MyCoRePDFViewerComponent) => {
        var errorText = context._languageModel.getFormatedTranslation("noPDF", "<a href='mailto:"
            + this._settings.adminMail + "'>" + this._settings.adminMail + "</a>");
        var messageBoxTitle = context._languageModel.getTranslation("noPDFShort");
        new ViewerErrorModal(
            this._settings.mobile,
            messageBoxTitle, errorText, this._settings.webApplicationBaseURL + "/modules/iview2/img/sad-emotion-egg.jpg", this.container[0]).show();
        context.trigger(new ShowContentEvent(this, jQuery(), ViewerBorderLayout.DIRECTION_WEST, 0));
    });

    private error = false;

    private toolbarLanguageSync = Utils.synchronize<MyCoRePDFViewerComponent>([(_self) => _self._toolbarModel != null, (_self) => _self._languageModel != null], (_self) => {
        _self.addDownloadButton();
    });

    private _toolbarModel: MyCoReBasicToolbarModel = null;
    private _languageModel: LanguageModel = null;

    public init() {
        if (this._settings.doctype == "pdf") {
            this._pdfUrl = ViewerFormatString(this._settings.pdfProviderURL, {
                filePath: this._settings.filePath,
                derivate: this._settings.derivate
            });
            GlobalWorkerOptions.workerSrc = this._settings.pdfWorkerURL;
            var that = this;
            var pdfLocation = this._pdfUrl;
            getDocument({
                url: pdfLocation,
                disableAutoFetch: true,
                cMapUrl: this._settings.webApplicationBaseURL + "/modules/iview2/cmaps/",
                cMapPacked: true
            }).promise.then((pdfDoc: PDFDocumentProxy) => {
                this._pdfDocument = pdfDoc;
                that._structureBuilder = new PDFStructureBuilder(that._pdfDocument, this._settings.filePath);
                var promise = that._structureBuilder.resolve();
                promise.then((structure: PDFStructureModel) => {
                    that._structure = structure;
                    var smle = new StructureModelLoadedEvent(that, that._structure);
                    that._structureModelLoadedEvent = smle;
                    that._pageCount = structure._imageList.length;
                    that.trigger(smle);
                });

                promise.onreject((err: any) => {
                    this.error = true;
                    this._errorModalSynchronize(this);
                });

            }, function (errorReason) {
                console.log("error");
            });


            this.trigger(new WaitForEvent(this, ProvideToolbarModelEvent.TYPE));
            this.trigger(new WaitForEvent(this, LanguageModelLoadedEvent.TYPE));
        }

    }


    public handle(e: ViewerEvent) {
        if (e.type == RequestPageEvent.TYPE) {
            const rpe = e as RequestPageEvent;

            let pageID = rpe._pageId;
            if (!this._pageCache.has(Number(pageID))) {
                var promise = this._pdfDocument.getPage(Number(pageID));
                promise.then((page: PDFPageProxy) => {
                        var pdfPage = new PDFPage(rpe._pageId, page, this._structureBuilder);
                        this._pageCache.set(Number(rpe._pageId), pdfPage);
                        rpe._onResolve(rpe._pageId, pdfPage);
                    },
                    (reason: string) => {
                        console.error("PDF Page Request rejected");
                        console.error("Reason: " + reason);
                    });
            } else {
                rpe._onResolve(pageID, this._pageCache.get(Number(pageID)));
            }
        }

        if (e.type == RequestTextContentEvent.TYPE) {
            const rtce = e as RequestTextContentEvent;

            this.handle(new RequestPageEvent(this, rtce._href, (pageId, abstractPage) => {
                const page = abstractPage as PDFPage;
                page.resolveTextContent((tc) => {
                    rtce._onResolve(rtce._href, tc);
                });
            }));


        }

        if (e.type == ProvideToolbarModelEvent.TYPE) {
            const ptme = e as ProvideToolbarModelEvent;
            this._toolbarModel = ptme.model;
            this.toolbarLanguageSync(this);
        }

        if (e.type == LanguageModelLoadedEvent.TYPE) {
            var lmle = e as LanguageModelLoadedEvent;
            this._languageModel = lmle.languageModel;
            this.toolbarLanguageSync(this);
            this._errorModalSynchronize(this);
        }

        if (e.type == ButtonPressedEvent.TYPE) {
            var bpe = e as ButtonPressedEvent;
            if (bpe.button.id == "PdfDownloadButton") {
                window.location.assign(this._pdfUrl + "?dl");
            }
        }

        return;
    }

    public get handlesEvents(): string[] {
        if (this._settings.doctype == "pdf") {
            return [RequestPageEvent.TYPE, ProvideToolbarModelEvent.TYPE, LanguageModelLoadedEvent.TYPE, ButtonPressedEvent.TYPE, RequestTextContentEvent.TYPE];
        }

        return [];
    }

    private addDownloadButton() {
        this._toolbarModel._actionControllGroup.addComponent(new ToolbarButton("PdfDownloadButton", "", this._languageModel.getTranslation("toolbar.pdfDownload"), "download"));
    }

}
