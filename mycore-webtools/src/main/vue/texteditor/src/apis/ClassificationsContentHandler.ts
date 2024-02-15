import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";

export class ClassificationsContentHandler extends BaseContentHandler {

    constructor(webApplicationBaseURL: string) {
        super(webApplicationBaseURL);
    }

    async load(classificationId: string): Promise<Content> {
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/classifications/${classificationId}`);
        const xml = await response.text();
        if (response.ok) {
            return {
                data: xml,
                type: "application/xml"
            };
        }
        throw new Error(`Unable to load ${classificationId}. Request failed with status code ${response.status}.`);
    }

    async save(classificationId: string, content: Content): Promise<void> {
        const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/classifications/${classificationId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/xml; charset=utf-8",
                "Authorization": authorizationHeader
            },
            body: content.data
        });
        if (response.ok) {
            return;
        }
        throw new Error(`Unable to save ${classificationId}. Request failed with status code ${response.status}.`);
    }

    async hasWriteAccess(classificationId: string): Promise<boolean> {
        // TODO
        return true;
    }

}
