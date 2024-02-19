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

    prettifyXml(sourceXml: string) {
            const xmlDoc = new DOMParser().parseFromString(sourceXml, "application/xml");
            const xsltDoc = new DOMParser().parseFromString(`
                <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                  <xsl:strip-space elements="*"/>
                  <xsl:template match="para[content-style][not(text())]">
                    <xsl:value-of select="normalize-space(.)"/>
                  </xsl:template>
                  <xsl:template match="node()|@*">
                    <xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy>
                  </xsl:template>
                  <xsl:output indent="yes"/>
                </xsl:stylesheet>`, "application/xml");
            const xsltProcessor = new XSLTProcessor();
            console.log(xsltDoc);
            xsltProcessor.importStylesheet(xsltDoc);
            const resultDoc = xsltProcessor.transformToDocument(xmlDoc);
            return new XMLSerializer().serializeToString(resultDoc);
    };

    dirtyAfterSave(id: string): boolean {
        return false;
    }

}
