interface ContentHandler {

  load(id: string): Promise<Content>;

  save(id: string, content: Content): Promise<void>;

  hasWriteAccess(id: string): Promise<boolean>

  /**
   * Determines if the content is changed by the server after a save. This happens for example if a mycore object
   * is saved. Their modifyDate will be updated by the server. If a object is dirty after a save the content should
   * be reloaded or updated in the ui.
   *
   * @param id the id to check
   */
  dirtyAfterSave(id: string): boolean

}

interface Content {

  data: string;

  type: string;

}
