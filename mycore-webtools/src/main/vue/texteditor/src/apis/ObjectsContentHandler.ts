import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";

export class ObjectsContentHandler extends BaseContentHandler {

    constructor(webApplicationBaseURL: string) {
        super(webApplicationBaseURL);
    }

    async load(id: string): Promise<Content> {
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`);
        const xml = await response.text();
        if (response.ok) {
            const contentType = response.headers.get("Content-Type");
            if (contentType === null) {
                throw new Error(`Unable to load ${id}. Request was successful but the 'Content-Type' is empty.`);
            }
            return {
                data: xml,
                type: contentType
            };
        }
        throw this.buildError(`Unable to load ${id}.`, response);
    }

    async save(id: string, content: Content): Promise<void> {
        const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": `${content.type}; charset=utf-8`,
                "Authorization": authorizationHeader
            },
            body: content.data
        });
        if (response.ok) {
            return;
        }
        throw this.buildError(`Unable to save ${id}.`, response);
    }

    async hasWriteAccess(id: string): Promise<boolean> {
        const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
        const baseId = this.getBaseId(id);
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${baseId}/try`, {
            method: "PUT",
            headers: {
                "Authorization": authorizationHeader
            }
        });
        return response.ok;
        /*
        TODO: this code uses an OPTION http request currently not supported by the rest v2 API. This should be used in the future.
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`, {
            method: "OPTIONS"
        });
        return super.handleWriteAccessResponse(id, response);
        */
    }

    dirtyAfterSave(id: string): boolean {
        const isContent = this.isContentsId(id);
        return !isContent;
    }

    /**
     * Checks if the id contains the '/contents' part. Assuming access to files.
     *
     * @param id the id to check
     * @return true if it's a content id
     */
    private isContentsId(id: string) {
        return id.indexOf("/contents") !== -1;
    }

    /**
     * Ignores the content part of the id (if any).
     *
     * @param id the requested id
     * @return the base id ignoring the content
     */
    private getBaseId(id: string) {
        if (!this.isContentsId(id)) {
            return id;
        }
        let path = id.split("/");
        return path.length <= 2 ? path[0] : `${path[0]}/${path[1]}/${path[2]}`;
    }

}
