import type {Content, ContentHandler, LockResult} from "@/apis/ContentHandler.ts";

/**
 * Abstract base class for all MyCoRe content handlers. Provides shared utilities for
 * error building, XML prettification and write-access checking that concrete handlers
 * can reuse.
 */
export abstract class BaseContentHandler implements ContentHandler {

  /** Base URL of the MyCoRe application, used to construct API request URLs. */
  mcrApplicationBaseURL: string;

  /**
   * @param webApplicationBaseURL - Base URL of the MyCoRe web application.
   */
  protected constructor(webApplicationBaseURL: string) {
    this.mcrApplicationBaseURL = webApplicationBaseURL;
  }

  abstract hasWriteAccess(path: string): Promise<boolean>;

  abstract load(path: string): Promise<Content>;

  abstract save(path: string, content: Content): Promise<void>;

  abstract dirtyAfterSave(path: string): boolean

  /**
   * Builds a descriptive Error from a failed HTTP response, extracting MyCoRe-specific
   * error headers (X-Error-Message, X-Error-Detail, X-Error-Uuid).
   *
   * @param preText - A human-readable prefix describing what operation failed.
   * @param response - The failed HTTP response.
   */
  buildError(preText: string, response: Response) {
    const message = response.headers.get("X-Error-Message");
    const detail = response.headers.get("X-Error-Detail");
    const errorUUID = response.headers.get("X-Error-Uuid");
    return new Error(`${preText} Request failed with status code ${response.status}. ${message} - ${detail} -
         Error Log Code: ${errorUUID}`);
  }

  /**
   * Prettifies xml. This is a workaround as long as the server does not returns a prettified xml by
   * itself.
   * TODO: remove if not necessary anymore
   *
   * https://stackoverflow.com/questions/376373/pretty-printing-xml-with-javascript
   *
   * @param xml xml to prettify
   */
  prettifyXml(xml: string): string {
    const tab = "  ";
    let formatted = "";
    let indent = "";
    xml.split(/>\s*</).forEach(function(node) {
      if (node.match(/^\/\w/)) indent = indent.substring(tab.length);
      formatted += `${indent}<${node}>\r\n`;
      if (node.match(/^<?\w[^>]*[^\/]$/)) indent += tab;
    });
    return formatted.substring(1, formatted.length - 3);
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
    return Promise.reject(new Error("Locking is not supported by this content handler."));
  }

  unlock(path: string): Promise<LockResult> {
    return Promise.reject(new Error("Locking is not supported by this content handler."));
  }

}
