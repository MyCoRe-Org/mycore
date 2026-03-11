import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";
import type {Content} from "@/apis/ContentHandler.ts";

/**
 * Content handler for MyCoRe classifications, accessed via the REST API v2
 * at `api/v2/classifications/{classificationId}`.
 *
 * Classifications are always returned as `application/xml` and prettified
 * before display since the server does not indent the XML itself.
 */
export class ClassificationsContentHandler extends BaseContentHandler {

  constructor(webApplicationBaseURL: string) {
    super(webApplicationBaseURL);
  }

  /**
   * Loads and prettifies the XML for the given classification.
   *
   * @param classificationId - The classification identifier (e.g. "jportal_class_00000004").
   */
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

  /**
   * Saves the classification XML via HTTP PUT.
   *
   * @param classificationId - The classification identifier.
   * @param content - The content to save.
   */
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

  /**
   * Always returns true since the REST API v2 does not provide a way to check
   * write access for classifications.
   *
   * TODO: implement once the API supports it.
   */
  async hasWriteAccess(classificationId: string): Promise<boolean> {
    // TODO - rest API v2 does not provide support for checking write access
    return true;
  }

  /**
   * Classifications are not dirty after save — the server returns the content unchanged.
   */
  dirtyAfterSave(id: string): boolean {
    return false;
  }

}
