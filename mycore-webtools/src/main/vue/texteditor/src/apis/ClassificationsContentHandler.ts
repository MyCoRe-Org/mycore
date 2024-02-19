import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";

export class ClassificationsContentHandler extends BaseContentHandler {

    constructor(webApplicationBaseURL: string) {
        super(webApplicationBaseURL);
    }

    async load(classificationId: string): Promise<Content> {
        const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/classifications/${classificationId}`);
        const xml = await response.text();
        const prettifiedXml = this.prettifyXml(xml);
        if (response.ok) {
            return {
                data: prettifiedXml,
                type: "application/xml"
            };
        }
        throw this.buildError(`Unable to load ${classificationId}.`, response);
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
        throw this.buildError(`Unable to save ${classificationId}.`, response);
    }

    async hasWriteAccess(classificationId: string): Promise<boolean> {
        // TODO - rest API v2 does not provide support for checking write access
        return true;
    }

    /**
     * https://stackoverflow.com/questions/376373/pretty-printing-xml-with-javascript
     *
     * @param xml xml to prettify
     */
    prettifyXml(xml: string) {
        const tab = "  ";
        let formatted = "";
        let indent = "";
        xml.split(/>\s*</).forEach(function (node) {
            if (node.match(/^\/\w/)) indent = indent.substring(tab.length);
            formatted += `${indent}<${node}>\r\n`;
            if (node.match(/^<?\w[^>]*[^\/]$/)) indent += tab;
        });
        return formatted.substring(1, formatted.length - 3);
    }

    dirtyAfterSave(id: string): boolean {
        return false;
    }

}
