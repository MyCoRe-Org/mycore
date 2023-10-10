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

export class TransferSession {
    private request: XMLHttpRequest;
    private _complete: boolean = false;
    private _bucketID: string;
    private _started: boolean = false;
    private _startPending: boolean = false;

    constructor(private _uploadHandler:string, private _parameters: Record<string, string>) {
    }

    get bucketID(): string {
        return this._bucketID;
    }

    get uploadHandler(): string {
        return this._uploadHandler;
    }

    get parameter(): Record<string, string> {
        return this._parameters;
    }

    get complete(): boolean {
        return this._complete;
    }

    get started(): boolean {
        return this._started;
    }

    get startPending(): boolean {
        return this._startPending;
    }

    public start(completionHandler: () => void, errorHandler: (message) => void) {
        this.request = new XMLHttpRequest();
        this._startPending = true;
        const uploadHandlerParameter = "?uploadHandler=" + this.uploadHandler;

        let parameters = "";
        if(this.parameter != null) {
            parameters = "&" + Object.keys(this.parameter)
                .map((key) => encodeURIComponent(key) +"=" + encodeURIComponent(this.parameter[key])).join("&");
        }

        this.request.open('PUT', Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/begin" + uploadHandlerParameter + parameters, true);

        this.request.onreadystatechange = (result) => {
            if (this.request.readyState === 4 && this.request.status === 200) {
                this._bucketID = this.request.responseText;
                this._started = true;
                if (completionHandler) {
                    completionHandler();
                }
            }
        };

        this.request.onerror = (evt) => {
            if (errorHandler) {
                errorHandler(this.request.responseText);
            }
        };

        this.request.send();
    }

    public commit(completionHandler: (location:string|null) => void, errorhandler: (message: string)=> void) {
        const xhr = new XMLHttpRequest();
        const basicURL = Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/"+this.bucketID+"/commit";

        xhr.open('PUT', basicURL, true);
        xhr.onload = (result) => {
            if (xhr.status === 204 || xhr.status === 201 || xhr.status == 200) {
                this._complete = true;
                completionHandler(xhr.getResponseHeader("Location"));
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
                errorhandler(message);
            }
        };
        xhr.send();
    }

}
