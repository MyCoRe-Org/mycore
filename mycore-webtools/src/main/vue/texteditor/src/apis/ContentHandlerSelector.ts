export class ContentHandlerSelector {

  private static MAP: Map<string, ContentHandler> = new Map<string, ContentHandler>();

  static registerContentHandler(type: string, contentHandler: ContentHandler): void {
    ContentHandlerSelector.MAP.set(type, contentHandler);
  }

  static get(type: string): ContentHandler | undefined {
    return ContentHandlerSelector.MAP.get(type);
  }

}
