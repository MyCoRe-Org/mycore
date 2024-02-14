interface ContentHandler {

    load(id: string): Promise<Content>;

    save(id: string, content: Content): Promise<void>;

    hasWriteAccess(id: string): Promise<boolean>

}

interface Content {

    data: string;

    type: string;

}
