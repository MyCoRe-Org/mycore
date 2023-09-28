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

import {Utils} from "./Utils";
import {FileTransfer} from "./FileTransfer";
import {FileTransferHandler} from "./FileTransferHandler";
import {TransferSession} from "./TransferSession";
import {TransferSesssionHandler} from "./TransferSesssionHandler";

export class FileTransferQueue {

    private static _singleton: FileTransferQueue;

    private beginSessionStartedHandlerList: Array<TransferSesssionHandler> = [];

    private beginSessionFinishedHandlerList: Array<TransferSesssionHandler> = [];

    private beginSessionErrorHandlerList: Array<TransferSesssionHandler> = [];

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

    private commitHandlerList: Array<(transfer: TransferSession, error: boolean, err?: string, location?: string) => void> = [];

    private commitStartHandlerList: Array<(transfer: TransferSession) => void> = [];


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

        this.startTransfers();
    }

    public registerTransferSession(uploadHandler: string, parameter: Record<string, string>) {
        return new TransferSession(uploadHandler, parameter);
    }

    public begin(session: TransferSession) {
        this.beginSessionStartedHandlerList.forEach((handler) => {
            handler(session);
        });
        session.start(()=> {
            this.startTransfers();
            this.beginSessionFinishedHandlerList.forEach((handler) => {
                handler(session);
            });
        }, (message: string) => {
            this.beginSessionErrorHandlerList.forEach((handler) => {
                handler(session, message);
            });
        });
    }

    public add(transfer: FileTransfer) {
        this.newFileTransferList.push(transfer);
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

    public addStartCommitHandler(handler: (session: TransferSession) => void) {
        this.commitStartHandlerList.push(handler);
    }

    public addCommitCompleteHandler(handler: (transfer: TransferSession, error: boolean, err?: string, location?: string) => void) {
        this.commitHandlerList.push(handler);
    }

    public addBeginSessionStartedHandler(handler: (session: TransferSession) => void) {
        this.beginSessionStartedHandlerList.push(handler);
    }

    public addBeginSessionFinishedHandler(handler: (session: TransferSession) => void) {
        this.beginSessionFinishedHandlerList.push(handler);
    }

    public addBeginSessionErrorHandler(handler: (session: TransferSession, message: string) => void) {
        this.beginSessionErrorHandlerList.push(handler);
    }

    public abortAll() {
        this.newFileTransferList.forEach((tr) => {
            this.abort(tr);
        });
        this.pendingFileTransferList.forEach((tr) => {
            this.abort(tr);
        })
    }

    private getNextPossibleTransfer() {
        for (const transfer of this.newFileTransferList) {
            if(!transfer.transferSession.started && !transfer.transferSession.startPending) {
                this.begin(transfer.transferSession);
            } else if(!transfer.started && transfer.transferSession.started) {
                const isIncompleteOrErrored = (requires) => !requires.complete || requires.error;
                if (!transfer.requires.some(isIncompleteOrErrored)) {
                    return transfer;
                }
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
                newTransfer.start(
                    this.buildTransferCompleteHandler(newTransfer),
                    this.buildTransferErrorHandler(newTransfer),
                    this.getTransferProgress(newTransfer)
                );
                this.startedHandlerList.forEach((handler) => {
                    handler(newTransfer);
                });
                startedCount++
            } else {
                break
            }
        }
    }

    private transferWithSessionLeft(session: TransferSession) {
        for (const transfer of this.newFileTransferList) {
            if (transfer.transferSession == session) {
                return true;
            }
        }
        for (const transfer of this.pendingFileTransferList) {
            if (transfer.transferSession == session) {
                return true;
            }
        }
    }

    private buildTransferCompleteHandler(transfer: FileTransfer): () => void {
        return () => {
            this.removePending(transfer);
            this.completeHandlerList.forEach((handler) => {
                handler(transfer);
            });

            if(!this.transferWithSessionLeft(transfer.transferSession)) {
                this.commitTransfer(transfer.transferSession);
            }

            this.startTransfers();
        };
    }

    private buildTransferErrorHandler(transfer: FileTransfer): () => void {
        return () => {
            this.removePending(transfer);
        };
    }

    private getTransferProgress(transfer: FileTransfer): () => void {
        return () => {
            this.progressHandlerList.forEach(handler => handler(transfer));
        };
    }

    private removePending(transfer: FileTransfer) {
        const transferIndex = this.pendingFileTransferList.indexOf(transfer);
        this.pendingFileTransferList.splice(transferIndex, 1);
    }

    private removeNew(transfer: FileTransfer) {
        const transferIndex = this.newFileTransferList.indexOf(transfer);
        this.newFileTransferList.splice(transferIndex, 1);
    }

    private commitTransfer(transfer: TransferSession) {
        this.commitStartHandlerList.forEach(handler => handler(transfer));
        transfer.commit((location) => {
            this.commitHandlerList.forEach(handler => handler(transfer, false, null, location))
        }, (message: string) => {
            this.commitHandlerList.forEach(handler => handler(transfer, true, message));
        });
    }
}