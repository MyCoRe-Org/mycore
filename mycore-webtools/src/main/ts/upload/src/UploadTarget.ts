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

import {FileTransfer} from "./FileTransfer";
import {FileTransferQueue} from "./FileTransferQueue";
import {Utils} from "./Utils";
import {TransferSession} from "./TransferSession";

/**
 * A possible target for file drag and drop.
 */
export class UploadTarget {

    // the path to a folder
    private target: string | null;
    private uploadHandler: string | null;
    private attributes: Record<string, string> = {};

    private readonly uploadTargetAttribute = "data-upload-target";

    private readonly uploadClassificationsAttribute = "data-upload-classifications";

    constructor(element: HTMLElement, manualToggle: HTMLElement) {
        this.target = element.getAttribute(this.uploadTargetAttribute);
        this.uploadHandler = element.getAttribute("data-upload-handler") || "Default";

        element.getAttributeNames().forEach((name) => {
            if(name.startsWith("data-upload-")) {
                this.attributes[name.substring("data-upload-".length)] = element.getAttribute(name) || "Default";
            }
        });

        const observer = new MutationObserver((mutations => {
            mutations.forEach((mutation) => {
                if (mutation.type === "attributes") {
                    if (this.uploadTargetAttribute === mutation.attributeName) {
                        this.target = element.getAttribute(this.uploadTargetAttribute);
                    }
                    if(mutation.attributeName.startsWith("data-upload-")) {
                        this.attributes[mutation.attributeName.substring("data-upload-".length)] =
                            element.getAttribute(mutation.attributeName);
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
            this.processDrop(event);
        });

        if (manualToggle != null) {
            manualToggle.addEventListener("click", (e) => {
                e.preventDefault();
                const fileInput = document.createElement("input");
                let testing = "mcr-testing" in window;
                if (testing) {
                    fileInput.setAttribute("id", "mcr-testing-file-input");
                    manualToggle.appendChild(fileInput);
                }



                fileInput.setAttribute("type", "file");
                fileInput.addEventListener('change', async () => {
                    let transferSession = FileTransferQueue.getQueue().registerTransferSession(this.uploadHandler,
                        this.attributes);
                    for (let i = 0; i < fileInput.files.length; i++) {
                        let file = fileInput.files.item(i);
                        FileTransferQueue.getQueue().setValidating(true);
                        const validation = await this.validateTraverse(file);
                        FileTransferQueue.getQueue().setValidating(false);
                        if (!validation.test) {
                            // TODO: show nice in GUI
                            alert(validation.reason);
                            return true;
                        }
                        const fileTransfer = new FileTransfer(file, this.target, transferSession, []);
                        FileTransferQueue.getQueue()
                        FileTransferQueue.getQueue().add(fileTransfer);
                    }
                });
                if (!testing) {
                    fileInput.click();
                }
            });
        }
    }

    private async processDrop(event: DragEvent): Promise<boolean> {
        event.stopPropagation();
        event.preventDefault();
        if ("dataTransfer" in event) {
            let items = event.dataTransfer.items;
            let ts = FileTransferQueue.getQueue().registerTransferSession(this.uploadHandler,this.attributes);

            const webkitEntries: any[] = [];
            for (let i = 0; i < items.length; i++) {
                webkitEntries.push(items[i].webkitGetAsEntry());
            }

            for (let i = 0; i < webkitEntries.length; i++) {
                const file = webkitEntries[i];
                FileTransferQueue.getQueue().setValidating(true);
                const validation = await this.validateTraverse(file);
                FileTransferQueue.getQueue().setValidating(false);
                if (!validation.test) {
                    // TODO: show nice in GUI
                    alert(validation.reason);
                    return true;
                }
            }
            for (let i = 0; i < webkitEntries.length; i++) {
                const file = webkitEntries[i];
                this.traverse(ts, file);
            }
        }

        return true;
    }

    private async validateTraverse(fileEntry: any): Promise<{ test: boolean, fileEntry: any, reason: string | null }> {
        if (fileEntry.isDirectory) {
            let children = await this.readEntries(fileEntry);
            const promises: Array<Promise<{ test: boolean, fileEntry: any, reason: string | null }>> = [];
            for (let childIndex in children) {
                const child = children[childIndex];
                const validation = this.validateTraverse(child);
                promises.push(validation);
            }

            const results = await Promise.all(promises);
            const failed = results.find((result) => !result.test);
            return failed || {test: true, fileEntry, reason: null};
        } else {
            const validation = await this.validateFile(fileEntry);
            return {test: validation.valid, fileEntry, reason: validation.reason};
        }
    }

    private async readEntries(fileEntry: any): Promise<any[]> {
        const reader = fileEntry.createReader();

        return new Promise((accept, reject) => {
            reader.readEntries((entries) => {
                accept(entries);
            }, (error) => {
                console.error(["Error while reading entry ", fileEntry, error])
                reject(error);
            });

        });
    }

    /**
     * This method sends the name and the size of a fileEntry to the server to validate them.
     * The client could lie, but the data will be also validated at the real upload. This is only to prevent the client
     * sending a 2GB file unnecessarily.
     * @param uploadId the upload id to which the file is added
     * @param fileEntry the entry which contains the name and size
     * @private
     */
    private async validateFile(fileEntry: File|FileSystemEntry): Promise<{ valid: boolean, reason: string | null }> {
        const size = await this.getEntrySize(fileEntry);
        return new Promise((accept, reject) => {
            const isFileSystemEntry = 'fullPath' in fileEntry;

            let uploadPath;
            if(isFileSystemEntry){
                uploadPath = fileEntry.fullPath[0] == '/' ? fileEntry.fullPath.substr(1) : fileEntry.fullPath;
            } else {
                uploadPath = fileEntry.name;
            }
            const url = Utils.getUploadSettings().webAppBaseURL + "rsc/files/upload/" + this.uploadHandler + "/" + uploadPath + "?size=" + size;
            const request = new XMLHttpRequest();
            request.open('GET', url, true);

            request.onreadystatechange = (result) => {
                if (request.readyState === 4)
                    if (request.status === 200) {
                        accept({valid: true, reason: null});
                    } else {
                        accept({valid: false, reason: request.responseText})
                    }
            }

            try {
                request.send();
            } catch (e){
            }
        });
    }

    /**
     * Returns the file size if the browser api supports it or -1 if not.
     * @param fileEntry the file entry
     * @private
     */
    private async getEntrySize(fileEntry: any): Promise<number> {
        return new Promise((accept, reject) => {
            if("getMetadata" in fileEntry){
                fileEntry.getMetadata((metadata) => {
                    accept(metadata.size);
                }, (err) => reject(err));
            } else {
                accept(-1);
            }
        });
    }

    private traverse(transferSession: TransferSession, fileEntry: any, parentTransfers: FileTransfer[] = []) {
        const fileTransfer = new FileTransfer(fileEntry, this.target, transferSession, parentTransfers);
        FileTransferQueue.getQueue().add(fileTransfer);

        if (fileEntry.isDirectory) {
            const reader = fileEntry.createReader();
            const newParentTransfers = parentTransfers.slice();
            newParentTransfers.push(fileTransfer);

            const errorCallback = (error) => {
                console.log(error);
            };

            const readEntry = (results) => {
                const result = results;

                result.forEach((e) => this.traverse(transferSession, e, newParentTransfers));
                if (result.length > 0) {
                    reader.readEntries(readEntry, errorCallback);
                }
            };

            reader.readEntries(readEntry, errorCallback);
        }
    }
}
