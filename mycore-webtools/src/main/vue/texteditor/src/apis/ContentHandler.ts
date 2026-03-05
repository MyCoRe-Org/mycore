/**
 * Represents the loaded content of a resource, including its raw data and MIME type.
 */
export interface Content {
  /** The raw text content (e.g. XML). */
  data: string;
  /** The MIME type of the content (e.g. "application/xml"). */
  type: string;
}

export interface LockResult {
  status: "locked" | "unlocked" | "not_owner";
}

/**
 * Abstraction for loading, saving and checking access to a specific type of MyCoRe resource.
 * Implementations exist for objects, derivates, and classifications.
 */
export interface ContentHandler {

  /**
   * Loads the content of the resource identified by the given path.
   *
   * @param path - The resource identifier (e.g. "jportal_jpjournal_00000599").
   * @returns The content including its data and MIME type.
   */
  load(path: string): Promise<Content>;

  /**
   * Saves the given content back to the resource identified by the given path.
   *
   * @param path - The resource identifier.
   * @param content - The content to save.
   */
  save(path: string, content: Content): Promise<void>;

  /**
   * Checks whether the current user has write access to the resource.
   *
   * @param path - The resource identifier.
   * @returns True if the user may save changes.
   */
  hasWriteAccess(path: string): Promise<boolean>;

  /**
   * Determines if the content is changed by the server after a save. This happens for example if a mycore object
   * is saved. Their modifyDate will be updated by the server. If a object is dirty after a save the content should
   * be reloaded or updated in the ui.
   *
   * @param path the path to check
   */
  dirtyAfterSave(path: string): boolean;

  supportsLocking(path: string): boolean;

  lock(path: string): Promise<LockResult>;

  unlock(path: string): Promise<LockResult>;

}
