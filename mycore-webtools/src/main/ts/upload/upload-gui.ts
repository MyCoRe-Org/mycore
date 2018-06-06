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

namespace mycore.upload {


    export class FileTransferGUI {


        constructor() {
            this.registerEventHandler();
        }

        private _uploadBox: HTMLElement = null;
        private _idEntryMap: {} = {};

        private inititalizeBox(): void {
            this._uploadBox = Helper.htmlToElement(FileTransferGUITemplates.boxTemplate);
            this.translateElements();
            window.document.body.appendChild(this._uploadBox);

            (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-abort")).addEventListener('click', ()=>{
                FileTransferQueue.getQueue().abortAll();
            });
        }

        private translateElements() {
            I18N.translateElements(this._uploadBox);
        }

        private completedCount: number = 0;

        private registerEventHandler(): void {
            const queue = FileTransferQueue.getQueue();

            queue.addAddedHandler((ft) => {
                this.handleTransferAdded(ft);
            });

            queue.addStartedHandler((ft) => {
                this.handleTransferStarted(ft);
            });

            queue.addCompleteHandler((ft) => {
                this.handleTransferCompleted(ft);
            });

            queue.addErrorHandler((ft) => {
                this.handleTransferError(ft);
            });

            queue.addRestartHandler((ft) => {
                this.handleTransferRestart(ft);
            });

            queue.addAbortHandler((ft) => {
                this.handleTransferAbort(ft);
            });

            queue.addStartCommitHandler((uploadID:string)=>{
                this.handleCommitStartet(uploadID);
            });

            queue.addCommitCompleteHandler((uploadID:string)=>{
                this.handleCommitCompleted(uploadID);
            });

            queue.addProgressHandler((ft)=> this.handleTransferProgress(ft));

            window.setInterval(()=>{
                for(let id in this.tpMap){
                    let progress = this.tpMap[id];
                    this.setTransferProgress(this.getEntry(progress.transfer), progress.loaded, progress.total );
                }
            }, 500);

            let lastLoaded = {};
            window.setInterval(() => {
                let allRate = 0;
                for (let id in this.tpMap) {
                    let progress = this.tpMap[ id ];
                    if (id in lastLoaded) {
                        let bytesInSecond = progress.loaded - lastLoaded[ id ];
                        allRate += bytesInSecond;
                        this.setTransferRate(this.getEntry(progress.transfer), bytesInSecond);
                    }
                    lastLoaded[ id ] = progress.loaded;
                }

                this.setAllProgress(allRate);

            }, 1000);

        }

        private handleTransferAdded(transfer: FileTransfer) {
            if (this._uploadBox == null) {
                this.inititalizeBox();
            }

            const newEntry = this.createFileTransferEntry(transfer);
            const entryList = this.getEntryListElement();
            entryList.appendChild(newEntry);
        }

        private getEntryListElement() {
            return <HTMLElement>this._uploadBox.querySelector(".mcr-upload-entry-list");
        }

        private getActiveInsertMarkerElement() {
            return <HTMLElement>this._uploadBox.querySelector(".mcr-upload-active-insert-marker");
        }

        private handleTransferStarted(transfer: FileTransfer) {
            const entry = this.getEntry(transfer);
            entry.remove();

            const markerElement = this.getActiveInsertMarkerElement();
            markerElement.parentElement.insertBefore(entry, markerElement);
        }

        private getEntry(transfer: mycore.upload.FileTransfer) {
            return <HTMLElement>this._idEntryMap[transfer.transferID];
        }

        private handleTransferCompleted(transfer: FileTransfer) {
            this.removeTransferEntry(transfer);
        }

        private removeTransferEntry(transfer: mycore.upload.FileTransfer) {
            const entry = this.getEntry(transfer);
            entry.remove();
            delete this.tpMap[ transfer.transferID ];
        }

        private handleTransferError(transfer: FileTransfer) {
            this.getEntry(transfer).classList.add("bg-danger")
        }

        private handleTransferRestart(transfer: FileTransfer) {
            // no error handling yet
        }

        private handleTransferAbort(transfer: FileTransfer) {
            if (transfer.transferID in this.tpMap) {
                delete this.tpMap[ transfer.transferID ];
            }
            this.removeTransferEntry(transfer);
        }

        private tpMap = {};

        private handleTransferProgress(transfer: FileTransfer) {
            const transID = transfer.transferID;
            this.tpMap[ transID ] =
                {
                    transfer : transfer,
                    loaded : transfer.loaded,
                    total : transfer.total
                };
        }

        private setFileName(entry: HTMLElement | string, name: string) {
            if (entry instanceof HTMLElement) {
                (<HTMLElement>entry.querySelector(".mcr-upload-file-name")).innerText = name;
            } else if (Helper.isString(entry)) {
                this.setFileName(this._idEntryMap[entry], name);
            }
        }

        private setTransferProgress(entry: HTMLElement | string, current: number, all: number) {
            if (entry instanceof HTMLElement) {
                const sizeStr = Helper.formatBytes(all, 1);
                const currentStr = Helper.formatBytes(current, 1);
                const percent = (current / all * 100).toPrecision(1);

                (<HTMLElement>entry.querySelector(".mcr-upload-file-size")).innerText = currentStr + " / " + sizeStr;
                const progressBarElement = (<HTMLElement> entry.querySelector(".mcr-upload-progressbar"));
                progressBarElement.style.width = percent + "%";
                progressBarElement.setAttribute("aria-valuenow", percent);
            } else if (Helper.isString(entry)) {
                this.setFileName(this._idEntryMap[entry], name);
            }
        }

        private createFileTransferEntry(transfer: FileTransfer): HTMLElement {
            const newEntry = Helper.htmlToElement(FileTransferGUITemplates.entryTemplate);
            this._idEntryMap[transfer.transferID] = newEntry;
            this.setFileName(newEntry, transfer.fileName);

            newEntry.setAttribute("data-id", transfer.transferID);

            (<HTMLElement>newEntry.querySelector(".mcr-upload-abort-transfer")).addEventListener("click", () => {
                FileTransferQueue.getQueue().abort(transfer);
            });

            return newEntry;
        }

        static start() {
            new FileTransferGUI();
        }

        private setTransferRate(entry: HTMLElement | string, bytesInSecond: number) {
            if (entry instanceof HTMLElement) {
                (<HTMLElement>entry.querySelector(".mcr-upload-transfer-rate")).innerText = Helper.formatBytes(bytesInSecond, 1) + "/s";
            } else if (Helper.isString(entry)) {
                this.setFileName(this._idEntryMap[ entry ], name);
            }
        }

        private setAllProgress(allRate: number) {
            if (this._uploadBox != null) {
                (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-rate")).innerText = Helper.formatBytes(allRate, 1) + "/s";
                (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-progress")).innerText = FileTransferQueue.getQueue().getAllCount().toString();
            }
        }

        private runningCommitList:Array<string> = [];

        private handleCommitStartet(uploadID: string) {
            this.runningCommitList.push(uploadID);
            this.showCommitWarning(true);
        }

        private handleCommitCompleted(uploadID: string) {
            this.runningCommitList.splice(this.runningCommitList.indexOf(uploadID), 1);
            if(this.runningCommitList.length===0){
                this.showCommitWarning(false);
                window.location.reload();
            }
        }

        private showCommitWarning(show:boolean) {
            const warning = this._uploadBox.querySelector(".mcr-commit-warn");

            if(show && warning.classList.contains("hidden")){
                warning.classList.remove("hidden");
            } else if(!show && !warning.classList.contains("hidden")){
                warning.classList.add("hidden");
            }
        }
    }

    class Helper {

        static isString(s) {
            return typeof(s) === 'string' || s instanceof String;
        }

        static htmlToElement(html: string): HTMLElement {
            const template = document.createElement('template');
            html = html.trim();
            template.innerHTML = html;

            return <HTMLElement>template.content.firstElementChild;
        }

        static formatBytes(bytes: number, decimals?: number) {
            if (bytes == 0) return '0 Bytes';
            const base = 1024;
            const dm = decimals || 2;
            const sizes = [ 'Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB' ];
            const i = Math.floor(Math.log(bytes) / Math.log(base));
            return parseFloat((bytes / Math.pow(base, i)).toFixed(dm)) + ' ' + sizes[ i ];
        }


    }

    class FileTransferGUITemplates {

        public static get entryTemplate(): string {
            return FileTransferGUITemplates._entryTemplate;
        }

        public static get boxTemplate(): string {
            return FileTransferGUITemplates._boxTemplate;
        }


        private static _boxTemplate =
            `<div class="mcr-upload">
    <div class="panel panel-default" style="height: 100%;">
        <div class="panel-heading">
            <span class="mcr-upload-title" data-i18n="component.webtools.upload.title"></span>
            <span class="fa fa-window-minimize pull-right" style="font-size: 11px;line-height: 22px;"></span></div>
        <div class="panel-body mcr-upload-entry-list" style="overflow-y:  scroll;">
            <div class="row hidden mcr-commit-warn bg-info">
                <div class="col-md-12" data-i18n="component.webtools.upload.processing"></div>
            </div>
            <div class="row status">
                <div class="col-md-8 mcr-upload-transfer-all-progress"></div>

                <small class="col-md-3 mcr-upload-transfer-all-rate"></small>
                <div class="col-md-1">
                    <span class="text-danger fa fa-remove mcr-upload-transfer-all-abort"></span>
                </div>
            </div>
            <div class="hidden mcr-upload-active-insert-marker"></div>
        </div>
    </div>
</div>`;


        private static _entryTemplate = `<div class="entry row">
    <span class="col-md-5 mcr-upload-file-name"></span>
    <small class="col-md-3 mcr-upload-file-size"></small>
    <small class="col-md-3 mcr-upload-transfer-rate"></small>
    <div class="col-md-1">
        <span class="text-danger fa fa-remove mcr-upload-abort-transfer"></span>
    </div>
    <div class="col-md-12">
        <div class="progress">
            <div class="progress-bar mcr-upload-progressbar" role="progressbar" aria-valuenow="0" aria-valuemin="0"
                 aria-valuemax="100" style="width:0%">
                <span class="sr-only mcr-upload-progress-sr"></span></div>
        </div>
    </div>
</div>`;

    }

    class I18N {
        private static DEFAULT_FETCH_LEVEL = 1;

        private static keyObj = {};

        private static fetchKeyHandlerList = {};

        private static currentLanguage: string = null;

        static translate(key: string, callback: (translation: string) => void) {
            let baseUrl: string = Utils.getUploadSettings().webAppBaseURL;
            let resourceUrl = baseUrl + "rsc/locale/translate/" + this.getCurrentLanguage() + "/";

            if (key in I18N.keyObj) {
                callback(I18N.keyObj[key]);
            } else {
                let fetchKey = key;
                if (key.indexOf(".") != -1) {
                    fetchKey = key.split('.', I18N.DEFAULT_FETCH_LEVEL).join(".") + "*";
                }

                let wrappedCallback = () => key in I18N.keyObj ? callback(I18N.keyObj[key]) : callback("???" + key + "???");

                if (fetchKey in I18N.fetchKeyHandlerList) {
                    I18N.fetchKeyHandlerList[fetchKey].push(wrappedCallback);
                } else {
                    I18N.fetchKeyHandlerList[fetchKey] = [wrappedCallback];

                    let xhttp = new XMLHttpRequest();
                    xhttp.onreadystatechange = () => {
                        if (xhttp.readyState === XMLHttpRequest.DONE && xhttp.status == 200) {
                            let jsonData = JSON.parse(xhttp.response);
                            for (let key in jsonData) {
                                I18N.keyObj[key] = jsonData[key];
                            }

                            for (let index in I18N.fetchKeyHandlerList[fetchKey]) {
                                I18N.fetchKeyHandlerList[fetchKey][index]()
                            }
                            delete I18N.fetchKeyHandlerList[fetchKey];
                        }
                    };
                    xhttp.open('GET', resourceUrl + fetchKey, true);
                    xhttp.send();
                }
            }
        }

        static translateElements(element: HTMLElement) {
            Array.prototype.slice.call(element.querySelectorAll("[data-i18n]")).forEach(childElement => {
                let child = <HTMLElement>childElement;
                let attr = child.getAttribute("data-i18n");
                I18N.translate(attr, translation => {
                    child.innerHTML = translation;
                });
            })
        }

        static getCurrentLanguage(): string {
            return window["mcrLanguage"];
        }
    }

    FileTransferGUI.start();


}
