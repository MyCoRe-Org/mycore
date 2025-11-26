import type {Content, ContentHandler, LockResult} from "@/apis/ContentHandler.ts";

export abstract class BaseContentHandler implements ContentHandler {

  mcrApplicationBaseURL: string;

  protected constructor(webApplicationBaseURL: string) {
    this.mcrApplicationBaseURL = webApplicationBaseURL;
  }

  abstract hasWriteAccess(path: string): Promise<boolean>;

  abstract load(path: string): Promise<Content>;

  abstract save(path: string, content: Content): Promise<void>;

  abstract dirtyAfterSave(path: string): boolean

  buildError(preText: string, response: Response) {
    const message = response.headers.get("X-Error-Message");
    const detail = response.headers.get("X-Error-Detail");
    const errorUUID = response.headers.get("X-Error-Uuid");
    return new Error(`${preText} Request failed with status code ${response.status}. ${message} - ${detail} -
         Error Log Code: ${errorUUID}`);
  }

  /**
   * Checks if the response 'Allow' header. If PUT is available, the user has write access.
   * TODO: Currently not supported by the rest v2 API. Should be used in future instead of "/try".
   *
   * @param path the id
   * @param response http response
   */
  handleWriteAccessResponse(path: string, response: Response) {
    if (!response.ok) {
      throw this.buildError(`Unable get OPTIONS of ${path}.`, response);
    }
    //TODO const allow = response.headers.get("Allow");
    const allow = "HEAD,DELETE,GET,OPTIONS,PUT";
    if (!allow) {
      throw new Error(`Unable get OPTIONS of ${path}. Request failed because 'Allow' header is empty.`);
    }
    return allow.toUpperCase().split(",").filter(s => s === "PUT").length >= 1;
  }

  supportsLocking(path: string): boolean {
    return false;
  }

  lock(path: string): Promise<LockResult> {
    return Promise.reject("Locking is not supported by this content handler.");
  }

  unlock(path: string): Promise<LockResult> {
    return Promise.reject("Locking is not supported by this content handler.");
  }

}
