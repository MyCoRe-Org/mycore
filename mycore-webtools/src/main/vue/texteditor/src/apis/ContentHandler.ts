interface ContentHandler {

  load(path: string): Promise<Content>;

  save(path: string, content: Content): Promise<void>;

  hasWriteAccess(path: string): Promise<boolean>

  /**
   * Determines if the content is changed by the server after a save. This happens for example if a mycore object
   * is saved. Their modifyDate will be updated by the server. If a object is dirty after a save the content should
   * be reloaded or updated in the ui.
   *
   * @param path the path to check
   */
  dirtyAfterSave(path: string): boolean

  supportsLocking(path: string): boolean

  lock(path: string): Promise<LockResult>

  unlock(path: string): Promise<LockResult>

}

interface Content {

  data: string;

  type: string;

}

interface LockResult {

  status: "locked" | "unlocked" | "not_owner";

}
