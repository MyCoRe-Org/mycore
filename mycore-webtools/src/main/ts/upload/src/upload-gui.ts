/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */



import {FileTransferQueue} from "./FileTransferQueue";
import {FileTransfer} from "./FileTransfer";
import {Utils} from "./Utils";
import {I18N} from "./I18N";
import {TransferSession} from "./TransferSession";
interface FileTransferProgress {
    transfer: FileTransfer,
    loaded:number,
    total: number
}
export class FileTransferGUI {


    private _uploadBox: HTMLElement = null;
    private _transferElementMap: Map<FileTransfer, HTMLElement> = new Map<FileTransfer, HTMLElement>();
    private _entryFileTransferMap: Map<HTMLElement, FileTransfer> = new Map<HTMLElement, FileTransfer>();
    private transferProgressMap = new Map<FileTransfer, FileTransferProgress>();
    private runningCommitList: Array<TransferSession> = [];

    constructor() {
        this.registerEventHandler();
    }

    static start() {
        new FileTransferGUI();
    }

    private inititalizeBox(): void {
        this._uploadBox = Helper.htmlToElement(FileTransferGUITemplates.boxTemplate);
        this.translateElements();
        window.document.body.appendChild(this._uploadBox);

        (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-abort")).addEventListener('click', () => {
            FileTransferQueue.getQueue().abortAll();
        });

        this._uploadBox.querySelector('.minimize').addEventListener('click', () => {
            this._uploadBox.classList.toggle('minimized');
        });
    }

    private translateElements() {
        I18N.translateElements(this._uploadBox);
    }

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

        queue.addStartCommitHandler((session: TransferSession) => {
            this.handleCommitStartet(session);
        });

        queue.addCommitCompleteHandler((session: TransferSession, error: boolean, message: string, location: string) => {
            this.handleCommitCompleted(session, error, message, location);
        });

        queue.addProgressHandler((ft) => this.handleTransferProgress(ft));

        queue.addBeginSessionErrorHandler((session, message) => {
           this.handleSessionBeginError(session, message);
        });

        window.setInterval(() => {
            this.transferProgressMap.forEach((progress) => {
                this.setTransferProgress(this.getEntry(progress.transfer), progress.loaded, progress.total);
            });
        }, 250);

        let lastLoaded:Map<FileTransfer, number> = new Map<FileTransfer, number>();
        window.setInterval(() => {
            let allRate = 0;

            this.transferProgressMap.forEach((progress) => {
                if (lastLoaded.has(progress.transfer)) {
                    let bytesInSecond = (progress.loaded - lastLoaded.get(progress.transfer))*2;
                    allRate += bytesInSecond;
                    this.setTransferRate(this.getEntry(progress.transfer), bytesInSecond);
                }
                lastLoaded.set(progress.transfer, progress.loaded);
            })


            this.setAllProgress(allRate);
        }, 500);

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

    private getEntry(transfer: FileTransfer) {
        return this._transferElementMap.get(transfer);
    }

    private handleTransferCompleted(transfer: FileTransfer) {
        this.removeTransferEntry(transfer);
    }

    private removeTransferEntry(transfer: FileTransfer) {
        const entry = this.getEntry(transfer);
        this._entryFileTransferMap.delete(entry);
        entry.remove();
        if(this.transferProgressMap.has(transfer)){
            this.transferProgressMap.delete(transfer);
        }
    }

    private handleTransferError(transfer: FileTransfer) {
        this.getEntry(transfer).classList.add("bg-danger")
    }

    private handleTransferRestart(transfer: FileTransfer) {
        // no error handling yet
    }

    private handleTransferAbort(transfer: FileTransfer) {
        this.removeTransferEntry(transfer);
    }

    private handleTransferProgress(transfer: FileTransfer) {
        this.transferProgressMap.set(transfer,
            {
                transfer: transfer,
                loaded: transfer.loaded,
                total: transfer.total
            });
    }

    private setFileName(entry: HTMLElement, name: string) {
        (entry.querySelector(".mcr-upload-file-name") as HTMLElement).innerText = name;
    }

    private setTransferProgress(entry: HTMLElement, current: number, all: number) {
        if (entry instanceof HTMLElement) {
            const sizeStr = Helper.formatBytes(all, 1);
            const currentStr = Helper.formatBytes(current, 1);
            const percent = (current / all * 100).toPrecision(1);

            (<HTMLElement>entry.querySelector(".mcr-upload-file-size")).innerText = currentStr + " / " + sizeStr;
            const progressBarElement = (<HTMLElement>entry.querySelector(".mcr-upload-progressbar"));
            progressBarElement.style.width = percent + "%";
            progressBarElement.setAttribute("aria-valuenow", percent);
        }
    }

    private createFileTransferEntry(transfer: FileTransfer): HTMLElement {
        const newEntry = Helper.htmlToElement(FileTransferGUITemplates.entryTemplate);
        this._transferElementMap.set(transfer, newEntry);
        this.setFileName(newEntry, transfer.fileName);
        this._entryFileTransferMap.set(newEntry, transfer);
        (<HTMLElement>newEntry.querySelector(".mcr-upload-abort-transfer")).addEventListener("click", () => {
            FileTransferQueue.getQueue().abort(transfer);
        });

        return newEntry;
    }

    private setTransferRate(entry: HTMLElement, bytesInSecond: number) {
        if (entry instanceof HTMLElement) {
            const rateText = Helper.formatBytes(bytesInSecond, 1) + "/s";
            (entry.querySelector(".mcr-upload-transfer-rate") as HTMLElement).innerText = rateText;
        }
    }

    private setAllProgress(allRate: number) {
        if (this._uploadBox != null) {
            (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-rate")).innerText = Helper.formatBytes(allRate, 1) + "/s";
            (<HTMLElement>this._uploadBox.querySelector(".mcr-upload-transfer-all-progress")).innerText = FileTransferQueue.getQueue().getAllCount().toString();
        }
    }

    private handleCommitStartet(session: TransferSession) {
        this.runningCommitList.push(session);
        this.showCommitWarning(true);
    }

    private handleCommitCompleted(session: TransferSession, error: boolean, message: string, location: string) {
        this.runningCommitList.splice(this.runningCommitList.indexOf(session), 1);
        if (!error) {
            if (this.runningCommitList.length === 0) {
                this.showCommitWarning(false);

                if (!location) {
                    window.location.reload();
                } else {
                    window.location.assign(location);
                }
            }
        } else {
            this.showCommitWarning(false);
            this.showError(message);
        }
    }

    private showError(message: string) {
        const error = this._uploadBox.querySelector(".mcr-commit-error");
        if (error.classList.contains("d-none")) {
            error.classList.remove("d-none");
        }

        const errorMessageElement = error.querySelector(".mcr-error-message");
        errorMessageElement.textContent = message;
    }

    private showCommitWarning(show: boolean) {
        const warning = this._uploadBox.querySelector(".mcr-commit-warn");

        if (show && warning.classList.contains("d-none")) {
            warning.classList.remove("d-none");
        } else if (!show && !warning.classList.contains("d-none")) {
            warning.classList.add("d-none");
        }
    }

    private handleSessionBeginError(session: TransferSession, message: string) {
        this.showError(message);
    }
}

class Helper {

    static isString(s) {
        return typeof (s) === 'string' || s instanceof String;
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
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
        const i = Math.floor(Math.log(bytes) / Math.log(base));
        return parseFloat((bytes / Math.pow(base, i)).toFixed(dm)) + ' ' + sizes[i];
    }


}

class FileTransferGUITemplates {

    private static _boxTemplate =
        `<div class="mcr-upload">
    <div class="card" style="height: 100%;">
        <div class="card-header">
            <span class="mcr-upload-title" data-i18n="component.webtools.upload.title"></span>
            <span class="fas fa-window-minimize minimize float-right" style="font-size: 11px;line-height: 22px;"></span></div>
        <div class="card-body mcr-upload-entry-list" style="overflow-y:  scroll;">
            <div class="container-fluid">
                <div class="row d-none mcr-commit-error bg-danger">
                    <div class="col-12" data-i18n="component.webtools.upload.error"></div>
                    <div class="col-12 mcr-error-message" style="overflow: hidden; max-height: 200px"></div>
                </div>
                <div class="row d-none mcr-commit-warn bg-info">
                    <div class="col-12" data-i18n="component.webtools.upload.processing"></div>
                </div>
                <div class="row status">
                    <div class="col mcr-upload-transfer-all-progress"></div>
    
                    <small class="col-2 mcr-upload-transfer-all-rate"></small>
                    <div class="col-1">
                        <span class="text-danger fas fa-times mcr-upload-transfer-all-abort" style="cursor: pointer;"></span>
                    </div>
                </div>
                <div class="d-none mcr-upload-active-insert-marker"></div>
            </div>
        </div>
    </div>
</div>`;

    public static get boxTemplate(): string {
        return FileTransferGUITemplates._boxTemplate;
    }

    private static _entryTemplate = `<div class="entry row">
    <span class="col mcr-upload-file-name"></span>
    <small class="col-2 mcr-upload-file-size"></small>
    <small class="col-2 mcr-upload-transfer-rate"></small>
    <div class="col-1">
        <span class="text-danger fas fa-times mcr-upload-abort-transfer" style="cursor: pointer;"></span>
    </div>
    <div class="col-12 pb-2 pt-2">
        <div class="progress">
            <div class="progress-bar mcr-upload-progressbar" role="progressbar" aria-valuenow="0" aria-valuemin="0"
                 aria-valuemax="100" style="width:0%">
                <span class="sr-only mcr-upload-progress-sr"></span></div>
        </div>
    </div>
</div>`;

    public static get entryTemplate(): string {
        return FileTransferGUITemplates._entryTemplate;
    }

}



FileTransferGUI.start();
