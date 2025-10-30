import {BaseContentHandler} from "@/apis/BaseContentHandler";
import {getAuthorizationHeader} from "@/apis/Auth";

export class ObjectsContentHandler extends BaseContentHandler {

  constructor(webApplicationBaseURL: string) {
    super(webApplicationBaseURL);
  }

  /**
   * Loads an object resource.
   * <p>
   * The path is analog to the REST v2 API. E.g. it can be:
   * <ul>
   *     <li>mir_mods_00000001</li>
   *     <li>mir_mods_00000001/derivates</li>
   *     <li>mir_mods_00000001/derivates/mir_derivate_00000001</li>
   *     <li>mir_mods_00000001/derivates/mir_derivate_00000001/contents</li>
   *     <li>mir_mods_00000001/derivates/mir_derivate_00000001/contents/myFile.xml</li>
   * </ul>
   *
   * @param path path to the resource
   */
  async load(path: string): Promise<Content> {
    const response = await fetch(`${this.mcrApplicationBaseURL}api/v2/objects/${path}`);
    if (!response.ok) {
      throw this.buildError(`Unable to load ${path}.`, response);
    }
    const xml = await response.text();
    const contentType = response.headers.get("Content-Type");
    if (contentType === null) {
      throw new Error(`Unable to load ${path}. Request was successful but the 'Content-Type' is empty.`);
    }
    return {
      data: xml,
      type: contentType
    };
  }

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
      }
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
