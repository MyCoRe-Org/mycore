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
        throw new Error(`Unable to load ${id}. Request failed with status code ${response.status}.`);
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
        throw new Error(`Unable to save ${id}. Request failed with status code ${response.status}.`);
    }

    async hasWriteAccess(id: string): Promise<boolean> {
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${id}`, {
            method: "OPTIONS"
        });
        return super.handleWriteAccessResponse(id, response);
    }

}
