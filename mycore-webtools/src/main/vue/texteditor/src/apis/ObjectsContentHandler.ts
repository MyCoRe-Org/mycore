import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";
import type {Content, LockResult} from "@/apis/ContentHandler.ts";

/**
 * Content handler for MyCoRe objects, derivates and their file contents.
 *
 * Supported id formats:
 * - `jportal_jpjournal_00000599` — a plain MyCoRe object
 * - `jportal_jpvolume_00118654/derivates/jportal_derivate_00201808` — a derivate
 * - `jportal_jpvolume_00118654/derivates/jportal_derivate_00201808/contents` — derivate file listing
 * - `jportal_jpvolume_00118654/derivates/jportal_derivate_00201808/contents/mets.xml` — a specific file
 *
 * All requests go through the MyCoRe REST API v2 at `api/v2/objects/{id}`.
 */
export class ObjectsContentHandler extends BaseContentHandler {

  constructor(webApplicationBaseURL: string) {
    super(webApplicationBaseURL);
  }

  /**
   * Loads and prettifies the XML for the given object, derivate or file path.
   *
   * @param path - The resource path as described in the class documentation.
   */
  async load(path: string): Promise<Content> {
    const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${path}`, {
      method: "GET",
      headers: {
        "Authorization": authorizationHeader
      },
    });
    if (!response.ok) {
      throw this.buildError(`Unable to load ${path}.`, response);
    }
    const xml = this.prettifyXml(await response.text());
    const contentType = response.headers.get("Content-Type");
    if (contentType === null) {
      throw new Error(`Unable to load ${path}. Request was successful but the 'Content-Type' is empty.`);
    }
    return {
      data: xml,
      type: contentType
    };
  }

  /**
   * Saves the content back to the given object or file via HTTP PUT.
   *
   * @param path - The resource path.
   * @param content - The content to save.
   */
  async save(path: string, content: Content): Promise<void> {
    const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${path}`, {
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
    throw this.buildError(`Unable to save ${path}.`, response);
  }

  /**
   * Checks write access by attempting a PUT request to the `/try` endpoint of the base object.
   * File content ids (containing `/contents`) are resolved to their parent object first.
   *
   * @param path - The resource path.
   */
  async hasWriteAccess(path: string): Promise<boolean> {
    const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
    const baseId = this.getBasePath(path);
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${baseId}/try`, {
      method: "PUT",
      headers: {
        "Authorization": authorizationHeader
      }
    });
    return response.ok;
    /*
    TODO: this code uses an OPTION http request currently not supported by the rest v2 API. This should be used in the future.
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${baseId}`, {
        method: "OPTIONS"
    });
    return super.handleWriteAccessResponse(id, response);
    */
  }

  /**
   * Objects are marked dirty after save because the server updates fields such as modifyDate.
   * File content ids are not dirty since the server returns the content as-is.
   *
   * @param path - The resource path.
   */
  dirtyAfterSave(path: string): boolean {
    const isContent = this.containsContent(path);
    return !isContent;
  }

  supportsLocking(path: string): boolean {
    return path.split("/").length == 1;
  }

  async lock(path: string): Promise<LockResult> {
    const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
    const objectId = this.getObjectId(path);
    console.log("lock " + objectId);
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${objectId}/lock`, {
      method: "PUT",
      headers: {
        "Authorization": authorizationHeader
      }
    });
    if (response.ok) {
      return {status: "locked"};
    }
    if (response.status === 409) {
      return {status: "not_owner"};
    }
    throw this.buildError(`Unable to lock ${path}.`, response);
  }

  async unlock(path: string): Promise<LockResult> {
    const authorizationHeader = await getAuthorizationHeader(this.mcrApplicationBaseURL);
    const objectId = this.getObjectId(path);
    console.log("unlock " + objectId);
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${objectId}/lock`, {
      method: "DELETE",
      headers: {
        "Authorization": authorizationHeader
      },
      keepalive: true
    });
    if (response.ok) {
      return {status: "unlocked"};
    }
    if (response.status === 409) {
      return {status: "not_owner"};
    }
    throw this.buildError(`Unable to unlock ${path}.`, response);
  }

  /**
   * Checks if the path contains the '/contents' part. Assuming access to files.
   *
   * @param path the path to check
   * @return true if the path accesses content
   */
  private containsContent(path: string) {
    return path.indexOf("/contents") !== -1;
  }

  /**
   * Ignores the content part of the path (if any). Always returns something like:
   * <ul>
   *     <li>mir_mods_00000001</li>
   *     <li>mir_mods_00000001/derivates</li>
   *     <li>mir_mods_00000001/derivates/mir_derivate_00000001</li>
   * </ul>
   *
   * @param path the requested path
   * @return the base path ignoring the content
   */
  private getBasePath(path: string) {
    if (!this.containsContent(path)) {
      return path;
    }
    const split = path.split("/");
    return split.length <= 2 ? split[0] : `${split[0]}/${split[1]}/${split[2]}`;
  }

  /**
   * Returns the underlying object identifier.
   *
   * @param path the path to the resource
   * @return the mycore object identifier
   */
  private getObjectId(path: string) {
    return path.split("/")[0];
  }

}
