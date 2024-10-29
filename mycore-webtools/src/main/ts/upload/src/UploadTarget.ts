/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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


                fileInput.multiple = true;
                fileInput.setAttribute("type", "file");
                fileInput.addEventListener('change', async () => {
                    const queue = FileTransferQueue.getQueue();
                    let transferSession = queue.registerTransferSession(this.uploadHandler,
                        this.attributes);
                    queue.setValidating(true);
                    const validFiles: File[] = [];
                    for (let i = 0; i < fileInput.files.length; i++) {
                        let file = fileInput.files.item(i);
                        const validation = await this.validateTraverse(file);
                        if (!validation.test) {
                            queue.setValidating(false);
                            queue.pushValidationError(validation.reason);
                            return true;
                        }
                        validFiles.push(file);
                    }
                    queue.setValidating(false);
                    validFiles.forEach((file) => {
                        const fileTransfer = new FileTransfer(file, this.target, transferSession, []);
                        queue.add(fileTransfer);
                    });
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
                const item = items[i];
                if (item.kind !== "file") {
                    console.warn("Item is not a file", item);
                    continue;
                }
                let result: FileSystemEntry | null = null;
                if ("webkitGetAsEntry" in item) {
                    result = item.webkitGetAsEntry();
                } else if ("getAsEntry" in item) {
                    // webkitGetAsEntry will be renamed to getAsEntry in the future
                    result = (item as any).getAsEntry();
                }
                if (result != null) {
                    webkitEntries.push(result);
                }
            }
            if (webkitEntries.length == 0) {
                return false;
            }

            FileTransferQueue.getQueue().setValidating(true);
            for (let i = 0; i < webkitEntries.length; i++) {
                const file = webkitEntries[i];
                const validation = await this.validateTraverse(file);
                if (!validation.test) {
                    FileTransferQueue.getQueue().setValidating(false);
                    FileTransferQueue.getQueue().pushValidationError(validation.reason);
                    return false;
                }
            }
            FileTransferQueue.getQueue().setValidating(false);

            for (let i = 0; i < webkitEntries.length; i++) {
                const file = webkitEntries[i];
                this.traverse(ts, file);
            }
        }

        return true;
    }

    private async validateTraverse(fileEntry: File | FileSystemEntry): Promise<{
        test: boolean,
        fileEntry: any,
        reason: string | null
    }> {
        if(fileEntry == null) {
            console.error("File entry is null", fileEntry);
            return {test: false, fileEntry: fileEntry, reason: "File entry is null"};
        }
        if ("isDirectory" in fileEntry && fileEntry.isDirectory) {
            let children = await this.readEntries(fileEntry as FileSystemDirectoryEntry);
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
            const validation = await this.validateFile(fileEntry as FileSystemFileEntry | File);
            return {test: validation.valid, fileEntry, reason: validation.reason};
        }
    }

    private async readEntries(fileEntry: FileSystemDirectoryEntry): Promise<FileSystemEntry[]> {
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
    private async validateFile(fileEntry: File | FileSystemEntry): Promise<{ valid: boolean, reason: string | null }> {
        const size = await this.getEntrySize(fileEntry);
        return new Promise((accept, reject) => {
            const isFileSystemEntry = 'fullPath' in fileEntry;

            let uploadPath;
            if(isFileSystemEntry){
                uploadPath = fileEntry.fullPath[0] == '/' ? fileEntry.fullPath.substr(1) : fileEntry.fullPath;
            } else {
                uploadPath = fileEntry.name;
            }
            uploadPath = uploadPath.split('/').map(s=>encodeURI(s)).join('/');
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
    private async getEntrySize(fileEntry: File | FileSystemEntry): Promise<number> {
        return new Promise((accept, reject) => {
            if("file" in fileEntry) {
                (fileEntry as FileSystemFileEntry ).file((file) => {
                    accept(file.size);
                }, (error) => {
                    console.error(["Error while reading file size", fileEntry, error]);
                    accept(-1);
                });
            } else if("size" in fileEntry) {
                accept(fileEntry.size);
            } else {
                console.error(["Error while reading file size", fileEntry]);
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
