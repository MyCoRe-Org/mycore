import type {ContentHandler} from "@/apis/ContentHandler.ts";

/**
 * Registry that maps resource type strings (e.g. "objects", "classifications") to their
 * corresponding {@link ContentHandler} implementations. Handlers are registered once on
 * application startup in App.vue and looked up by the editor view on every navigation.
 */
export class ContentHandlerSelector {

  private static MAP: Map<string, ContentHandler> = new Map<string, ContentHandler>();

  /**
   * Registers a content handler for the given type.
   *
   * @param type - The route type string (e.g. "objects", "classifications").
   * @param contentHandler - The handler to associate with this type.
   */
  static registerContentHandler(type: string, contentHandler: ContentHandler): void {
    ContentHandlerSelector.MAP.set(type, contentHandler);
  }

  /**
   * Returns the content handler registered for the given type, or undefined if none exists.
   *
   * @param type - The route type string.
   */
  static get(type: string): ContentHandler | undefined {
    return ContentHandlerSelector.MAP.get(type);
  }

}
