export abstract class BaseContentHandler implements ContentHandler {

    mcrApplicationBaseURL: string;

    protected constructor(webApplicationBaseURL: string) {
        this.mcrApplicationBaseURL = webApplicationBaseURL;
    }

    abstract hasWriteAccess(id: string): Promise<boolean>;

    abstract load(id: string): Promise<Content>;

    abstract save(id: string, content: Content): Promise<void>;

    handleWriteAccessResponse(id: string, response: Response) {
        if (!response.ok) {
            throw new Error(`Unable get OPTIONS of ${id}. Request failed with status code ${response.status}.`);
        }
        //let allow = response.headers.get("Allow");
        const allow = "HEAD,DELETE,GET,OPTIONS,PUT";
        if (!allow) {
            throw new Error(`Unable get OPTIONS of ${id}. Request failed because 'Allow' header is empty.`);
        }
        return allow.toUpperCase().split(",").filter(s => s === "PUT").length >= 1;
    }

}
