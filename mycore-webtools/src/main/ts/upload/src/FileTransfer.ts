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

import {Utils} from "./Utils";
import {TransferSession} from "./TransferSession";

export class FileTransfer {

    private aborted: boolean = false;
    private completeHandler: () => void;
    private errorHandler: () => void;
    private progressHandler: () => void;
    private request: XMLHttpRequest;

    constructor(private _entry: any,
                private _target: string,
                private _transferSession: TransferSession,
                public requires: Array<FileTransfer> = []) {
    }

    get fileName(): string {
        return (this._entry instanceof File) ? this._entry.name : this._entry.fullPath;
    }

    get transferSession(): TransferSession {
        return this._transferSession;
    }

    get entry(): any {
        return this._entry;
    }

    get target(): string {
        return this._target;
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
        const isDirectory = !(this._entry instanceof File) && this._entry.isDirectory;
        if (this._entry instanceof File) {
            uploadPath = this.entry.name;
        } else {
            uploadPath = this._entry.fullPath[0] == '/' ? this._entry.fullPath.substr(1) : this._entry.fullPath;
        }

        this.request = new XMLHttpRequest();

        this.request.open('PUT', Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/" +
            this.transferSession.bucketID + this.target + uploadPath + "?isDirectory=" + isDirectory, true);

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
