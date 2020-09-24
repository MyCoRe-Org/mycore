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

    export interface UploadSettings {
        webAppBaseURL: string;
    }

    export class Utils {
        static getUploadSettings(): UploadSettings {
            const uploadSettingsName = "mycoreUploadSettings";

            if (!(uploadSettingsName in window)) {
                throw new Error(uploadSettingsName + " is not defined in Window!");
            }
            return <UploadSettings>window[uploadSettingsName];
        }
    }

    /**
     * A possible target for file drag and drop.
     */
    export class UploadTarget {

        // the path to a folder
        private target: string | null;
        private uploadHandler: string | null;
        private object: string | null;
        private classifications: string | null;

        private readonly uploadTargetAttribute = "data-upload-target";

        private readonly uploadClassificationsAttribute = "data-upload-classifications";

        constructor(element: HTMLElement, manualToggle: HTMLElement) {
            this.target = element.getAttribute(this.uploadTargetAttribute);
            this.object = element.getAttribute("data-upload-object");
            this.uploadHandler = element.getAttribute("data-upload-handler");
            this.classifications = element.getAttribute(this.uploadClassificationsAttribute);

            const observer = new MutationObserver((mutations => {
                mutations.forEach((mutation) => {
                    if (mutation.type === "attributes") {
                        if (this.uploadTargetAttribute === mutation.attributeName) {
                            this.target = element.getAttribute(this.uploadTargetAttribute);
                        }
                        if (this.uploadClassificationsAttribute === mutation.attributeName) {
                            this.classifications = element.getAttribute(this.uploadClassificationsAttribute);
                        }
                    }
                })
            }));

            observer.observe(element, {attributes: true});

            element.addEventListener('dragover', (e: DragEvent) => {
                e.stopPropagation();
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
                element.classList.add("dragover");
            });

            element.addEventListener('dragleave', (e: DragEvent) => {
                element.classList.remove("dragover");
            });

            element.addEventListener("drop", (event: DragEvent) => {
                event.stopPropagation();
                event.preventDefault();
                if ("dataTransfer" in event) {
                    let items = event.dataTransfer.items;
                    const uploadID = (Math.random() * 10000).toString(10);

                    for (let i = 0; i < items.length; i++) {
                        const file = <WebKitFileEntry>items[i].webkitGetAsEntry();
                        this.traverse(file, uploadID, this.object);
                    }
                }
            });

            if (manualToggle != null) {
                manualToggle.addEventListener("click", (e) => {
                    e.preventDefault();
                    const fileInput = document.createElement("input");
                    const uploadID = (Math.random() * 10000).toString(10);

                    fileInput.setAttribute("type", "file");
                    fileInput.addEventListener('change', () => {
                        for (let i = 0; i < fileInput.files.length; i++) {
                            let file = fileInput.files.item(i);
                            const fileTransfer = new FileTransfer(file, this.target, uploadID, this.object, [], this.uploadHandler, this.classifications);
                            FileTransferQueue.getQueue().add(fileTransfer);
                        }
                    });
                    fileInput.click();
                });
            }
        }

        private traverse(fileEntry: WebKitFileEntry, uploadID: string, object: string, parentTransfers: FileTransfer[] = []) {
            const fileTransfer = new FileTransfer(fileEntry, this.target, uploadID, object, parentTransfers, this.uploadHandler, this.classifications);
            FileTransferQueue.getQueue().add(fileTransfer);

            if (fileEntry.isDirectory) {
                const reader: WebKitDirectoryReader = (<any>fileEntry).createReader();
                const newParentTransfers = parentTransfers.slice();
                newParentTransfers.push(fileTransfer);

                const errorCallback = (error) => {
                    console.log(error);
                };

                const readEntry = (results) => {
                    const result: WebKitFileEntry[] = (<any>results);

                    result.forEach((e) => this.traverse(e, uploadID, object, newParentTransfers));
                    if (result.length > 0) {
                        reader.readEntries(readEntry, errorCallback);
                    }
                };

                reader.readEntries(readEntry, errorCallback);
            }
        }
    }

    export class FileTransferQueue {

        private static _singleton: FileTransferQueue;
        /**
         * List of all completed file handlers.
         * @type {FileTransferHandler[]}
         */
        private completeHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all started event handlers.
         * @type {FileTransferHandler[]}
         */
        private startedHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all add event handlers.
         * @type {FileTransferHandler[]}
         */
        private addedHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all restart event handler.
         * @type {FileTransferHandler[]}
         */
        private restartHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all error event handler.
         * @type {FileTransferHandler[]}
         */
        private errorHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all progress event handler.
         * @type {FileTransferHandler[]}
         */
        private progressHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all abort event handler.
         * @type {FileTransferHandler[]}
         */
        private abortHandlerList: Array<FileTransferHandler> = [];

        /**
         * List of all not started file tranfers.
         * @type {FileTransferHandler[]}
         */
        private newFileTransferList: Array<FileTransfer> = [];
        /**
         * List of all pending file transfers
         * @type {FileTransfer[]}
         */
        private pendingFileTransferList: Array<FileTransfer> = [];

        private commitHandlerList: Array<(uploadID: string, error: boolean, err?: string) => void> = [];

        private commitStartHandlerList: Array<(uploadID: string) => void> = [];


        /**
         * How much parallel uploads.
         * @type {number}
         */
        private MAX_PENDING_SIZE = 5;


        /**
         * Counts how much transfers are left for a specific upload id.
         */
        private uploadIDCount: {} = {};

        constructor() {
        }

        /**
         * gets the file queue singleton
         * @returns {any | mycore.upload.FileTransferQueue}
         */
        public static getQueue() {
            return FileTransferQueue._singleton || (FileTransferQueue._singleton = new FileTransferQueue());
        }

        public getAllCount() {
            return this.newFileTransferList.length + this.pendingFileTransferList.length;
        }

        public getPendingCount() {
            return this.pendingFileTransferList.length;
        }

        public abort(transfer: FileTransfer) {
            let list = this.pendingFileTransferList;
            let index = this.pendingFileTransferList.indexOf(transfer);
            if (index != -1) {
                transfer.abort();
            } else {
                list = this.newFileTransferList;
                index = list.indexOf(transfer);
            }

            if (index == -1) {
                return;
            }

            list.splice(index, 1);

            this.abortHandlerList.forEach(abortHandler => {
                abortHandler(transfer);
            });

            // Abort also all transfers which depend of this (important for Folders)
            [this.newFileTransferList, this.pendingFileTransferList].forEach(possibleList => {
                possibleList.filter(otherTransfer => {
                    return otherTransfer.requires.indexOf(transfer) != -1;
                }).forEach(otherTransferToAbort => {
                    this.abort(otherTransferToAbort);
                });
            });

            this.decreaseCountForID(transfer.uploadID);
            this.startTransfers();
        }

        public add(transfer: FileTransfer) {
            this.newFileTransferList.push(transfer);
            this.increaseCountForID(transfer.uploadID);
            this.addedHandlerList.forEach((handler) => {
                handler(transfer);
            });
            this.startTransfers();
        }

        public addCompleteHandler(handler: FileTransferHandler) {
            this.completeHandlerList.push(handler);
        }

        public addAddedHandler(handler: FileTransferHandler) {
            this.addedHandlerList.push(handler);
        }

        public addStartedHandler(handler: FileTransferHandler) {
            this.startedHandlerList.push(handler);
        }

        public addRestartHandler(handler: FileTransferHandler) {
            this.restartHandlerList.push(handler);
        }

        public addErrorHandler(handler: FileTransferHandler) {
            this.errorHandlerList.push(handler);
        }

        public addProgressHandler(handler: FileTransferHandler) {
            this.progressHandlerList.push(handler);
        }

        public addAbortHandler(handler: FileTransferHandler) {
            this.abortHandlerList.push(handler);
        }

        public addStartCommitHandler(handler: (uploadID: string) => void) {
            this.commitStartHandlerList.push(handler);
        }

        public addCommitCompleteHandler(handler: (uploadID: string, error: boolean, err?: string) => void) {
            this.commitHandlerList.push(handler);
        }

        public abortAll() {
            this.newFileTransferList.forEach((tr) => {
                this.abort(tr);
            });
            this.pendingFileTransferList.forEach((tr) => {
                this.abort(tr);
            })
        }

        private getCountForUploadID(id: string) {
            if (!(id in this.uploadIDCount)) {
                this.uploadIDCount[id] = 0;
            }

            return this.uploadIDCount[id];
        }

        private increaseCountForID(id: string) {
            this.uploadIDCount[id] = this.getCountForUploadID(id) + 1;
        }

        private decreaseCountForID(id: string) {
            this.uploadIDCount[id] = this.getCountForUploadID(id) - 1;
        }

        private getNextPossibleTransfer() {
            for (const transfer of this.newFileTransferList) {
                const isIncompleteOrErrored = (requires) => !requires.complete || requires.error;
                if (!transfer.requires.some(isIncompleteOrErrored)) {
                    return transfer;
                }
            }
            return null;
        }

        private startTransfers() {
            let canStartCount = Math.max(0, Math.min(this.MAX_PENDING_SIZE - this.pendingFileTransferList.length, this.newFileTransferList.length));

            for (let startedCount = 0; startedCount < canStartCount;) {
                let newTransfer = this.getNextPossibleTransfer();
                if (newTransfer != null) {
                    this.removeNew(newTransfer);
                    this.pendingFileTransferList.push(newTransfer);
                    newTransfer.start(this.getTransferComplete(newTransfer), this.getTransferError(newTransfer), this.getTransferProgress(newTransfer));
                    this.startedHandlerList.forEach((handler) => {
                        handler(newTransfer);
                    });
                    startedCount++
                } else {
                    break
                }
            }
        }

        private getTransferComplete(transfer: FileTransfer): () => void {
            return () => {
                this.removePending(transfer);
                this.decreaseCountForID(transfer.uploadID);
                this.completeHandlerList.forEach((handler) => {
                    handler(transfer);
                });
                if (this.getCountForUploadID(transfer.uploadID) == 0) {
                    this.commitTransfer(transfer.uploadID, transfer.uploadHandler, transfer.classifications);
                }
                this.startTransfers();
            };
        }

        private getTransferError(transfer: FileTransfer): () => void {
            return () => {
                this.decreaseCountForID(transfer.uploadID);
                this.removePending(transfer);
            };
        }

        private getTransferProgress(transfer: FileTransfer): () => void {
            return () => {
                this.progressHandlerList.forEach(handler => handler(transfer));
            };
        }

        private removePending(transfer: mycore.upload.FileTransfer) {
            const transferIndex = this.pendingFileTransferList.indexOf(transfer);
            this.pendingFileTransferList.splice(transferIndex, 1);
        }

        private removeNew(transfer: mycore.upload.FileTransfer) {
            const transferIndex = this.newFileTransferList.indexOf(transfer);
            this.newFileTransferList.splice(transferIndex, 1);
        }

        private commitTransfer(uploadID: string, uploadHandler: string = null, classifications: string = null) {
            const xhr = new XMLHttpRequest();
            const uploadHandlerParameter = (uploadHandler != null) ? "&uploadHandler=" + uploadHandler : "";
            const classificationsParameter = (classifications != null) ? "&classifications=" + classifications : "";
            const basicURL = Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/commit?uploadID=" + uploadID;

            this.commitStartHandlerList.forEach(handler => handler(uploadID));

            xhr.open('PUT', basicURL + uploadHandlerParameter + classificationsParameter, true);
            xhr.onload = (result) => {
                if (xhr.status === 204 || xhr.status === 201 || xhr.status == 200) {
                    this.commitHandlerList.forEach(handler => handler(uploadID, false));
                } else {
                    let message;
                    switch (xhr.responseType) {
                        case "document":
                            message = xhr.responseXML.querySelector("message").textContent
                            break;
                        case "text":
                            message = xhr.responseText;
                            break;
                        default:
                            message = xhr.statusText;
                    }
                    this.commitHandlerList.forEach(handler => handler(uploadID, true, message))
                }


            };

            xhr.send();
        }
    }

    export interface FileTransferHandler {
        (fileTransfer: FileTransfer): void;
    }

    export class FileTransfer {

        private aborted: boolean = false;
        private completeHandler: () => void;
        private errorHandler: () => void;
        private progressHandler: () => void;
        private request: XMLHttpRequest;

        constructor(private _entry: WebKitFileEntry | File,
                    private _target: string,
                    private _uploadID: string,
                    private _targetObject: string,
                    public requires: Array<FileTransfer> = [],
                    private _uploadHandler: string = null,
                    private _classifications: string = null) {
            this._transferID = (Math.random() * 1000).toString();
        }

        get fileName(): string {
            return (this._entry instanceof File) ? this._entry.name : this._entry.fullPath;
        }

        get uploadHandler(): string {
            return this._uploadHandler;
        }

        get uploadID(): string {
            return this._uploadID;
        }

        get entry(): WebKitFileEntry | File {
            return this._entry;
        }

        get target(): string {
            return this._target;
        }

        get targetObject(): string {
            return this._targetObject;
        }

        get classifications(): string {
            return this._classifications;
        }

        private _error: boolean = false;

        get error(): boolean {
            return this._error;
        }

        private _started: boolean = false;

        get started(): boolean {
            return this._started;
        }

        private _complete: boolean = false;

        get complete(): boolean {
            return this._complete;
        }

        private _loaded: number = 0;

        get loaded(): number {
            return this._loaded;
        }

        private _total: number = 0;

        get total(): number {
            return this._total;
        }

        private _transferID: string;

        get transferID(): string {
            return this._transferID;
        }

        public abort() {
            this.aborted = true;
            if (this.request != null) {
                this.request.abort();
            }
        }

        public start(completeHandler?: () => void, errorHandler?: () => void, progressHandler?: () => void): void {
            this.completeHandler = completeHandler;
            this.errorHandler = errorHandler;
            this.progressHandler = progressHandler;

            this._started = true;

            if (this._entry instanceof File) {
                this.send(this._entry);
            } else {
                if (this._entry.isDirectory) {
                    if (!this.aborted) {
                        this.send();
                    }
                } else {
                    this._entry.file((f) => {
                        if (this.aborted) {
                            return;
                        }
                        const file: File = <any>f;
                        this.send(file);
                    }, () => {
                        this._error = true;
                        if (this.errorHandler) {
                            this.errorHandler();
                        }
                    });
                }
            }


        }

        public send(file?: File): void {
            let uploadPath;
            if (this._entry instanceof File) {
                uploadPath = this.entry.name;
            } else {
                uploadPath = this._entry.fullPath[0] == '/' ? this._entry.fullPath.substr(1) : this._entry.fullPath;

            }

            this.request = new XMLHttpRequest();

            this.request.open('PUT', Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/" + this.targetObject + this.target + uploadPath + "?uploadID=" + this._uploadID, true);

            this.request.onreadystatechange = (result) => {
                if (this.request.readyState === 4 && this.request.status === 204) {
                    this._complete = true;
                    if (this.completeHandler) {
                        this.completeHandler();
                    }
                }
            };

            this.request.upload.onprogress = (ev) => {
                if (ev.lengthComputable) {
                    this._loaded = ev.loaded;
                    this._total = ev.total;
                }

                if (this.progressHandler) {
                    this.progressHandler();
                }
            };

            this.request.onerror = () => {
                if (this.errorHandler) {
                    this.errorHandler();
                }
            };

            try {

                if (typeof file != "undefined") {
                    this.request.send(file);
                } else {
                    this.request.send();
                }
            } catch (e) {
                if (this.errorHandler) {
                    this.errorHandler();
                }
            }

        }

    }

    /**
     * Creates a UploadTarget for every element with the attribute data-upload-target
     * @param {HTMLElement} element
     */
    export function enable(element: HTMLElement) {
        (<HTMLElement[]>Array.prototype.slice.call(element.querySelectorAll("[data-upload-target]")))
            .forEach(element => {
                const fileBoxToggle = element.querySelector(".mcr-upload-show");
                new UploadTarget(element, <HTMLElement>fileBoxToggle);
            })
    }

}
